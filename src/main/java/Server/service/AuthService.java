package Server.service;

import Common.DataBase.entities.Account;
import Common.DataBase.entities.User;
import Common.DataBase.repository.AccountRepository;
import Common.DataBase.repository.UserRepository;
import Common.Enum.UserRole;
import Common.util.PasswordUtil;
import Server.service.Exceptions.*;

public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public AuthService() {
        this.userRepository = new UserRepository();
        this.accountRepository = new AccountRepository();
    }

    // tránh đặt tk, mk là dấu cách
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public User login(String username, String password)
            throws UsernameIsBlankException, UserNotFoundException, WrongPasswordException, PasswordIsBlankException {
        if (isBlank(username)) {
            throw new UsernameIsBlankException(ReturnMessage.USERNAME_IS_BLANK);
        }

        if (isBlank(password)) {
            throw new PasswordIsBlankException(ReturnMessage.PASSWORD_IS_BLANK);
        }

        User user;
        try {
            user = userRepository.findByUsername(username.trim());
        } catch (RuntimeException e) {
            throw new DataAccessException(ReturnMessage.DATA_ACCESS, e);
        }
        if (user == null) {
            throw new UserNotFoundException(ReturnMessage.USER_NOT_FOUND);
        }

        if (!PasswordUtil.verify(password, user.getPassword())) {
            throw new WrongPasswordException(ReturnMessage.WRONG_PASSWORD);
        }
        if (!PasswordUtil.isBCryptHash(user.getPassword())) {
            try {
                userRepository.updatePassword(user.getId(), PasswordUtil.hash(password));
            } catch (RuntimeException e) {
                throw new DataAccessException(ReturnMessage.DATA_ACCESS, e);
            }
        }

        return user;
    }

    public User register(String username, String password, String fullname, UserRole role)
            throws UsernameIsBlankException, UsernameAlreadyExistsException, PasswordIsBlankException {
        if (isBlank(username)) {
            throw new UsernameIsBlankException(ReturnMessage.USERNAME_IS_BLANK);
        }

        if (isBlank(password)){
            throw new PasswordIsBlankException(ReturnMessage.PASSWORD_IS_BLANK);
        }

        if (password.length() < 6 || password.length() > 10) {
            throw new PasswordIsBlankException(ReturnMessage.PASSWORD_LENGTH_INVALID);
        }

        String normalizedUsername = username.trim();
        try {
            if (userRepository.existsByUsername(normalizedUsername)) {
                throw new UsernameAlreadyExistsException(ReturnMessage.USERNAME_ALREADY_EXISTS);
            }
        } catch (UsernameAlreadyExistsException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new DataAccessException(ReturnMessage.DATA_ACCESS, e);
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPassword(PasswordUtil.hash(password));
        user.setRole(role == null ? UserRole.BIDDER : role);
        user.setFullname(isBlank(fullname) ? normalizedUsername : fullname.trim());

        User created;
        try {
            created = userRepository.createUser(user);
        } catch (RuntimeException e) {
            throw new DataAccessException(ReturnMessage.DATA_ACCESS, e);
        }

        Account account = new Account();
        account.setUser_id(created.getId());
        account.setBalance(1000_000_000_000L);
        account.setLocked_balance(0L);
        try {
            accountRepository.CreateAccount(account);
        } catch (RuntimeException e) {
            throw new DataAccessException(ReturnMessage.DATA_ACCESS, e);
        }

        return created;
    }
}
