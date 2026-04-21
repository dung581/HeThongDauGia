package Common.DataBase.repository;

import Common.DataBase.ConnectionDatabase;
import Common.DataBase.entities.Account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AccountRepository {
    public List<Account> getAllAccount() {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT * FROM Account";
        ConnectionDatabase db = new ConnectionDatabase();
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
}