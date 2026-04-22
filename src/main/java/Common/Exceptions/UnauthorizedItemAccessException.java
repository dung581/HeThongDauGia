package Common.Exceptions;

public class UnauthorizedItemAccessException extends RuntimeException {
    public UnauthorizedItemAccessException(String message) {
        super(message);
    }
}
