package Server.service;

import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Bid;
import Common.DataBase.entities.Item;
import Common.DataBase.entities.Stake;
import Common.Enum.AuctionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testutil.FakeRepositories.FakeAuctionRepository;
import testutil.FakeRepositories.FakeBidRepository;
import testutil.FakeRepositories.FakeItemRepository;
import testutil.FakeServices.FakeAutoBidService;
import testutil.FakeServices.FakeStakeService;
import testutil.TestReflection;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GIAI ĐOẠN 5b — ĐẶT GIÁ (BidService)
 *
 * Bản mới: placeBid kiểm tra phiên RUNNING -> bước giá tối thiểu ->
 * release stake người cũ -> createStakeForAuction -> lưu bid ->
 * cập nhật giá -> autoTime (gia hạn). Auto-bid trigger được controller gọi ở task nền,
 * KHÔNG gọi trong placeBid.
 */
@DisplayName("Giai đoạn 5b: Đặt giá (BidService)")
class BidServiceTest {

    private BidService bidService;
    private FakeAuctionRepository auctionRepo;
    private FakeBidRepository bidRepo;
    private FakeItemRepository itemRepo;
    private FakeStakeService stakeService;
    private FakeAutoBidService autoBidService;

    @BeforeEach
    void setUp() {
        bidService = new BidService();
        auctionRepo = new FakeAuctionRepository();
        bidRepo = new FakeBidRepository();
        itemRepo = new FakeItemRepository();
        stakeService = new FakeStakeService();
        autoBidService = new FakeAutoBidService();
        TestReflection.setField(bidService, "auctionRepo", auctionRepo);
        TestReflection.setField(bidService, "bidRepo", bidRepo);
        TestReflection.setField(bidService, "itemRepo", itemRepo);
        TestReflection.setField(bidService, "stakeService", stakeService);
        TestReflection.setField(bidService, "autoBidService", autoBidService);
    }

    private Auction seedAuction(long id, long itemId, long price, long currentUser, AuctionState state) {
        Auction a = new Auction();
        a.setId(id);
        a.setItem_id(itemId);
        a.setCurrent_price(price);
        a.setCurrent_user_id(currentUser);
        a.setState(state);
        a.setEndTime(LocalDateTime.now().plusHours(1));
        return auctionRepo.put(a);
    }

    private void seedItem(long id, long minIncrement) {
        Item i = new Item();
        i.setId(id);
        i.setMinIncrement(minIncrement);
        itemRepo.put(i);
    }

    @Test
    @DisplayName("Đặt giá hợp lệ: tạo cọc, lưu bid, cập nhật giá, gọi autoTime")
    void placeBid_success() {
        seedAuction(50L, 5L, 1000, 0L, AuctionState.RUNNING);
        seedItem(5L, 100);

        Bid result = bidService.placeBid(7L, 5L, 1100); // 1000 + 100

        assertEquals(7L, result.getUser_id());
        assertEquals(1100, result.getPrice());
        assertEquals(50L, result.getAuction_id());

        assertEquals(1, stakeService.createCalls.size());
        // createStakeForAuction(auctionId=50, userId=7, itemId=5, amount=1100)
        assertEquals(50L, stakeService.createCalls.get(0)[0]);
        assertEquals(7L, stakeService.createCalls.get(0)[1]);
        assertEquals(5L, stakeService.createCalls.get(0)[2]);
        assertEquals(1100L, stakeService.createCalls.get(0)[3]);

        assertEquals(1, bidRepo.saveCalls);
        assertEquals(50L, auctionRepo.lastUpdatedAuctionId.longValue());
        assertEquals(7L, auctionRepo.lastUpdatedUserId.longValue());
        assertEquals(1100L, auctionRepo.lastUpdatedPrice.longValue());
        assertEquals(1, autoBidService.autoTimeCalls);
    }

