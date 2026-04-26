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
    public List<Item> getAllItem() {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM Item";
        DbConnection db = new DbConnection();
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
                items.add(i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }
    public void saveItem(Item item) {
        DbConnection db = new DbConnection();
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
        DbConnection db = new DbConnection();

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
}