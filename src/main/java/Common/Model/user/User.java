package Common.Model.user;

import Common.Enum.UserRole;
import Common.Model.entity.Entity;

public class User extends Entity {
    private static final long serialVersionUID = 1L;

    private String name;
    private String username;
    private String password;
    private long money;
    private UserRole role;

    public User(String UID, String name, String username, String password, long money, UserRole role) {
        super(UID);
        this.name = name;
        this.username = username;
        this.password = password;
        this.money = money;
        this.role = role;
    }

    public String getUID() {
        return getIID();
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public long getMoney() {
        return money;
    }

    public UserRole getRole() {
        return role;
    }

    public void updateMoney(long amount) {
        this.money += amount;
    }

    public boolean canBid(long price) {
        return money >= price;
    }

    // Helper check quyền
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isSeller() {
        return role == UserRole.SELLER;
    }

    public boolean isBidder() {
        return role == UserRole.BIDDER;
    }
}