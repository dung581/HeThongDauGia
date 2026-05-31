package Common.DataBase.repository;

import Common.DataBase.DbConnection;
import Common.DataBase.entities.Bid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BidRepository {

    private static final DbConnection db = new DbConnection();

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

    public void saveBid(Bid b) {
        try {
            saveBidWithCreatedAt(b);
        } catch (Exception e) {
            if (isMissingCreatedAtColumn(e)) {
                saveBidWithoutCreatedAt(b);
                return;
            }
            throw new RuntimeException(e);
        }
    }

    private void saveBidWithCreatedAt(Bid b) throws Exception {

        String sql = """
            INSERT INTO bid
            (session_id, user_id, items_id, price, created_at)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, b.getAuction_id());
            ps.setLong(2, b.getUser_id());
            ps.setLong(3, b.getItem_id());
            ps.setLong(4, b.getPrice());
            LocalDateTime createdAt = b.getCreated_at() == null ? LocalDateTime.now() : b.getCreated_at();
            ps.setTimestamp(5, Timestamp.valueOf(createdAt));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    b.setId(keys.getLong(1));
                }
            }
        }
    }

    private void saveBidWithoutCreatedAt(Bid b) {
        String sql = """
            INSERT INTO bid
            (session_id, user_id, items_id, price)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, b.getAuction_id());
            ps.setLong(2, b.getUser_id());
            ps.setLong(3, b.getItem_id());
            ps.setLong(4, b.getPrice());

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    b.setId(keys.getLong(1));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isMissingCreatedAtColumn(Exception e) {
        String message = e.getMessage();
        return message != null && message.toLowerCase().contains("created_at");
    }

    public List<Bid> getBySessionId(long sessionId) {

        List<Bid> list = new ArrayList<>();

        String sql = """
            SELECT *
            FROM bid
            WHERE session_id = ?
            ORDER BY id ASC
        """;

        try (Connection conn = DbConnection.getConnection();
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
