package Server.service;

import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Item;
import Common.DataBase.entities.Stake;
import Common.Enum.AuctionState;
import Common.Enum.ItemStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testutil.FakeRepositories.FakeAuctionRepository;
import testutil.FakeRepositories.FakeItemRepository;
import testutil.FakeServices.FakeAccountService;
import testutil.FakeServices.FakeStakeService;
import testutil.TestReflection;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GIAI ĐOẠN 4 — VÒNG ĐỜI PHIÊN ĐẤU GIÁ (AuctionService)
 *
 * Bản mới: khi đóng phiên có người thắng, cộng tiền bán cho seller (nếu seller khác winner).
 */
@DisplayName("Giai đoạn 4: Vòng đời phiên đấu giá (AuctionService)")
class AuctionServiceTest {

    private AuctionService auctionService;
    private FakeAuctionRepository repo;
    private FakeItemRepository itemRepo;
    private FakeStakeService stakeService;
    private FakeAccountService accountService;
    private ItemService itemService;

    @BeforeEach
    void setUp() {
        auctionService = new AuctionService();
        repo = new FakeAuctionRepository();
        itemRepo = new FakeItemRepository();
        stakeService = new FakeStakeService();
        accountService = new FakeAccountService();
        itemService = new ItemService();
        TestReflection.setField(itemService, "repo", itemRepo);
        TestReflection.setField(auctionService, "repo", repo);
        TestReflection.setField(auctionService, "itemRepo", itemRepo);
        TestReflection.setField(auctionService, "itemService", itemService);
        TestReflection.setField(auctionService, "stakeService", stakeService);
        TestReflection.setField(auctionService, "accountService", accountService);
    }

    private Item seedItem(long id, ItemStatus status, long beginPrice, long ownerId) {
        Item i = new Item();
        i.setId(id);
        i.setStatus(status);
        i.setBeginPrice(beginPrice);
        i.setOwner_user_id(ownerId);
        return itemRepo.put(i);
    }

    private Auction seedAuction(long id, long itemId, long currentUser, long price, AuctionState state) {
        Auction a = new Auction();
        a.setId(id);
        a.setItem_id(itemId);
        a.setCurrent_user_id(currentUser);
        a.setCurrent_price(price);
        a.setState(state);
        return repo.put(a);
    }

    @Test
    @DisplayName("Tạo phiên từ vật phẩm APPROVED -> RUNNING, giá khởi điểm = beginPrice")
    void createSession_success() {
        seedItem(5L, ItemStatus.APPROVED, 1000, 3L);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        Auction a = auctionService.createSession(5L, end);

        assertEquals(AuctionState.RUNNING, a.getState());
        assertEquals(1000, a.getCurrent_price());
        assertEquals(end, a.getEndTime());
        assertEquals(1, repo.saveCalls);
        assertEquals(ItemStatus.IN_AUCTION, itemRepo.byId.get(5L).getStatus());
    }

    @Test
    @DisplayName("Không tạo được phiên nếu vật phẩm không tồn tại")
    void createSession_itemNotFound() {
        assertThrows(RuntimeException.class,
                () -> auctionService.createSession(5L, LocalDateTime.now().plusHours(1)));
    }

    @Test
    @DisplayName("Không tạo được phiên nếu vật phẩm chưa APPROVED")
    void createSession_itemNotApproved() {
        seedItem(5L, ItemStatus.PENDING, 1000, 3L);
        assertThrows(RuntimeException.class,
                () -> auctionService.createSession(5L, LocalDateTime.now().plusHours(1)));
        assertEquals(0, repo.saveCalls);
    }

    @Test
    @DisplayName("approveAndCreateSession: duyệt PENDING rồi mở phiên trong một bước")
    void approveAndCreateSession_success() {
        seedItem(5L, ItemStatus.PENDING, 1000, 3L);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        Auction a = auctionService.approveAndCreateSession(5L, end);

        assertEquals(AuctionState.RUNNING, a.getState());
        assertEquals(ItemStatus.IN_AUCTION, itemRepo.byId.get(5L).getStatus());
    }

