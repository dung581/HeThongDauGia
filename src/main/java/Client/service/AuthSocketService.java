package Client.service;

import Client.net.TcpJsonClient;
import Common.DataBase.entities.User;
import Common.Enum.UserRole;
import Common.Model.net.SocketResponse;
import Server.service.Exceptions.DataAccessException;
import Server.service.Exceptions.PasswordIsBlankException;
import Server.service.Exceptions.ReturnMessage;
import Server.service.Exceptions.UserNotFoundException;
import Server.service.Exceptions.UsernameAlreadyExistsException;
import Server.service.Exceptions.UsernameIsBlankException;
import Server.service.Exceptions.WrongPasswordException;

import java.util.HashMap;
import java.util.Map;

public class AuthSocketService {
    private static final String HOST = "127.0.0.1"; //192.168.1.185
    private static final int PORT = 8888;
    private final TcpJsonClient client;

    public AuthSocketService() {
        this.client = new TcpJsonClient(HOST, PORT);
    }

    public User login(String username, String password)
            throws UsernameIsBlankException, UserNotFoundException, WrongPasswordException, PasswordIsBlankException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("password", password);
        SocketResponse response = client.send("auth.login", payload);
        if (!response.isSuccess()) {
            throwMappedLoginException(response.getMessage());
        }
        return toUser(response);
    }

    public User register(String username, String password, UserRole role)
            throws UsernameIsBlankException, UsernameAlreadyExistsException, PasswordIsBlankException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("password", password);
        payload.put("role", role == null ? UserRole.BIDDER.name() : role.name());
        SocketResponse response = client.send("auth.register", payload);
        if (!response.isSuccess()) {
            throwMappedRegisterException(response.getMessage());
        }
        return toUser(response);
    }

    private User toUser(SocketResponse response) {
        Map<String, Object> data = response.getData();
        User user = new User();
        user.setId(((Number) data.get("id")).longValue());
        user.setUsername(String.valueOf(data.get("username")));
        user.setFullname(String.valueOf(data.get("fullname")));
        user.setRole(UserRole.valueOf(String.valueOf(data.get("role"))));
        return user;
    }

    private void throwMappedLoginException(String message)
            throws UsernameIsBlankException, UserNotFoundException, WrongPasswordException, PasswordIsBlankException {
        if (message == null) throw new DataAccessException("Lỗi không xác định", null);
        if (message.contains(ReturnMessage.USERNAME_IS_BLANK)) throw new UsernameIsBlankException(message);
        if (message.contains(ReturnMessage.USER_NOT_FOUND)) throw new UserNotFoundException(message);
        if (message.contains(ReturnMessage.WRONG_PASSWORD)) throw new WrongPasswordException(message);
        if (message.contains(ReturnMessage.PASSWORD_IS_BLANK)) throw new PasswordIsBlankException(message);
        throw new DataAccessException(message, null);
    }

    private void throwMappedRegisterException(String message)
            throws UsernameIsBlankException, UsernameAlreadyExistsException, PasswordIsBlankException {
        if (message == null) throw new DataAccessException("Lỗi không xác định", null);
        if (message.contains(ReturnMessage.USERNAME_IS_BLANK)) throw new UsernameIsBlankException(message);
        if (message.contains(ReturnMessage.USERNAME_ALREADY_EXISTS)) throw new UsernameAlreadyExistsException(message);
        if (message.contains(ReturnMessage.PASSWORD_IS_BLANK) || message.contains(ReturnMessage.PASSWORD_LENGTH_INVALID)) {
            throw new PasswordIsBlankException(message);
        }
        throw new DataAccessException(message, null);
    }
}

