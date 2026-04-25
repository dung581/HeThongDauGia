package Common.Model.user;

import Common.DataBase.entities.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResult {
    private final boolean success;
    private final String message;
    private final User user;

    public static AuthResult ok(String message, User user) {
        return new AuthResult(true, message, user);
    }

    public static AuthResult fail(String message) {
        return new AuthResult(false, message, null);
    }
}
