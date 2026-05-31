package Server.service;

import Common.DataBase.entities.Item;
import Common.Enum.ItemStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testutil.FakeRepositories.FakeItemRepository;
import testutil.TestReflection;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GIAI ĐOẠN 2 — QUẢN LÝ VẬT PHẨM (ItemService)
 *
 * Vòng đời: PENDING -> APPROVED -> IN_AUCTION -> SOLD, hoặc bị từ chối.
 * Bản mới: upload validate beginPrice > 0 và minIncrement > 0.
 */
@DisplayName("Giai đoạn 2: Quản lý vật phẩm (ItemService)")
class ItemServiceTest {

    private ItemService itemService;
    private FakeItemRepository itemRepo;

    @BeforeEach
    void setUp() {
        itemService = new ItemService();
        itemRepo = new FakeItemRepository();
        TestReflection.setField(itemService, "repo", itemRepo);
    }

    private Item newValidItem() {
        Item i = new Item();
        i.setFullname("Đồng hồ cổ");
        i.setBeginPrice(1000);
        i.setMinIncrement(100);
        return i;
    }

    private Item seed(long id, ItemStatus status) {
        Item i = new Item();
        i.setId(id);
        i.setStatus(status);
        i.setBeginPrice(1000);
        i.setMinIncrement(100);
        return itemRepo.put(i);
    }

    @Test
    @DisplayName("Upload hợp lệ đặt trạng thái PENDING và điền mặc định")
    void upload_setsPendingAndDefaults() {
        Item result = itemService.upload(newValidItem());

        assertEquals(ItemStatus.PENDING, result.getStatus());
        assertEquals("", result.getDescription());
        assertEquals("Cho duyet", result.getMota());
        assertEquals(1, itemRepo.saveCalls);
    }

    @Test
    @DisplayName("Upload bị từ chối khi beginPrice <= 0")
    void upload_invalidBeginPrice() {
        Item i = newValidItem();
        i.setBeginPrice(0);
        assertThrows(RuntimeException.class, () -> itemService.upload(i));
        assertEquals(0, itemRepo.saveCalls);
    }

    @Test
    @DisplayName("Upload bị từ chối khi minIncrement <= 0")
    void upload_invalidMinIncrement() {
        Item i = newValidItem();
        i.setMinIncrement(0);
        assertThrows(RuntimeException.class, () -> itemService.upload(i));
        assertEquals(0, itemRepo.saveCalls);
    }

    @Test
    @DisplayName("Duyệt vật phẩm PENDING -> APPROVED")
    void approve_pendingItem() {
        seed(10L, ItemStatus.PENDING);
        itemService.approve(10L);
        Item updated = itemRepo.byId.get(10L);
        assertEquals(ItemStatus.APPROVED, updated.getStatus());
        assertEquals("Da duyet", updated.getMota());
    }

    @Test
    @DisplayName("Không duyệt được vật phẩm không tồn tại")
    void approve_notFound() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> itemService.approve(99L));
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    @DisplayName("Không duyệt được vật phẩm không ở trạng thái PENDING")
    void approve_notPending() {
        seed(10L, ItemStatus.APPROVED);
        assertThrows(RuntimeException.class, () -> itemService.approve(10L));
    }

    @Test
    @DisplayName("Từ chối vật phẩm PENDING với lý do tùy chỉnh")
    void reject_withReason() {
        seed(10L, ItemStatus.PENDING);
        itemService.reject(10L, "Anh mo");
        assertEquals(10L, itemRepo.lastRejectedId.longValue());
        assertEquals("Anh mo", itemRepo.lastRejectReason);
    }

    @Test
    @DisplayName("Từ chối với lý do rỗng dùng ghi chú mặc định")
    void reject_blankReasonUsesDefault() {
        seed(10L, ItemStatus.PENDING);
        itemService.reject(10L, "   ");
        assertEquals("Khong dat yeu cau", itemRepo.lastRejectReason);
    }

    @Test
    @DisplayName("Không từ chối được vật phẩm không ở trạng thái PENDING")
    void reject_notPending() {
        seed(10L, ItemStatus.IN_AUCTION);
        assertThrows(RuntimeException.class, () -> itemService.reject(10L, "x"));
        assertNull(itemRepo.lastRejectedId);
    }

    @Test
    @DisplayName("getById ném lỗi khi không tìm thấy")
    void getById_notFound() {
        assertThrows(RuntimeException.class, () -> itemService.getById(99L));
    }

    @Test
    @DisplayName("listByOwner trả về đúng vật phẩm theo chủ sở hữu")
    void listByOwner_filters() {
        Item a = seed(1L, ItemStatus.PENDING);
        a.setOwner_user_id(7L);
        Item b = seed(2L, ItemStatus.PENDING);
        b.setOwner_user_id(9L);

        List<Item> result = itemService.listByOwner(7L);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    @DisplayName("listPending chỉ trả về vật phẩm PENDING")
    void listPending_filters() {
        seed(1L, ItemStatus.PENDING);
        seed(2L, ItemStatus.APPROVED);
        List<Item> result = itemService.listPending();
        assertEquals(1, result.size());
        assertEquals(ItemStatus.PENDING, result.get(0).getStatus());
    }
}