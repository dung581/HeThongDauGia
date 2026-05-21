package Common.DataBase.repository;

import Common.DataBase.DbConnection;
import Common.DataBase.entities.Bid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class BidRepository {

    private static DbConnection db = new DbConnection();

    private Bid mapResultSet(ResultSet rs) throws Exception {
        Bid b = new Bid();
        b.setId(rs.getLong("id"));
        b.setAuction_id(readLong(rs, "session_id", "auction_id"));
        b.setUser_id(rs.getLong("user_id"));
        b.setItem_id(readLong(rs, "items_id", "item_id"));
        b.setPrice(rs.getLong("price"));

        Timestamp createdAt = readTimestamp(rs, "created_at");
        if (createdAt != null) {
            b.setCreated_at(createdAt.toLocalDateTime());
        }
        return b;
    }

    private long readLong(ResultSet rs, String primaryColumn, String fallbackColumn) throws Exception {
        try {
            return rs.getLong(primaryColumn);
        } catch (Exception ignored) {
            return rs.getLong(fallbackColumn);
        }
    }

    private Timestamp readTimestamp(ResultSet rs, String column) {
        try {
            return rs.getTimestamp(column);
        } catch (Exception ignored) {
            return null;
        }
    }

    public List<Bid> getAllBid() {

        List<Bid> bids = new ArrayList<>();

        String sql = "SELECT * FROM bid";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                bids.add(mapResultSet(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return bids;
    }

    public void saveBid(Bid b) {

        String sql = """
            INSERT INTO bid
            (session_id, user_id, items_id, price)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, b.getAuction_id());
            ps.setLong(2, b.getUser_id());
            ps.setLong(3, b.getItem_id());
            ps.setLong(4, b.getPrice());

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Bid> getByItemId(long itemId) {

        List<Bid> list = new ArrayList<>();

        String sql = """
            SELECT * FROM bid
            WHERE items_id = ?
            ORDER BY id ASC
        """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, itemId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                list.add(mapResultSet(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    public List<Bid> getBySessionId(long sessionId) {

        List<Bid> list = new ArrayList<>();

        String sql = """
            SELECT *
            FROM bid
            WHERE session_id = ?
            ORDER BY id ASC
        """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, sessionId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapResultSet(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return list;
    }
}
