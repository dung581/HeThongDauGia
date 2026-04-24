package Common.DataBase.repository;


import Common.DataBase.DbConnection;
import Common.DataBase.entities.User;
import Common.Enum.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;


public class UserRepository {
    public List<User> getallUser(){
        List<User> users= new ArrayList<>();
        String sql= "SELECT * FROM User";
        DbConnection db = new DbConnection();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                User s = new User();
                s.setId(rs.getLong("id"));
                s.setUsername(rs.getString("username"));
                s.setPassword(rs.getString("password"));
                s.setRole(UserRole.valueOf(rs.getString("role")));
                users.add(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }
    public void saveUser(User user) {
        // 1. Câu lệnh SQL để thêm mới
        String sql = "INSERT INTO User (username, password) VALUES (?, ?)";
        DbConnection db = new DbConnection();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // 2. Java đổ dữ liệu từ Object vào các dấu hỏi chấm (?)
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());

            // 3. Thực hiện đẩy vào Database
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public User getUserById(long id) {
        String sql = "SELECT * FROM user WHERE id = ?";
        DbConnection db = new DbConnection();

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User u = new User();
                u.setId(rs.getLong("id"));
                u.setUsername(rs.getString("username"));
                u.setPassword(rs.getString("password"));
                u.setRole(UserRole.valueOf(rs.getString("role")));
                return u;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

