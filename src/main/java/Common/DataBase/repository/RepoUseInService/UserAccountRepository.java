package Common.DataBase.repository.RepoUseInService;

import Common.DataBase.DbConnection;
import Common.Model.user.UserAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserAccountRepository {

    public UserAccount getUserAccount(long userId) {

        String sql = """
            SELECT u.id,
                   u.fullname,
                   a.balance,
                   a.locked_balance
            FROM users u
            JOIN account a ON u.id = a.user_id
            WHERE u.id = ?
        """;

        DbConnection db = new DbConnection();

        try (
                Connection conn = DbConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    UserAccount ua = new UserAccount();

                    ua.setUserId(rs.getLong("id"));
                    ua.setFullname(rs.getString("fullname"));
                    ua.setBalance(rs.getLong("balance"));
                    ua.setLockedBalance(rs.getLong("locked_balance"));

                    return ua;
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error while getting user account", e);
        }

        return null;
    }
}