package Common.DataBase.repository;


import Common.DataBase.ConnectionDatabase;
import Common.Enum.ItemStatus;
import Common.DataBase.entities.Stake;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class StakeRepository {
    public List<Stake> getAllStake() {
        List<Stake> stakes = new ArrayList<>();
        String sql = "SELECT * FROM Stake";
        ConnectionDatabase db = new ConnectionDatabase();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Stake s = new Stake();
                s.setId(rs.getLong("id"));
                s.setSession_id(rs.getLong("session_id"));
                s.setLocked_item_id(rs.getLong("locked_item_id"));
                s.setUser_id(rs.getLong("user_id"));
                s.setAmount(rs.getLong("amount"));
                s.setStatus(ItemStatus.valueOf(rs.getString("status")));
                stakes.add(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stakes;
    }
}