package Server.service.Exceptions;

public class PasswordIsBlankException extends Exception {
    public PasswordIsBlankException(String message) {
        super(message);
    }
}
