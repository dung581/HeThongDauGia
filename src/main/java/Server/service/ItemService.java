package Server.service;

import Common.DataBase.entities.Item;
import Common.DataBase.repository.ItemRepository;
import Common.Enum.ItemStatus;

import java.util.List;

public class ItemService {

    private final ItemRepository repo = new ItemRepository();

    public Item upload(Item item) {
        // Service là lớp chặn dữ liệu nghiệp vụ: controller nào upload item cũng phải có giá hợp lệ.
        if (item.getBeginPrice() <= 0) {
            throw new RuntimeException("Begin price must be greater than 0");
        }
        if (item.getMinIncrement() <= 0) {
            throw new RuntimeException("Min increment must be greater than 0");
        }
        // Mọi item seller upload đều đi qua trạng thái chờ duyệt, controller không tự set rule này.
        item.setStatus(ItemStatus.PENDING);
        if (item.getDescription() == null) {
            item.setDescription("");
        }
        if (item.getMota() == null) {
            item.setMota("Cho duyet");
        }
        repo.saveItem(item);
        return item;
    }

    public void approve(long itemId) {
        Item item = repo.getItemById(itemId);
        if (item == null) {
            throw new RuntimeException("Item not found");
        }
        // Chỉ item PENDING mới được duyệt để tránh mở lại item đã hủy/đã đấu giá.
        if (item.getStatus() != ItemStatus.PENDING) {
            throw new RuntimeException("Item is not in PENDING state");
        }
        item.setStatus(ItemStatus.APPROVED);
        item.setMota("Da duyet");
        repo.update(item);
    }

    public void reject(long itemId, String reason) {
        Item item = repo.getItemById(itemId);
        if (item == null) {
            throw new RuntimeException("Item not found");
        }
        // Từ chối chỉ áp dụng trước khi item được duyệt, vì sau đó item có thể đã liên kết với phiên đấu giá.
        if (item.getStatus() != ItemStatus.PENDING) {
            throw new RuntimeException("Item is not in PENDING state");
        }
        String note = (reason == null || reason.trim().isEmpty()) ? "Khong dat yeu cau" : reason.trim();
        repo.rejectWithReason(itemId, note);
    }

    public List<Item> listPending() {
        return repo.getByStatus(ItemStatus.PENDING);
    }

    public List<Item> listApproved() {
        return repo.getByStatus(ItemStatus.APPROVED);
    }

    public List<Item> listByOwner(long ownerUserId) {
        return repo.getByOwnerUserId(ownerUserId);
    }

    public List<Item> listAll() {
        return repo.getAllItem();
    }

    public Item getById(long itemId) {
        Item item = repo.getItemById(itemId);

        if (item == null) {
            throw new RuntimeException("Item not found");
        }

        return item;
    }
}
