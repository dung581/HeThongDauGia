package Server.service.Exceptions;

public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(String itemId) {
        super("Không tìm thấy sản phẩm với ID: " +itemId);
    }
}
