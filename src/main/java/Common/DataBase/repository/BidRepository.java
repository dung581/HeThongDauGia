package Common.DataBase.repository;

import Common.DataBase.DbConnection;
import Common.DataBase.entities.Item;
import Common.DataBase.entities.bid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class BidRepository {
    public List<bid> getAllBid() {
        List<bid> bids = new ArrayList<>();
        String sql = "SELECT * FROM bid";
        DbConnection db = new DbConnection();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                bid b = new bid();
                b.setId(rs.getLong("id"));
                b.setAuction_id(rs.getLong("session_id"));
                b.setUser_id(rs.getLong("user_id"));
                b.setItem_id(rs.getLong("item_id"));
                b.setPrice(rs.getLong("price"));
                bids.add(b);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bids;
    }
    public void saveBid(bid b) {
        DbConnection db = new DbConnection();
        String sql = "INSERT INTO bid (session_id, user_id, item_id, price) VALUES (?, ?, ?, ?)";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, b.getAuction_id());
            ps.setLong(2, b.getUser_id());
            ps.setLong(3, b.getItem_id());
            ps.setLong(4, b.getPrice());
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
    public bid getBidById(long id) {
        String sql = "SELECT * FROM bid WHERE id = ?";
        DbConnection db = new DbConnection();

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                bid b = new bid();
                b.setId(rs.getLong("id"));
                b.setAuction_id(rs.getLong("auction_id"));
                b.setUser_id(rs.getLong("user_id"));
                b.setItem_id(rs.getLong("item_id"));
                b.setPrice(rs.getLong("price"));
                return b;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}