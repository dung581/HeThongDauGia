package Server.service;

import Common.DataBase.entities.Account;
import Common.DataBase.entities.User;
import Common.Enum.UserRole;
import Common.util.PasswordUtil;
import Server.service.Exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import testutil.FakeRepositories.FakeAccountRepository;
import testutil.FakeRepositories.FakeUserRepository;
import testutil.TestReflection;

import static org.junit.jupiter.api.Assertions.*;

// Test Xác thực (Đăng nhập)
@DisplayName("Giai đoạn 1: Xác thực (AuthService)")
class AuthServiceTest {

    private AuthService authService;
    private FakeUserRepository userRepo;
    private FakeAccountRepository accountRepo;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
        userRepo = new FakeUserRepository();
        accountRepo = new FakeAccountRepository();
        TestReflection.setField(authService, "userRepository", userRepo);
        TestReflection.setField(authService, "accountRepository", accountRepo);
    }

    /** Seed user với mật khẩu đã băm BCrypt (giống dữ liệu thật trong DB). */
    private void seedUser(long id, String username, String plainPassword) {
        User u = new User(id, username, PasswordUtil.hash(plainPassword), UserRole.BIDDER, username);
        userRepo.put(u);
    }

    @Nested
    @DisplayName("Đăng nhập (login)")
    class Login {

        @Test
        @DisplayName("Đăng nhập thành công khi đúng tài khoản và mật khẩu")
        void login_success() throws Exception {
            seedUser(1L, "alice", "secret123");

            User result = authService.login("alice", "secret123");

            assertNotNull(result);
            assertEquals("alice", result.getUsername());
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("Tự cắt khoảng trắng username trước khi tra cứu")
        void login_trimsUsername() throws Exception {
            seedUser(1L, "alice", "secret123");

            User result = authService.login("  alice  ", "secret123");

            assertEquals("alice", result.getUsername());
        }

        @Test
        @DisplayName("Ném UsernameIsBlankException khi username rỗng/null/toàn dấu cách")
        void login_blankUsername() {
            assertThrows(UsernameIsBlankException.class, () -> authService.login("", "secret123"));
            assertThrows(UsernameIsBlankException.class, () -> authService.login(null, "secret123"));
            assertThrows(UsernameIsBlankException.class, () -> authService.login("   ", "secret123"));
        }

        @Test
        @DisplayName("Ném PasswordIsBlankException khi mật khẩu rỗng")
        void login_blankPassword() {
            assertThrows(PasswordIsBlankException.class, () -> authService.login("alice", ""));
            assertThrows(PasswordIsBlankException.class, () -> authService.login("alice", null));
        }

        @Test
        @DisplayName("Ném UserNotFoundException khi không tìm thấy tài khoản")
        void login_userNotFound() {
            assertThrows(UserNotFoundException.class, () -> authService.login("ghost", "secret123"));
        }

        @Test
        @DisplayName("Ném WrongPasswordException khi sai mật khẩu")
        void login_wrongPassword() {
            seedUser(1L, "alice", "correct123");
            assertThrows(WrongPasswordException.class, () -> authService.login("alice", "wrong999"));
        }

        @Test
        @DisplayName("Mật khẩu lưu dạng plaintext (chưa băm) vẫn đăng nhập được và được nâng cấp thành băm")
        void login_upgradesPlaintextPassword() throws Exception {
            // Seed thẳng mật khẩu plaintext (không băm) để mô phỏng dữ liệu cũ
            User u = new User(1L, "bob", "plain12", UserRole.BIDDER, "bob");
            userRepo.put(u);

            User result = authService.login("bob", "plain12");

            assertNotNull(result);
            // Đã gọi updatePassword để nâng cấp sang băm
            assertEquals(1L, userRepo.lastUpdatePwdUserId.longValue());
            assertTrue(PasswordUtil.isBCryptHash(userRepo.lastUpdatePwdValue));
        }
    }

    @Nested
    @DisplayName("Đăng ký (register)")
    class Register {

        @Test
        @DisplayName("Đăng ký thành công, tạo Account đi kèm, mật khẩu được băm")
        void register_success() throws Exception {
            User result = authService.register("bob", "pass12", "Bob", UserRole.BIDDER);

            assertNotNull(result);
            assertEquals(1, userRepo.createCalls);
            assertEquals(1, accountRepo.createCalls);
            // Mật khẩu lưu phải là băm BCrypt, không phải plaintext
            assertTrue(PasswordUtil.isBCryptHash(result.getPassword()));
            assertNotEquals("pass12", result.getPassword());
        }

        @Test
        @DisplayName("Account khởi tạo có balance > 0 và locked_balance = 0")
        void register_createsAccountWithDefaults() throws Exception {
            User created = authService.register("bob", "pass12", "Bob", UserRole.BIDDER);

            Account acc = accountRepo.byUserId.get(created.getId());
            assertNotNull(acc);
            assertTrue(acc.getBalance() > 0);
            assertEquals(0L, acc.getLocked_balance());
            assertEquals(created.getId(), acc.getUser_id());
        }

        @Test
        @DisplayName("Mặc định role BIDDER khi truyền null")
        void register_defaultRoleBidder() throws Exception {
            authService.register("bob", "pass12", "Bob", null);
            assertEquals(UserRole.BIDDER, userRepo.lastCreated.getRole());
        }

        @Test
        @DisplayName("Dùng username làm fullname khi fullname trống")
        void register_fullnameFallsBackToUsername() throws Exception {
            authService.register("bob", "pass12", "  ", UserRole.BIDDER);
            assertEquals("bob", userRepo.lastCreated.getFullname());
        }

        @Test
        @DisplayName("Ném UsernameIsBlankException khi username rỗng")
        void register_blankUsername() {
            assertThrows(UsernameIsBlankException.class,
                    () -> authService.register("", "pass12", "Bob", UserRole.BIDDER));
        }

        @Test
        @DisplayName("Ném PasswordIsBlankException khi mật khẩu rỗng")
        void register_blankPassword() {
            assertThrows(PasswordIsBlankException.class,
                    () -> authService.register("bob", "", "Bob", UserRole.BIDDER));
        }

        @Test
        @DisplayName("Ném lỗi khi mật khẩu ngắn hơn 6 hoặc dài hơn 10 ký tự")
        void register_passwordLengthInvalid() {
            assertThrows(PasswordIsBlankException.class,
                    () -> authService.register("bob", "123", "Bob", UserRole.BIDDER));
            assertThrows(PasswordIsBlankException.class,
                    () -> authService.register("bob", "12345678901", "Bob", UserRole.BIDDER));
        }

        @Test
        @DisplayName("Chấp nhận mật khẩu ở biên 6 và 10 ký tự")
        void register_passwordBoundary() {
            assertDoesNotThrow(() -> authService.register("u6", "123456", "U6", UserRole.BIDDER));
            assertDoesNotThrow(() -> authService.register("u10", "1234567890", "U10", UserRole.BIDDER));
        }

        @Test
        @DisplayName("Ném UsernameAlreadyExistsException khi tên đã tồn tại")
        void register_duplicateUsername() {
            seedUser(5L, "bob", "existing1");
            assertThrows(UsernameAlreadyExistsException.class,
                    () -> authService.register("bob", "pass12", "Bob", UserRole.BIDDER));
            assertEquals(0, userRepo.createCalls);
        }
    }
}