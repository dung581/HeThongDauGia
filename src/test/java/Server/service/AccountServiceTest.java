package Server.service;

import Common.DataBase.entities.Account;
import Common.DataBase.entities.User;
import Common.Enum.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testutil.FakeRepositories.FakeAccountRepository;
import testutil.FakeRepositories.FakeUserRepository;
import testutil.TestReflection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GIAI ĐOẠN 3 — TÀI KHOẢN & DÒNG TIỀN (AccountService)
 *
 * balance (tổng) và locked_balance (đang khóa). available = balance - locked.
 *
 * Lưu ý bản mới:
 * - requireAccount tự tạo account (balance=0) nếu user tồn tại mà chưa có account.
 * - releaseFunds dùng Math.min (không ném lỗi khi release quá số đang khóa).
 * - deductLockedFunds kiểm tra balance < amount (không phải locked).
 */
@DisplayName("Giai đoạn 3: Tài khoản & dòng tiền (AccountService)")
class AccountServiceTest {

    private AccountService accountService;
    private FakeAccountRepository repo;
    private FakeUserRepository userRepo;

    @BeforeEach
    void setUp() {
        accountService = new AccountService();
        repo = new FakeAccountRepository();
        userRepo = new FakeUserRepository();
        TestReflection.setField(accountService, "repo", repo);
        TestReflection.setField(accountService, "userRepository", userRepo);
    }

    private Account seedAccount(long userId, long balance, long locked) {
        Account a = new Account();
        a.setId(userId);
        a.setUser_id(userId);
        a.setBalance(balance);
        a.setLocked_balance(locked);
        return repo.put(a);
    }

    private void seedUserOnly(long userId) {
        userRepo.put(new User(userId, "user" + userId, "x", UserRole.BIDDER, "User"));
    }

    @Test
    @DisplayName("getBalance ném lỗi khi user không tồn tại và chưa có account")
    void getBalance_userNotFound() {
        assertThrows(RuntimeException.class, () -> accountService.getBalance(1L));
    }

    @Test
    @DisplayName("Khi user tồn tại nhưng chưa có account thì tự tạo account số dư 0")
    void requireAccount_autoCreatesForExistingUser() {
        seedUserOnly(1L);
        Account acc = accountService.getBalance(1L);
        assertNotNull(acc);
        assertEquals(0L, acc.getBalance());
        assertEquals(0L, acc.getLocked_balance());
    }

    @Test
    @DisplayName("Nạp tiền cộng vào balance")
    void deposit_increasesBalance() {
        Account acc = seedAccount(1L, 1000, 0);
        accountService.deposit(1L, 500);
        assertEquals(1500, acc.getBalance());
        assertEquals(1, repo.updateCalls);
    }

    @Test
    @DisplayName("Nạp số tiền <= 0 bị từ chối")
    void deposit_rejectsNonPositive() {
        seedAccount(1L, 1000, 0);
        assertThrows(RuntimeException.class, () -> accountService.deposit(1L, 0));
        assertThrows(RuntimeException.class, () -> accountService.deposit(1L, -100));
    }

    @Test
    @DisplayName("creditSaleProceeds cộng tiền bán cho người bán")
    void creditSaleProceeds_increasesBalance() {
        Account acc = seedAccount(1L, 1000, 0);
        accountService.creditSaleProceeds(1L, 2000);
        assertEquals(3000, acc.getBalance());
    }

    @Test
    @DisplayName("Khóa tiền tăng locked khi đủ tiền khả dụng")
    void lockFunds_success() {
        Account acc = seedAccount(1L, 1000, 200); // available = 800
        accountService.lockFunds(1L, 800);
        assertEquals(1000, acc.getLocked_balance());
    }

    @Test
    @DisplayName("Khóa tiền thất bại khi vượt số khả dụng")
    void lockFunds_notEnough() {
        seedAccount(1L, 1000, 200); // available = 800
        RuntimeException ex = assertThrows(RuntimeException.class, () -> accountService.lockFunds(1L, 801));
        assertTrue(ex.getMessage().contains("Not enough money"));
    }

    @Test
    @DisplayName("Khóa đúng bằng số khả dụng (biên) thì thành công")
    void lockFunds_exactlyAvailable() {
        Account acc = seedAccount(1L, 1000, 0);
        assertDoesNotThrow(() -> accountService.lockFunds(1L, 1000));
        assertEquals(1000, acc.getLocked_balance());
    }

    @Test
    @DisplayName("Giải phóng tiền giảm locked, balance không đổi")
    void releaseFunds_success() {
        Account acc = seedAccount(1L, 1000, 600);
        accountService.releaseFunds(1L, 400);
        assertEquals(200, acc.getLocked_balance());
        assertEquals(1000, acc.getBalance());
    }

    @Test
    @DisplayName("Giải phóng nhiều hơn số đang khóa thì chỉ release tối đa phần đang khóa (Math.min)")
    void releaseFunds_moreThanLockedClampsToLocked() {
        Account acc = seedAccount(1L, 1000, 300);
        accountService.releaseFunds(1L, 999); // chỉ release được 300
        assertEquals(0, acc.getLocked_balance());
    }

    @Test
    @DisplayName("Trừ tiền khi thắng giảm cả balance lẫn locked")
    void deductLockedFunds_success() {
        Account acc = seedAccount(1L, 1000, 800);
        accountService.deductLockedFunds(1L, 800);
        assertEquals(200, acc.getBalance());
        assertEquals(0, acc.getLocked_balance());
    }

    @Test
    @DisplayName("Trừ tiền thất bại khi balance không đủ")
    void deductLockedFunds_balanceNotEnough() {
        seedAccount(1L, 500, 500);
        assertThrows(RuntimeException.class, () -> accountService.deductLockedFunds(1L, 600));
    }

    @Test
    @DisplayName("getAvailable trả về balance - locked")
    void getAvailable_computesCorrectly() {
        seedAccount(1L, 1000, 350);
        assertEquals(650, accountService.getAvailable(1L));
    }
}