    @Test
    @DisplayName("approveAndCreateSession ném lỗi nếu vật phẩm không PENDING")
    void approveAndCreateSession_notPending() {
        seedItem(5L, ItemStatus.APPROVED, 1000, 3L);
        assertThrows(RuntimeException.class,
                () -> auctionService.approveAndCreateSession(5L, LocalDateTime.now().plusHours(1)));
    }

    @Test
    @DisplayName("Đóng phiên có người thắng: trừ tiền winner, cộng tiền seller, stake WON, PAID, SOLD")
    void closeSession_withWinner() {
        seedItem(5L, ItemStatus.IN_AUCTION, 1000, 3L); // seller = user 3
        seedAuction(50L, 5L, 7L, 2000, AuctionState.RUNNING); // winner = user 7
        Stake winning = new Stake();
        winning.setId(99L);
        stakeService.activeStakeToReturn = winning;

        auctionService.closeSession(50L);

        assertEquals(1, accountService.deductCalls.size());
        assertEquals(7L, accountService.deductCalls.get(0)[0]);
        assertEquals(2000L, accountService.deductCalls.get(0)[1]);
        // Cộng tiền cho seller (user 3)
        assertEquals(1, accountService.creditCalls.size());
        assertEquals(3L, accountService.creditCalls.get(0)[0]);
        assertEquals(2000L, accountService.creditCalls.get(0)[1]);
        assertTrue(stakeService.wonCalls.contains(99L));
        assertEquals(AuctionState.PAID, repo.byId.get(50L).getState());
        assertEquals(ItemStatus.SOLD, itemRepo.byId.get(5L).getStatus());
    }

    @Test
    @DisplayName("Không cộng tiền seller nếu seller chính là winner")
    void closeSession_sellerIsWinner_noCredit() {
        seedItem(5L, ItemStatus.IN_AUCTION, 1000, 7L); // seller = winner = 7
        seedAuction(50L, 5L, 7L, 2000, AuctionState.RUNNING);
        stakeService.activeStakeToReturn = null;

        auctionService.closeSession(50L);

        assertEquals(0, accountService.creditCalls.size());
    }

    @Test
    @DisplayName("Đóng phiên không ai đặt giá: CANCELED, vật phẩm về APPROVED")
    void closeSession_noBidder() {
        seedItem(5L, ItemStatus.IN_AUCTION, 1000, 3L);
        seedAuction(50L, 5L, 0L, 1000, AuctionState.RUNNING);

        auctionService.closeSession(50L);

        assertEquals(0, accountService.deductCalls.size());
        assertEquals(AuctionState.CANCELED, repo.byId.get(50L).getState());
        assertEquals(ItemStatus.APPROVED, itemRepo.byId.get(5L).getStatus());
    }

    @Test
    @DisplayName("Đóng phiên đã không RUNNING thì không làm gì (idempotent)")
    void closeSession_alreadyClosed() {
        seedAuction(50L, 5L, 7L, 2000, AuctionState.PAID);
        auctionService.closeSession(50L);
        assertEquals(0, accountService.deductCalls.size());
        assertEquals(0, repo.updateCalls);
    }

    @Test
    @DisplayName("Đóng phiên không tồn tại thì ném lỗi")
    void closeSession_notFound() {
        assertThrows(RuntimeException.class, () -> auctionService.closeSession(50L));
    }

    @Test
    @DisplayName("closeIfExpired: chưa hết giờ thì không đóng, trả về false")
    void closeIfExpired_notYet() {
        Auction a = seedAuction(50L, 5L, 7L, 2000, AuctionState.RUNNING);
        a.setEndTime(LocalDateTime.now().plusHours(1));

        boolean closed = auctionService.closeIfExpired(50L);

        assertFalse(closed);
        assertEquals(AuctionState.RUNNING, repo.byId.get(50L).getState());
    }

    @Test
    @DisplayName("closeIfExpired: đã hết giờ thì đóng, trả về true")
    void closeIfExpired_expired() {
        seedItem(5L, ItemStatus.IN_AUCTION, 1000, 3L);
        Auction a = seedAuction(50L, 5L, 0L, 1000, AuctionState.RUNNING);
        a.setEndTime(LocalDateTime.now().minusMinutes(1));

        boolean closed = auctionService.closeIfExpired(50L);

        assertTrue(closed);
        assertEquals(AuctionState.CANCELED, repo.byId.get(50L).getState());
    }
}
