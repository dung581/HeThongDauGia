package Common.Exceptions;

public class ItemLockedException extends RuntimeException {
    public ItemLockedException(String message) {
        super(message);
    }
}
