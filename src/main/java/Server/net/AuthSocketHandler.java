package Server.net;

import Common.DataBase.entities.User;
import Common.Enum.UserRole;
import Common.Model.net.SocketRequest;
import Common.Model.net.SocketResponse;
import Server.service.AuthService;
import Server.service.Exceptions.PasswordIsBlankException;
import Server.service.Exceptions.UserNotFoundException;
import Server.service.Exceptions.UsernameAlreadyExistsException;
import Server.service.Exceptions.UsernameIsBlankException;
import Server.service.Exceptions.WrongPasswordException;

import java.util.HashMap;
import java.util.Map;

public class AuthSocketHandler implements SocketActionHandler {
    private final AuthService authService = new AuthService();

    @Override
    public SocketResponse handle(SocketRequest request) {
        String action = request.getAction();
        if ("auth.login".equals(action)) {
            return handleLogin(request);
        }
        if ("auth.register".equals(action)) {
            return handleRegister(request);
        }
        return SocketResponse.fail("Unsupported auth action: " + action);
    }

    private SocketResponse handleLogin(SocketRequest request) {
        Map<String, Object> payload = request.getPayload();
        String username = payload == null ? null : String.valueOf(payload.get("username"));
        String password = payload == null ? null : String.valueOf(payload.get("password"));

        try {
            User user = authService.login(username, password);
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("username", user.getUsername());
            data.put("fullname", user.getFullname());
            data.put("role", user.getRole().name());
            return SocketResponse.ok("LOGIN_OK", data);
        } catch (UsernameIsBlankException | UserNotFoundException | WrongPasswordException | PasswordIsBlankException e) {
            return SocketResponse.fail(e.getMessage());
        } catch (Exception e) {
            return SocketResponse.fail("SERVER_ERROR: " + e.getMessage());
        }
    }

    private SocketResponse handleRegister(SocketRequest request) {
        Map<String, Object> payload = request.getPayload();
        String username = payload == null ? null : String.valueOf(payload.get("username"));
        String password = payload == null ? null : String.valueOf(payload.get("password"));
        String roleRaw = payload == null ? null : String.valueOf(payload.get("role"));

        try {
            UserRole role = roleRaw == null ? UserRole.BIDDER : UserRole.valueOf(roleRaw);
            User user = authService.register(username, password, username, role);
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("username", user.getUsername());
            data.put("fullname", user.getFullname());
            data.put("role", user.getRole().name());
            return SocketResponse.ok("REGISTER_OK", data);
        } catch (UsernameIsBlankException | UsernameAlreadyExistsException | PasswordIsBlankException e) {
            return SocketResponse.fail(e.getMessage());
        } catch (IllegalArgumentException e) {
            return SocketResponse.fail("Role invalid");
        } catch (Exception e) {
            return SocketResponse.fail("SERVER_ERROR: " + e.getMessage());
        }
    }
}

