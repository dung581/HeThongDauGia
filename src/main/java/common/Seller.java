package common;

public class Seller extends User{
    public Seller(String UID, String name, String username, String password, String role, long money) {
        super(UID, name, username, password, "SELLER", money);
    }

    @Override
    String getRole() {
        return "SELLER";
    }
}
