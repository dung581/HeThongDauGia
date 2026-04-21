package Common.DataBase.repository;



import Common.DataBase.ConnectionDatabase;
import Common.DataBase.entities.Item;
import Common.DataBase.entities.ItemStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ItemRepository {
    public List<Item> getAllItem() {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM Item";
        ConnectionDatabase db = new ConnectionDatabase();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Item i = new Item();
                i.setId(rs.getLong("id"));
                i.setWinner_id(rs.getLong("winner_id"));
                i.setBeginPrice(rs.getLong("beginPrice"));
                i.setStatus(ItemStatus.valueOf(rs.getString("status")));
                items.add(i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }
}