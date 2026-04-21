package Common.Model.user;

import Common.Model.User;

public class Seller extends User {
    public Seller(String UID, String name, String username, String password, String role, long money) {
        super(UID, name, username, password, "SELLER", money);
    }

    @Override
    public String getRole() {
        return "SELLER";
    }
}
