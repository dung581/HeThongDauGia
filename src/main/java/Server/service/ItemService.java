package Server.service;

import Common.DataBase.entities.Item;
import Common.DataBase.repository.ItemRepository;
import Common.Enum.ItemStatus;

import java.util.List;

public class ItemService {

    private final ItemRepository repo = new ItemRepository();

    public Item upload(Item item) {
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

    public List<Item> listAllPaged(int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(pageSize, 1);
        int offset = (safePage - 1) * safeSize;
        return repo.getAllPaged(safeSize, offset);
    }

    public List<Item> listByOwnerPaged(long ownerUserId, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(pageSize, 1);
        int offset = (safePage - 1) * safeSize;
        return repo.getByOwnerUserIdPaged(ownerUserId, safeSize, offset);
    }

    public int countAll() {
        return repo.countAll();
    }

    public int countByOwner(long ownerUserId) {
        return repo.countByOwnerUserId(ownerUserId);
    }

    public void markSold(long itemId) {
        Item item = repo.getItemById(itemId);

        if (item == null) {
            throw new RuntimeException("Item not found");
        }

        item.setStatus(ItemStatus.SOLD);
        repo.update(item);
    }

    public Item getById(long itemId) {
        Item item = repo.getItemById(itemId);

        if (item == null) {
            throw new RuntimeException("Item not found");
        }

        return item;
    }

    public void deleteById(long itemId) {
        Item item = repo.getItemById(itemId);
        if (item == null) {
            throw new RuntimeException("Item not found");
        }
        if (item.getStatus() == ItemStatus.IN_AUCTION || item.getStatus() == ItemStatus.SOLD) {
            throw new RuntimeException("Khong the xoa san pham dang dau gia hoac da ban");
        }
        repo.deleteById(itemId);
    }
}
