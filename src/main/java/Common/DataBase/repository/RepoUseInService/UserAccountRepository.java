package Common.DataBase.repository.RepoUseInService;

import Common.DataBase.DbConnection;
import Common.Model.user.UserAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserAccountRepository {
    public UserAccount getUserAccount(long userId) {
        String sql = """
        SELECT u.id, u.fullname, a.balance, a.locked_balance
        FROM user u
        JOIN account a ON u.id = a.user_id
        WHERE u.id = ?
    """;
        DbConnection db= new DbConnection();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                UserAccount ua = new UserAccount();
                ua.setUserId(rs.getLong("id"));
                ua.setFullname(rs.getString("fullname"));
                ua.setBalance(rs.getLong("balance"));
                ua.setLockedBalance(rs.getLong("locked_balance"));
                return ua;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return null;
    }
}
