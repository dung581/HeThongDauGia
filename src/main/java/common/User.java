package common;

public abstract class User {
    private String UID;
    private String name;
    private String username;
    private String password;
    private String role;
    private long money;

    public User(String UID, String name, String username, String password, String role, long money) {
        this.UID = UID;
        this.name = name;
        this.username = username;
        this.password = password;
        this.role = role;
        this.money = 100_000_000_000L;
    }

    public String getUID() {
        return UID;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public long getMoney() {
        return money;
    }

    public void updateMoney(long amount){
        this.money += amount;
    }

    public boolean canBid(long price){
        return money >= price;
    }

    abstract String getRole();
}
