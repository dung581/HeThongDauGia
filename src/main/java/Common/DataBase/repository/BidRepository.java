package Common.DataBase.repository;

import Common.DataBase.DbConnection;
import Common.DataBase.entities.Bid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;


public class BidRepository {
    private static DbConnection db= new DbConnection();
    public List<Bid> getAllBid() {
        List<Bid> bids = new ArrayList<>();
        String sql = "SELECT * FROM bid";
        DbConnection db = new DbConnection();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Bid b = new Bid();
                b.setId(rs.getLong("id"));
                b.setAuction_id(rs.getLong("session_id"));
                b.setUser_id(rs.getLong("user_id"));
                b.setItem_id(rs.getLong("item_id"));
                b.setPrice(rs.getLong("price"));
                b.setCreated_at(
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
                bids.add(b);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bids;
    }
    public void saveBid(Bid b) {
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

    public List<Bid> getByItemId(long itemId) {
        List<Bid> list = new ArrayList<>();

        String sql = "SELECT * FROM bid WHERE item_id = ? ORDER BY created_at ASC";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, itemId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Bid b = new Bid();

                b.setId(rs.getLong("id"));
                b.setAuction_id(rs.getLong("auction_id"));
                b.setUser_id(rs.getLong("user_id"));
                b.setItem_id(rs.getLong("item_id"));
                b.setPrice(rs.getLong("price"));

                list.add(b);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return list;
    }
}