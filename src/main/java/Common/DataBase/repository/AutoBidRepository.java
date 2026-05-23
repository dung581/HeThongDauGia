package Common.DataBase.repository;

import Common.DataBase.DbConnection;
import Common.DataBase.entities.Autobid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AutoBidRepository {

    DbConnection db = new DbConnection();

    private Autobid map(ResultSet rs) throws Exception {
        Autobid a = new Autobid();
        a.setId(rs.getLong("id"));
        a.setUser_id(rs.getLong("user_id"));
        a.setItem_id(rs.getLong("item_id"));
        a.setMax_price(rs.getLong("max_price"));
        a.set_active(rs.getBoolean("is_active"));
        return a;
    }

    public Autobid saveAutobid(Autobid a) {

        String sql = """
            INSERT INTO autobid
            (user_id, item_id, max_price, is_active)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, a.getUser_id());
            ps.setLong(2, a.getItem_id());
            ps.setLong(3, a.getMax_price());
            ps.setBoolean(4, a.is_active());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("Cannot save AutoBid");
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    a.setId(keys.getLong(1));
                }
            }

            return a;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Autobid> getActiveByItemId(long itemId) {

        List<Autobid> list = new ArrayList<>();

        String sql = """
            SELECT * FROM autobid
            WHERE item_id = ? AND is_active = true
        """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, itemId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                list.add(map(rs));
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

    public void updateMaxAndActive(long id, long maxPrice, boolean isActive) {

        String sql = "UPDATE autobid SET max_price = ?, is_active = ? WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, maxPrice);
            ps.setBoolean(2, isActive);
            ps.setLong(3, id);

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

                list.add(map(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    public Autobid getLatestByUserAndItem(long userId, long itemId) {
        String sql = """
            SELECT *
            FROM autobid
            WHERE user_id = ? AND item_id = ?
            ORDER BY id DESC
            LIMIT 1
        """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setLong(2, itemId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return map(rs);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    // them 5p neu dat gia qua gan endtime
    public void updateEndTime(long auctionId, LocalDateTime newEndTime) {
        String sql = "UPDATE auction SET end_time = ? WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(newEndTime));
            ps.setLong(2, auctionId);

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("Auction not found");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
