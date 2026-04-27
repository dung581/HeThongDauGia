package Common.DataBase.repository;



import Common.DataBase.DbConnection;
import Common.DataBase.entities.Item;
import Common.Enum.ItemStatus;
import Common.Enum.StakeStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ItemRepository {
    DbConnection db= new DbConnection();
    public List<Item> getAllItem() {
        List<Item> ItemS = new ArrayList<>();
        String sql = "SELECT * FROM Item";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Item i = new Item();
                i.setId(rs.getLong("id"));
                i.setFullname(rs.getString("fullname"));
                i.setOwner_user_id(rs.getLong("owner_user_id"));
                i.setBeginPrice(rs.getLong("beginPrice"));
                i.setStatus(ItemStatus.valueOf(rs.getString("status")));
                ItemS.add(i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ItemS;
    }
    public void saveItem(Item item) {
        String sql = "INSERT INTO Item (fullname, owner_user_id, beginPrice, status) VALUES (?, ?, ?, ?)";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getFullname());
            ps.setLong(2, item.getOwner_user_id());
            ps.setLong(3, item.getBeginPrice());
            ps.setString(4, item.getStatus().toString());
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
    public Item getItemById(long id) {
        String sql = "SELECT * FROM item WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Item i = new Item();
                i.setId(rs.getLong("id"));
                i.setOwner_user_id(rs.getLong("owner_user_id"));
                i.setBeginPrice(rs.getLong("begin_price"));
                i.setStatus(ItemStatus.valueOf(rs.getString("status")));
                return i;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public void update(Item item) {

        String sql = """
        UPDATE item
        SET 
            status = ?
        WHERE id = ?
    """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, item.getOwner_user_id());
            ps.setLong(2, item.getBeginPrice());
            ps.setString(3, item.getStatus().name());
            ps.setLong(4, item.getId());

            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new RuntimeException("Item not found");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public List<Item> getByStatus(ItemStatus status) {

        List<Item> list = new ArrayList<>();

        String sql = "SELECT * FROM item WHERE status = ?";

        try (Connection conn =db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Item item = new Item();

                item.setId(rs.getLong("id"));
                item.setOwner_user_id(rs.getLong("owner_user_id"));
                item.setBeginPrice(rs.getLong("beginPrice"));
                item.setStatus(
                        ItemStatus.valueOf(rs.getString("status"))
                );

                list.add(item);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return list;
    }
}