package Common.DataBase.repository;

import Common.DataBase.DbConnection;
import Common.DataBase.entities.Auction;
import Common.Enum.AuctionState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


public class AuctionRepository {
    private static DbConnection db= new DbConnection();
    public List<Auction> getAll() {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT * FROM auction";
        DbConnection db = new DbConnection();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Auction a = new Auction();
                a.setId(rs.getLong("id"));
                a.setItem_id(rs.getLong("item_id"));
                a.setCurrent_user_id(rs.getLong("current_user_id"));
                a.setCurrent_price(rs.getLong("current_price"));

                Timestamp start = rs.getTimestamp("start_time");
                if (start != null) a.setStartTime(start.toLocalDateTime());

                Timestamp end = rs.getTimestamp("end_time");
                if (end != null) a.setEndTime(end.toLocalDateTime());

                a.setState(Enum.valueOf(
                        Common.Enum.AuctionState.class,
                        rs.getString("state")
                ));

                list.add(a);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return list;
    }
    public void save(Auction a) {
        String sql = """
        INSERT INTO auction 
        (item_id, current_user_id, current_price, start_time, end_time, state)
        VALUES (?, ?, ?, ?, ?, ?)
    """;
        DbConnection db= new DbConnection();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, a.getItem_id());
            ps.setLong(2, a.getCurrent_user_id());
            ps.setLong(3, a.getCurrent_price());

            ps.setTimestamp(4, a.getStartTime() != null
                    ? Timestamp.valueOf(a.getStartTime()) : null);

            ps.setTimestamp(5, a.getEndTime() != null
                    ? Timestamp.valueOf(a.getEndTime()) : null);

            ps.setString(6, a.getState().name());

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public void updateCurrentBid(long auctionId, long userId, long price) {
        String sql = """
        UPDATE auction
        SET current_user_id = ?, current_price = ?
        WHERE id = ?
    """;
        DbConnection db= new DbConnection();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setLong(2, price);
            ps.setLong(3, auctionId);

            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new RuntimeException("Auction not found");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public void update(Auction a) {
        String sql = """
        UPDATE auction
        SET item_id = ?, 
            current_user_id = ?, 
            current_price = ?, 
            start_time = ?, 
            end_time = ?, 
            state = ?
        WHERE id = ?
    """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, a.getItem_id());
            ps.setLong(2, a.getCurrent_user_id());
            ps.setLong(3, a.getCurrent_price());

            ps.setTimestamp(4, a.getStartTime() != null
                    ? Timestamp.valueOf(a.getStartTime()) : null);

            ps.setTimestamp(5, a.getEndTime() != null
                    ? Timestamp.valueOf(a.getEndTime()) : null);

            ps.setString(6, a.getState() != null
                    ? a.getState().name() : null);

            ps.setLong(7, a.getId());

            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new RuntimeException("Auction not found");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public List<Auction> getActive() {
        List<Auction> list = new ArrayList<>();

        String sql = "SELECT * FROM auction WHERE state = 'RUNNING'";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Auction a = new Auction();

                a.setId(rs.getLong("id"));
                a.setItem_id(rs.getLong("item_id"));
                a.setCurrent_user_id(rs.getLong("current_user_id"));
                a.setCurrent_price(rs.getLong("current_price"));

                Timestamp start = rs.getTimestamp("start_time");
                if (start != null) {
                    a.setStartTime(start.toLocalDateTime());
                }

                Timestamp end = rs.getTimestamp("end_time");
                if (end != null) {
                    a.setEndTime(end.toLocalDateTime());
                }

                a.setState(AuctionState.valueOf(rs.getString("state")));

                list.add(a);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return list;
    }
}