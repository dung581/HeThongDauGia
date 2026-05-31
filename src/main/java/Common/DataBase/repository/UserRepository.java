package Common.DataBase.repository;

import Common.DataBase.DbConnection;
import Common.DataBase.entities.User;
import Common.Enum.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    private static final String USER_TABLE = "users";

    private User mapResultSet(ResultSet rs) throws Exception {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));

        String role = rs.getString("role");
        if (role != null && !role.isBlank()) {
            u.setRole(UserRole.valueOf(role));
        }

        u.setFullname(rs.getString("fullname"));
        return u;
    }

    public List<User> getallUser() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM " + USER_TABLE;
        DbConnection db = new DbConnection();

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                users.add(mapResultSet(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not load users", e);
        }

        return users;
    }

    public User getUserById(long id) {
        String sql = "SELECT * FROM " + USER_TABLE + " WHERE id = ?";
        DbConnection db = new DbConnection();

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not find user by id", e);
        }

        return null;
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM " + USER_TABLE + " WHERE LOWER(username) = LOWER(?)";
        DbConnection db = new DbConnection();

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not find user by username", e);
        }

        return null;
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(1) FROM " + USER_TABLE + " WHERE LOWER(username) = LOWER(?)";
        DbConnection db = new DbConnection();

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not check username", e);
        }

        return false;
    }

    public void updatePassword(long userId, String password) {
        String sql = "UPDATE " + USER_TABLE + " SET password = ? WHERE id = ?";
        DbConnection db = new DbConnection();

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, password);
            ps.setLong(2, userId);

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("User not found");
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not update user password", e);
        }
    }

    public User createUser(User user) {
        String sql = "INSERT INTO " + USER_TABLE + " (username, password, role, fullname) VALUES (?, ?, ?::user_role, ?)";
        DbConnection db = new DbConnection();

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole() == null ? UserRole.BIDDER.name() : user.getRole().name());
            ps.setString(4, user.getFullname() == null || user.getFullname().isBlank()
                    ? user.getUsername()
                    : user.getFullname());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                } else {
                    User created = findByUsername(user.getUsername());
                    if (created != null) {
                        user.setId(created.getId());
                    }
                }
            }

            return user;
        } catch (Exception e) {
            throw new RuntimeException("Could not create user", e);
        }
    }
}
