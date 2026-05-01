package Client;

import Common.DataBase.entities.User;
import Common.Enum.UserRole;
import Server.service.AuthService;
import Server.service.Exceptions.*;

// Lop adapter de UI goi backend auth
public class Authservice {
    private final AuthService authService;

    public Authservice() {
        this.authService = new AuthService();
    }

    public User login(String username, String password)
            throws UsernameIsBlankException, UserNotFoundException, WrongPasswordException, PasswordIsBlankException {
        return authService.login(username, password);
    }

    public User register(String username, String password , UserRole role)
            throws UsernameIsBlankException, UsernameAlreadyExistsException, PasswordIsBlankException {
        return authService.register(username, password, username, role);    //sua : thay vi mac dinh la bidder thi co the chon bidder va seller
    }
}
