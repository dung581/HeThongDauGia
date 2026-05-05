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
}
