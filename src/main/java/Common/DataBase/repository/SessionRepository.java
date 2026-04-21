package Common.DataBase.repository;

import Common.DataBase.ConnectionDatabase;
import Common.DataBase.entities.session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class SessionRepository {
    public List<session> getAllSession() {
        List<session> sessions = new ArrayList<>();
        String sql = "SELECT * FROM session";
        ConnectionDatabase db = new ConnectionDatabase();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                session s = new session();
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
}