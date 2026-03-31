package common;

public class Admin extends User{
    public Admin(String UID, String name, String username, String password, String role, long money) {
        super(UID, name, username, password, "ADMIN", money);
    }

    @Override
    String getRole() {
        return "ADMIN";
    }
}
