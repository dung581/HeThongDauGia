package Common.DataBase.repository;

import Common.DataBase.DbConnection;
import Common.DataBase.entities.Account;
import Common.DataBase.entities.Auction;
import Common.Enum.AuctionState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AccountRepository {
    DbConnection db= new DbConnection();
    public List<Account> getAllAccount() {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT * FROM Account";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Account a = new Account();
                a.setId(rs.getLong("id"));
                a.setUser_id(rs.getLong("user_id"));
                a.setBalance(rs.getLong("balance"));
                a.setLocked_balance(rs.getLong("locked_balance"));
                accounts.add(a);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return accounts;
    }
    public void CreateAccount(Account account) {
        // Khởi tạo ngay trong hàm
        String sql = "INSERT INTO Account (user_id, balance, locked_balance) VALUES (?, ?, ?)";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, account.getUser_id());
            ps.setLong(2, account.getBalance());
            ps.setLong(3, account.getLocked_balance());
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
    public Account getAccountByUserId(long user_id) {
        String sql = "SELECT * FROM account WHERE user_id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, user_id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Account a = new Account();
                a.setId(rs.getLong("id"));
                a.setUser_id(rs.getLong("user_id"));
                a.setBalance(rs.getLong("balance"));
                a.setLocked_balance(rs.getLong("locked_balance"));
                return a;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public void update(Account a) {
        String sql = """
        UPDATE account
        SET balance = ?, locked_balance = ?
        WHERE user_id = ?
    """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, a.getBalance());
            ps.setLong(2, a.getLocked_balance());
            ps.setLong(3, a.getUser_id());

            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new RuntimeException("Account not found");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}