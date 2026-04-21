package Common.DataBase.repository;


import Common.DataBase.ConnectionDatabase;
import Common.DataBase.entities.User;
import Common.DataBase.entities.UserStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;


public class UserRepository {
    public List<User> getallUser(){
        List<User> users= new ArrayList<>();
        String sql= "SELECT * FROM User";
        ConnectionDatabase db = new ConnectionDatabase();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                User s = new User();
                s.setId(rs.getLong("id"));
                s.setUsername(rs.getString("username"));
                s.setPassword(rs.getLong("password"));
                s.setRoll(UserStatus.valueOf(rs.getString("roll")));
                users.add(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }
}

