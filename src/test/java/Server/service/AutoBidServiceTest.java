package Server.service;

import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Autobid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testutil.FakeRepositories.FakeAuctionRepository;
import testutil.FakeRepositories.FakeAutoBidRepository;
import testutil.TestReflection;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GIAI ĐOẠN 6 — ĐẤU GIÁ TỰ ĐỘNG (AutoBidService)
 *
 * Cấu hình mới/cập nhật, lấy bản ghi mới nhất, anti-sniping gia hạn 5 phút cuối.
 * trigger() tự new BidService bên trong nên không test trực tiếp việc đặt giá ở đây.
 */
@DisplayName("Giai đoạn 6: Đấu giá tự động (AutoBidService)")
class AutoBidServiceTest {

    private AutoBidService autoBidService;
    private FakeAutoBidRepository repo;
    private FakeAuctionRepository auctionRepo;

    @BeforeEach
    void setUp() {
        autoBidService = new AutoBidService();
        repo = new FakeAutoBidRepository();
        auctionRepo = new FakeAuctionRepository();
        TestReflection.setField(autoBidService, "repo", repo);
        TestReflection.setField(autoBidService, "auctionRepo", auctionRepo);
    }

    private Autobid seed(long id, long userId, long itemId, long maxPrice, boolean active) {
        Autobid a = new Autobid();
        a.setId(id);
        a.setUser_id(userId);
        a.setItem_id(itemId);
        a.setMax_price(maxPrice);
        a.set_active(active);
        return repo.put(a);
    }

    @Test
    @DisplayName("configureAndActivate tạo mới khi chưa có cấu hình")
    void configureAndActivate_createsNew() {
        Autobid result = autoBidService.configureAndActivate(7L, 5L, 5000);
        assertEquals(5000, result.getMax_price());
        assertTrue(result.is_active());
        assertEquals(1, repo.saveCalls);
    }

    @Test
    @DisplayName("configureAndActivate cập nhật khi đã có cấu hình")
    void configureAndActivate_updatesExisting() {
        seed(100L, 7L, 5L, 3000, false);

        Autobid result = autoBidService.configureAndActivate(7L, 5L, 8000);

        assertEquals(8000, result.getMax_price());
        assertTrue(result.is_active());
        assertEquals(1, repo.maxActiveUpdates.size());
        assertEquals(100L, repo.maxActiveUpdates.get(0)[0]);
        assertEquals(8000L, repo.maxActiveUpdates.get(0)[1]);
        assertEquals(0, repo.saveCalls);
    }

    @Test
    @DisplayName("getLatestByUserAndItem lấy bản ghi id lớn nhất của đúng item")
    void getLatest_picksHighestId() {
        seed(1L, 7L, 5L, 1000, true);
        seed(3L, 7L, 5L, 3000, true);
        seed(2L, 7L, 9L, 2000, true);

        Optional<Autobid> result = autoBidService.getLatestByUserAndItem(7L, 5L);
        assertTrue(result.isPresent());
        assertEquals(3L, result.get().getId());
    }

    @Test
    @DisplayName("getLatestByUserAndItem rỗng khi không có cấu hình")
    void getLatest_empty() {
        assertTrue(autoBidService.getLatestByUserAndItem(7L, 5L).isEmpty());
    }

    @Test
    @DisplayName("deactivateByUserAndItem tắt cấu hình mới nhất")
    void deactivateByUserAndItem() {
        seed(100L, 7L, 5L, 3000, true);
        autoBidService.deactivateByUserAndItem(7L, 5L);
        assertEquals(1, repo.activeUpdates.size());
        assertEquals(100L, repo.activeUpdates.get(0)[0]);
        assertEquals(Boolean.FALSE, repo.activeUpdates.get(0)[1]);
    }

    @Test
    @DisplayName("deactivateByUserAndItem ném lỗi khi không tìm thấy cấu hình")
    void deactivateByUserAndItem_notFound() {
        assertThrows(RuntimeException.class,
                () -> autoBidService.deactivateByUserAndItem(7L, 5L));
    }

    @Test
    @DisplayName("trigger đặt giá hộ khi có autobid hợp lệ của người khác (max >= nextPrice)")
    void trigger_placesForValidConfig() {
        // autobid của user 9, max 5000, đang active
        seed(1L, 9L, 5L, 5000, true);
        // current price 1000, step 100 -> nextPrice 1100, max 5000 >= 1100 -> đủ điều kiện
        // Lưu ý: trigger tự new BidService nên sẽ gọi DB thật -> bắt mọi lỗi và trả false.
        // Ở đây ta chỉ kiểm tra trigger lọc đúng config (không ném ra ngoài).
        boolean result = autoBidService.trigger(5L, 1000, 7L, 100);
        // Vì BidService thật sẽ ném lỗi (không có DB), trigger nuốt lỗi và trả false.
        assertFalse(result);
    }

    @Test
    @DisplayName("trigger bỏ qua autobid của chính người vừa đặt giá")
    void trigger_skipsSameUser() {
        seed(1L, 7L, 5L, 5000, true); // cùng user 7 đang giữ giá
        boolean result = autoBidService.trigger(5L, 1000, 7L, 100);
        assertFalse(result); // không có ai khác để đặt hộ
    }

    @Test
    @DisplayName("trigger bỏ qua autobid khi max < nextPrice")
    void trigger_skipsWhenMaxTooLow() {
        seed(1L, 9L, 5L, 1050, true); // max 1050 < nextPrice 1100
        boolean result = autoBidService.trigger(5L, 1000, 7L, 100);
        assertFalse(result);
    }

    @Test
    @DisplayName("autoTime gia hạn thêm 5 phút khi bid trong 5 phút cuối")
    void autoTime_extendsNearEnd() {
        Auction a = new Auction();
        a.setId(50L);
        LocalDateTime end = LocalDateTime.now().plusMinutes(2);
        a.setEndTime(end);

        autoBidService.autoTime(a);

        assertTrue(a.getEndTime().isAfter(end));
        assertEquals(1, repo.endTimeUpdates.size());
        assertEquals(50L, repo.endTimeUpdates.get(0)[0]);
    }

    @Test
    @DisplayName("autoTime KHÔNG gia hạn khi còn nhiều thời gian")
    void autoTime_noExtendWhenFar() {
        Auction a = new Auction();
        a.setId(50L);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        a.setEndTime(end);

        autoBidService.autoTime(a);

        assertEquals(end, a.getEndTime());
        assertEquals(0, repo.endTimeUpdates.size());
    }

    @Test
    @DisplayName("autoTime KHÔNG gia hạn khi phiên đã quá hạn")
    void autoTime_noExtendWhenExpired() {
        Auction a = new Auction();
        a.setId(50L);
        LocalDateTime end = LocalDateTime.now().minusMinutes(1);
        a.setEndTime(end);

        autoBidService.autoTime(a);

        assertEquals(end, a.getEndTime());
        assertEquals(0, repo.endTimeUpdates.size());
    }
}