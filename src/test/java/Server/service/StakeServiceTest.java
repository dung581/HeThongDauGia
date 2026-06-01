package Server.service;

import Common.DataBase.entities.Stake;
import Common.Enum.StakeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testutil.FakeRepositories.FakeStakeRepository;
import testutil.FakeServices.FakeAccountService;
import testutil.TestReflection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GIAI ĐOẠN 5a — ĐẶT CỌC (StakeService)
 *
 * Bản mới: createStakeForAuction(auctionId, userId, itemId, amount).
 * Đặc biệt: rollback (hoàn tiền) khi lưu stake thất bại.
 */
@DisplayName("Giai đoạn 5a: Đặt cọc (StakeService)")
class StakeServiceTest {

    private StakeService stakeService;
    private FakeStakeRepository repo;
    private FakeAccountService accountService;

    @BeforeEach
    void setUp() {
        stakeService = new StakeService();
        repo = new FakeStakeRepository();
        accountService = new FakeAccountService();
        TestReflection.setField(stakeService, "repo", repo);
        TestReflection.setField(stakeService, "accountService", accountService);
    }

    @Test
    @DisplayName("Tạo cọc: khóa tiền và lưu với trạng thái LOCKED")
    void createStake_success() {
        Stake s = stakeService.createStakeForAuction(50L, 7L, 5L, 2000);

        assertEquals(1, accountService.lockCalls.size());
        assertEquals(7L, accountService.lockCalls.get(0)[0]);
        assertEquals(2000L, accountService.lockCalls.get(0)[1]);
        assertEquals(StakeStatus.LOCKED, s.getStatus());
        assertEquals(50L, s.getAution_id());
        assertEquals(5L, s.getLocked_item_id());
        assertEquals(2000, s.getAmount());
        assertEquals(1, repo.saveCalls);
    }

    @Test
    @DisplayName("Rollback: nếu lưu cọc lỗi thì hoàn lại tiền đã khóa")
    void createStake_rollbackOnSaveFailure() {
        repo.failOnSave = true;

        assertThrows(RuntimeException.class,
                () -> stakeService.createStakeForAuction(50L, 7L, 5L, 2000));

        assertEquals(1, accountService.lockCalls.size(), "Phải đã khóa tiền");
        assertEquals(1, accountService.releaseCalls.size(), "Phải hoàn lại tiền khi lưu lỗi");
        assertEquals(7L, accountService.releaseCalls.get(0)[0]);
        assertEquals(2000L, accountService.releaseCalls.get(0)[1]);
    }

    @Test
    @DisplayName("Giải phóng cọc (theo id) đang LOCKED -> RELEASED và hoàn tiền")
    void releaseStakeById_locked() {
        Stake s = new Stake();
        s.setId(99L);
        s.setUser_id(7L);
        s.setAmount(2000);
        s.setStatus(StakeStatus.LOCKED);
        repo.byId.put(99L, s);

        stakeService.releaseStake(99L);

        assertEquals(1, accountService.releaseCalls.size());
        assertEquals(StakeStatus.RELEASED, repo.byId.get(99L).getStatus());
    }

    @Test
    @DisplayName("Giải phóng cọc (theo object) đang LOCKED -> RELEASED và hoàn tiền")
    void releaseStakeByObject_locked() {
        Stake s = new Stake();
        s.setId(88L);
        s.setUser_id(7L);
        s.setAmount(1500);
        s.setStatus(StakeStatus.LOCKED);
        repo.byId.put(88L, s);

        stakeService.releaseStake(s);

        assertEquals(1, accountService.releaseCalls.size());
        assertEquals(1500L, accountService.releaseCalls.get(0)[1]);
        assertEquals(1, repo.statusUpdates.size());
        assertEquals(StakeStatus.RELEASED, repo.statusUpdates.get(0)[1]);
    }

    @Test
    @DisplayName("Giải phóng cọc null thì bỏ qua an toàn")
    void releaseStake_null() {
        assertDoesNotThrow(() -> stakeService.releaseStake(null));
        assertEquals(0, accountService.releaseCalls.size());
    }

    @Test
    @DisplayName("Không giải phóng cọc không còn LOCKED (tránh hoàn tiền 2 lần)")
    void releaseStake_notLocked() {
        Stake s = new Stake();
        s.setId(77L);
        s.setStatus(StakeStatus.RELEASED);

        stakeService.releaseStake(s);

        assertEquals(0, accountService.releaseCalls.size());
        assertTrue(repo.statusUpdates.isEmpty());
    }

    @Test
    @DisplayName("markWon cập nhật trạng thái WON")
    void markWon() {
        Stake s = new Stake();
        s.setId(99L);
        s.setStatus(StakeStatus.LOCKED);
        repo.byId.put(99L, s);

        stakeService.markWon(99L);

        assertEquals(StakeStatus.WON, repo.byId.get(99L).getStatus());
    }

    @Test
    @DisplayName("getActiveStake tìm được cọc đang LOCKED của phiên + người dùng")
    void getActiveStake_findsLocked() {
        Stake s = new Stake();
        s.setId(99L);
        s.setAution_id(50L);
        s.setUser_id(7L);
        s.setStatus(StakeStatus.LOCKED);
        repo.byId.put(99L, s);

        Stake found = stakeService.getActiveStake(50L, 7L);
        assertNotNull(found);
        assertEquals(99L, found.getId());

        s.setStatus(StakeStatus.RELEASED);
        assertNull(stakeService.getActiveStake(50L, 7L));
    }
}