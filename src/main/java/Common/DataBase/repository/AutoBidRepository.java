package Common.DataBase.repository;

import Common.DataBase.DbConnection;
import Common.DataBase.entities.Autobid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AutoBidRepository {
    DbConnection db= new DbConnection();
    public List<Autobid> getAllAutobid() {
        List<Autobid> Autobids = new ArrayList<>();
        String sql = "SELECT * FROM Autobid";
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
                Autobids.add(a);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Autobids;
    }
    public void saveAutobid(Autobid a) {
        String sql = "INSERT INTO Autobid (session_id, user_id, item_id, max_price, is_active) VALUES (?, ?, ?, ?, ?)";
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

    public List<Autobid> getActiveByItemId(long itemId) {
        List<Autobid> list = new ArrayList<>();

        String sql = """
        SELECT * FROM Autobid 
        WHERE item_id = ? AND is_active = true
    """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, itemId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Autobid a = new Autobid();
                a.setId(rs.getLong("id"));
                a.setUser_id(rs.getLong("user_id"));
                a.setItem_id(rs.getLong("item_id"));
                a.setMax_price(rs.getLong("max_price"));
                a.set_active(rs.getBoolean("is_active"));
                list.add(a);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return list;
    }
    public void updateActive(long id, boolean isActive) {
        String sql = "UPDATE autobid SET is_active = ? WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, isActive);
            ps.setLong(2, id);

            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new RuntimeException("AutoBid not found");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public List<Autobid> getByUserId(long userId) {
        List<Autobid> list = new ArrayList<>();

        String sql = "SELECT * FROM autobid WHERE user_id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Autobid a = new Autobid();

                a.setId(rs.getLong("id"));
                a.setUser_id(rs.getLong("user_id"));
                a.setItem_id(rs.getLong("item_id"));
                a.setMax_price(rs.getLong("max_price"));
                a.set_active(rs.getBoolean("is_active"));

                list.add(a);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return list;
    }
}