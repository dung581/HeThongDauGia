package Server.service.Exceptions;

public class ItemLockedException extends RuntimeException {
    public ItemLockedException(String message) {
        super(message);
    }
}
