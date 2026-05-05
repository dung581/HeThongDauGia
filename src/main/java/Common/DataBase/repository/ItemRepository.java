package Common.DataBase.repository;

import Common.DataBase.DbConnection;
import Common.DataBase.entities.Item;
import Common.Enum.ItemStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ItemRepository {
    DbConnection db = new DbConnection();

    private Item map(ResultSet rs) throws Exception {
        Item i = new Item();
        i.setId(rs.getLong("id"));
        i.setFullname(rs.getString("fullname"));
        i.setOwner_user_id(rs.getLong("owner_user_id"));
        i.setDescription(rs.getString("description"));
        i.setBeginPrice(rs.getLong("beginPrice"));
        i.setMota(rs.getString("mota"));
        i.setStatus(ItemStatus.valueOf(rs.getString("status")));
        return i;
    }

    public List<Item> getAllItem() {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM Item";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                items.add(map(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    public void saveItem(Item item) {
        String sql = "INSERT INTO Item (fullname, owner_user_id, description, beginPrice, mota, status) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getFullname());
            ps.setLong(2, item.getOwner_user_id());
            ps.setString(3, item.getDescription());
            ps.setLong(4, item.getBeginPrice());
            ps.setString(5, item.getMota());
            ps.setString(6, item.getStatus().toString());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Item getItemById(long id) {
        String sql = "SELECT * FROM item WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return map(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void update(Item item) {
        String sql = "UPDATE item SET fullname = ?, owner_user_id = ?, description = ?, beginPrice = ?, mota = ?, status = ? WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, item.getFullname());
            ps.setLong(2, item.getOwner_user_id());
            ps.setString(3, item.getDescription());
            ps.setLong(4, item.getBeginPrice());
            ps.setString(5, item.getMota());
            ps.setString(6, item.getStatus().name());
            ps.setLong(7, item.getId());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("Item not found");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updateStatus(long id, ItemStatus status) {
        String sql = "UPDATE item SET status = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, id);

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("Item not found, id =" + id);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void rejectWithReason(long id, String reason) {
        String sql = "UPDATE item SET status = ?, mota = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ItemStatus.REJECTED.name());
            ps.setString(2, reason);
            ps.setLong(3, id);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("Item not found, id =" + id);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Item> getByStatus(ItemStatus status) {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT * FROM item WHERE status = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    public List<Item> getByOwnerUserId(long ownerUserId) {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT * FROM item WHERE owner_user_id = ? ORDER BY id DESC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, ownerUserId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
