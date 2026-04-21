package Common.DataBase.repository;

import Common.DataBase.ConnectionDatabase;
import Common.DataBase.entities.autobid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AutobidRepository {
    public List<autobid> getAllAutobid() {
        List<autobid> autobids = new ArrayList<>();
        String sql = "SELECT * FROM autobid";
        ConnectionDatabase db = new ConnectionDatabase();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                autobid a = new autobid();
                a.setId(rs.getLong("id"));
                a.setAution_id(rs.getLong("session_id"));
                a.setUser_id(rs.getLong("user_id"));
                a.setItem_id(rs.getLong("item_id"));
                a.setMax_price(rs.getLong("max_price"));
                a.set_active(rs.getBoolean("is_active"));
                autobids.add(a);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return autobids;
    }
    public void saveAutobid(autobid a) {
        ConnectionDatabase db = new ConnectionDatabase(); // Biến db trong hàm
        String sql = "INSERT INTO autobid (session_id, user_id, item_id, max_price, is_active) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, a.getAution_id());
            ps.setLong(2, a.getUser_id());
            ps.setLong(3, a.getItem_id());
            ps.setLong(4, a.getMax_price());
            ps.setBoolean(5, a.is_active());
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}