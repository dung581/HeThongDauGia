package Common.DataBase.repository;


import Common.DataBase.DbConnection;
import Common.DataBase.entities.Stake;
import Common.Enum.StakeStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class StakeRepository {
    private static DbConnection db = new DbConnection();

    public static List<Stake> getAllStake() {
        List<Stake> stakes = new ArrayList<>();
        String sql = "SELECT * FROM Stake";
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
                s.setStatus(StakeStatus.valueOf(rs.getString("status")));
                stakes.add(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stakes;
    }

    public Stake getById(long id) {
        String sql = "SELECT * FROM stake WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Stake s = new Stake();

                s.setId(rs.getLong("id"));
                s.setAution_id(rs.getLong("auction_id"));
                s.setUser_id(rs.getLong("user_id"));
                s.setAmount(rs.getLong("amount"));

                String statusStr = rs.getString("status");
                if (statusStr != null) {
                    s.setStatus(StakeStatus.valueOf(statusStr));
                }

                return s;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return null; // không tìm thấy
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateUserAmountStatus(long auctionID, long userId, long amount, StakeStatus status) {

    }

    public void updateStatus(long autionId, StakeStatus status) {
        String sql = "UPDATE stake SET status = ? WHERE auction_id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setLong(2, autionId);

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public List<Stake> getByUserId(long userId) {
        List<Stake> list = new ArrayList<>();

        String sql = "SELECT * FROM stake WHERE user_id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Stake s = new Stake();

                s.setId(rs.getLong("id"));
                s.setAution_id(rs.getLong("auction_id"));
                s.setUser_id(rs.getLong("user_id"));
                s.setAmount(rs.getLong("amount"));

                String statusStr = rs.getString("status");
                if (statusStr != null) {
                    s.setStatus(StakeStatus.valueOf(statusStr));
                }

                list.add(s);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return list;
    }
    public Stake getByAuctionIdAndUserIdAndStatus(long auctionId, long userId, StakeStatus status) {

        String sql = """
        SELECT * FROM stake 
        WHERE auction_id = ? 
          AND user_id = ? 
          AND status = ?
        LIMIT 1
    """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, auctionId);
            ps.setLong(2, userId);
            ps.setString(3, status.name());

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Stake s = new Stake();

                s.setId(rs.getLong("id"));
                s.setAution_id(rs.getLong("auction_id"));
                s.setUser_id(rs.getLong("user_id"));
                s.setAmount(rs.getLong("amount"));

                String statusStr = rs.getString("status");
                if (statusStr != null) {
                    s.setStatus(StakeStatus.valueOf(statusStr));
                }

                return s;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return null;
    }
}