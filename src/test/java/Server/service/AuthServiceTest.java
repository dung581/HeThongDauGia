package Server.service;

import Common.DataBase.entities.User;
import Common.Enum.UserRole;
import Server.service.Exceptions.PasswordIsBlankException;
import Server.service.Exceptions.UserNotFoundException;
import Server.service.Exceptions.UsernameAlreadyExistsException;
import Server.service.Exceptions.UsernameIsBlankException;
import Server.service.Exceptions.WrongPasswordException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    private AuthService authService;

    @BeforeEach
    public void setUp() {
        authService = new AuthService();
    }

    @Test
    public void testRegisterSuccess() throws Exception {
        String uniqueUsername = "test_reg_" + System.currentTimeMillis();
        String password = "pass123";
        String fullname = "Nguyễn Văn Test";
        UserRole role = UserRole.BIDDER;

        User user = authService.register(uniqueUsername, password, fullname, role);

        assertNotNull(user);
        assertTrue(user.getId() > 0, "User ID should be generated and greater than 0");
        assertEquals(uniqueUsername, user.getUsername());
        assertEquals(fullname, user.getFullname());
        assertEquals(role, user.getRole());
    }

    @Test
    public void testRegisterUsernameBlank() {
        assertThrows(UsernameIsBlankException.class, () -> {
            authService.register("", "pass123", "FullName", UserRole.BIDDER);
        }, "Should throw UsernameIsBlankException for empty username");

        assertThrows(UsernameIsBlankException.class, () -> {
            authService.register(null, "pass123", "FullName", UserRole.BIDDER);
        }, "Should throw UsernameIsBlankException for null username");
    }

    @Test
    public void testRegisterPasswordBlank() {
        assertThrows(PasswordIsBlankException.class, () -> {
            authService.register("someuser", "", "FullName", UserRole.BIDDER);
        }, "Should throw PasswordIsBlankException for empty password");

        assertThrows(PasswordIsBlankException.class, () -> {
            authService.register("someuser", null, "FullName", UserRole.BIDDER);
        }, "Should throw PasswordIsBlankException for null password");
    }

    @Test
    public void testRegisterPasswordLengthInvalid() {
        assertThrows(PasswordIsBlankException.class, () -> {
            authService.register("someuser", "123", "FullName", UserRole.BIDDER);
        }, "Should throw PasswordIsBlankException for password less than 6 chars");

        assertThrows(PasswordIsBlankException.class, () -> {
            authService.register("someuser", "12345678901", "FullName", UserRole.BIDDER);
        }, "Should throw PasswordIsBlankException for password greater than 10 chars");
    }

    @Test
    public void testRegisterUsernameAlreadyExists() throws Exception {
        String existingUsername = "dup_" + System.currentTimeMillis();
        authService.register(existingUsername, "pass123", "FullName", UserRole.BIDDER);

        assertThrows(UsernameAlreadyExistsException.class, () -> {
            authService.register(existingUsername, "pass123", "FullName", UserRole.BIDDER);
        }, "Should throw UsernameAlreadyExistsException for duplicate username");
    }

    @Test
    public void testLoginSuccess() throws Exception {
        String uniqueUsername = "test_login_" + System.currentTimeMillis();
        String password = "pass123";
        authService.register(uniqueUsername, password, "FullName", UserRole.BIDDER);

        User loggedInUser = authService.login(uniqueUsername, password);

        assertNotNull(loggedInUser);
        assertEquals(uniqueUsername, loggedInUser.getUsername());
    }

    @Test
    public void testLoginUserNotFound() {
        assertThrows(UserNotFoundException.class, () -> {
            authService.login("non_existing_user_" + System.currentTimeMillis(), "pass123");
        }, "Should throw UserNotFoundException for non-existing username");
    }

    @Test
    public void testLoginWrongPassword() throws Exception {
        String uniqueUsername = "wrong_pass_" + System.currentTimeMillis();
        String password = "pass123";
        authService.register(uniqueUsername, password, "FullName", UserRole.BIDDER);

        assertThrows(WrongPasswordException.class, () -> {
            authService.login(uniqueUsername, "wrongpw");
        }, "Should throw WrongPasswordException for incorrect password");
    }
}
