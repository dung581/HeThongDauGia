package Common.DataBase.repository;

import Common.DataBase.DbConnection;
import Common.DataBase.entities.Autobid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AutoBidRepository {
    public List<Autobid> getAllAutobid() {
        List<Autobid> autobids = new ArrayList<>();
        String sql = "SELECT * FROM autobid";
        DbConnection db = new DbConnection();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Autobid a = new Autobid();
                a.setId(rs.getLong("id"));
                a.setAuction_id(rs.getLong("session_id"));
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
    public void saveAutobid(Autobid a) {
        DbConnection db = new DbConnection(); // Biến db trong hàm
        String sql = "INSERT INTO autobid (session_id, user_id, item_id, max_price, is_active) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, a.getAuction_id());
            ps.setLong(2, a.getUser_id());
            ps.setLong(3, a.getItem_id());
            ps.setLong(4, a.getMax_price());
            ps.setBoolean(5, a.is_active());
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
    public Autobid getAutobidById(long id) {
        String sql = "SELECT * FROM autobid WHERE id = ?";
        DbConnection db = new DbConnection();

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Autobid a = new Autobid();
                a.setId(rs.getLong("id"));
                a.setAuction_id(rs.getLong("session_id"));
                a.setUser_id(rs.getLong("user_id"));
                a.setItem_id(rs.getLong("item_id"));
                a.setMax_price(rs.getLong("max_price"));
                a.set_active(rs.getBoolean("is_active"));
                return a;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // không tìm thấy
    }
}