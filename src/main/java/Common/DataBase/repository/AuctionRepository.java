package Common.DataBase.repository;

import Common.DataBase.DbConnection;
import Common.DataBase.entities.Auction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AuctionRepository {
    public List<Auction> getAllSession() {
        List<Auction> sessions = new ArrayList<>();
        String sql = "SELECT * FROM session";
        DbConnection db = new DbConnection();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Auction s = new Auction();
                s.setId(rs.getLong("id"));
                s.setItem_id(rs.getLong("item_id"));
                s.setCurrent_user_id(rs.getLong("current_user_id"));
                s.setCurrent_price(rs.getLong("current_price"));

                Timestamp ts = rs.getTimestamp("availability_time");
                if (ts != null) {
                    s.setAvailability_time(ts.toLocalDateTime());
                }

                sessions.add(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sessions;
    }
    public void savAution(Auction s) {
        DbConnection db = new DbConnection(); // Biến db trong hàm
        String sql = "INSERT INTO session (item_id, current_user_id, current_price, availability_time) VALUES (?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, s.getItem_id());
            ps.setLong(2, s.getCurrent_user_id());
            ps.setLong(3, s.getCurrent_price());
            ps.setTimestamp(4, java.sql.Timestamp.valueOf(s.getAvailability_time()));
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}