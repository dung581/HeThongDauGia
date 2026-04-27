package Server.service;

import Common.DataBase.entities.Item;
import Common.DataBase.repository.ItemRepository;
import Common.Enum.ItemStatus;

import java.util.List;

public class ItemService {

    private ItemRepository repo = new ItemRepository();

    // =====================
    // 1. upload
    // =====================
    public Item upload(Item item) {

        // user upload → luôn PENDING
        item.setStatus(ItemStatus.PENDING);

        repo.saveItem(item);

        return item;
    }

    // =====================
    // 2. approve (admin)
    // =====================
    public void approve(long itemId) {

        Item item = repo.getItemById(itemId);

        if (item == null) {
            throw new RuntimeException("Item not found");
        }

        if (item.getStatus() != ItemStatus.PENDING) {
            throw new RuntimeException("Item is not in PENDING state");
        }

        // duyệt → APPROVED
        item.setStatus(ItemStatus.APPROVED);

        repo.update(item);
    }

    // =====================
    // 3. list pending (admin duyệt)
    // =====================
    public List<Item> listPending() {
        return repo.getByStatus(ItemStatus.PENDING);
    }

    // =====================
    // 4. list approved (ready for auction)
    // =====================
    public List<Item> listApproved() {
        return repo.getByStatus(ItemStatus.APPROVED);
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