    @Test
    @DisplayName("Từ chối khi không đủ bước giá tối thiểu")
    void placeBid_belowMinIncrement() {
        seedAuction(50L, 5L, 1000, 0L, AuctionState.RUNNING);
        seedItem(5L, 100); // minAllowed = 1100

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bidService.placeBid(7L, 5L, 1099));
        assertTrue(ex.getMessage().contains("at least"));
        assertEquals(0, bidRepo.saveCalls);
    }

    @Test
    @DisplayName("Chấp nhận giá đúng bằng mức tối thiểu (biên)")
    void placeBid_exactlyMinAllowed() {
        seedAuction(50L, 5L, 1000, 0L, AuctionState.RUNNING);
        seedItem(5L, 100);
        assertDoesNotThrow(() -> bidService.placeBid(7L, 5L, 1100));
        assertEquals(1, bidRepo.saveCalls);
    }

    @Test
    @DisplayName("minIncrement <= 0 thì mặc định bước giá là 1")
    void placeBid_defaultIncrementWhenZero() {
        seedAuction(50L, 5L, 1000, 0L, AuctionState.RUNNING);
        seedItem(5L, 0); // -> 1, minAllowed = 1001

        assertThrows(RuntimeException.class, () -> bidService.placeBid(7L, 5L, 1000));
        assertDoesNotThrow(() -> bidService.placeBid(7L, 5L, 1001));
    }

    @Test
    @DisplayName("Đặt giá khi phiên đã đóng bị từ chối")
    void placeBid_auctionClosed() {
        seedAuction(50L, 5L, 1000, 0L, AuctionState.PAID);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bidService.placeBid(7L, 5L, 2000));
        assertTrue(ex.getMessage().contains("closed"));
    }

    @Test
    @DisplayName("Đặt giá khi không tìm thấy phiên bị từ chối")
    void placeBid_auctionNotFound() {
        assertThrows(RuntimeException.class, () -> bidService.placeBid(7L, 5L, 2000));
    }

    @Test
    @DisplayName("Đặt giá khi không tìm thấy vật phẩm bị từ chối")
    void placeBid_itemNotFound() {
        seedAuction(50L, 5L, 1000, 0L, AuctionState.RUNNING);
        assertThrows(RuntimeException.class, () -> bidService.placeBid(7L, 5L, 2000));
    }

    @Test
    @DisplayName("Giải phóng cọc của người giữ giá cũ trước khi tạo cọc mới")
    void placeBid_releasesPreviousLeaderStake() {
        seedAuction(50L, 5L, 1000, 3L, AuctionState.RUNNING); // người cũ = user 3
        seedItem(5L, 100);
        Stake oldStake = new Stake();
        oldStake.setId(88L);
        stakeService.activeStakeToReturn = oldStake;

        bidService.placeBid(7L, 5L, 1100);

        // release theo object stake cũ
        assertEquals(1, stakeService.releaseByObjCalls.size());
        assertEquals(88L, stakeService.releaseByObjCalls.get(0).getId());
        assertEquals(1, stakeService.createCalls.size());
    }

    @Test
    @DisplayName("Không giải phóng cọc khi chưa có người giữ giá (current_user_id = 0)")
    void placeBid_noPreviousLeader() {
        seedAuction(50L, 5L, 1000, 0L, AuctionState.RUNNING);
        seedItem(5L, 100);

        bidService.placeBid(7L, 5L, 1100);

        assertTrue(stakeService.releaseByObjCalls.isEmpty());
    }

    @Test
    @DisplayName("getHistoryBySession trả về bid sắp xếp giảm dần theo giá")
    void getHistory_sortedByPriceDesc() {
        bidRepo.saved.add(makeBid(50L, 1000));
        bidRepo.saved.add(makeBid(50L, 3000));
        bidRepo.saved.add(makeBid(50L, 2000));

        List<Bid> result = bidService.getHistoryBySession(50L);

        assertEquals(3000, result.get(0).getPrice());
        assertEquals(2000, result.get(1).getPrice());
        assertEquals(1000, result.get(2).getPrice());
    }

    private Bid makeBid(long auctionId, long price) {
        Bid b = new Bid();
        b.setAuction_id(auctionId);
        b.setPrice(price);
        return b;
    }
}