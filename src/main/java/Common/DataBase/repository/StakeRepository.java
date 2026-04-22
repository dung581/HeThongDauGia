package Common.DataBase.repository;


import Common.DataBase.DbConnection;
import Common.Enum.ItemStatus;
import Common.DataBase.entities.Stake;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class StakeRepository {
    public static List<Stake> getAllStake() {
        List<Stake> stakes = new ArrayList<>();
        String sql = "SELECT * FROM Stake";
        DbConnection db = new DbConnection();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Stake s = new Stake();
                s.setId(rs.getLong("id"));
                s.setAution_id(rs.getLong("session_id"));
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
    public void saveStake(Stake stake) {
        DbConnection db = new DbConnection(); // Biến db trong hàm
        String sql = "INSERT INTO Stake (session_id, locked_item_id, user_id, amount, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, stake.getAution_id());
            ps.setLong(2, stake.getLocked_item_id());
            ps.setLong(3, stake.getUser_id());
            ps.setLong(4, stake.getAmount());
            ps.setString(5, stake.getStatus().toString());
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}