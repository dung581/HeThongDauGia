package Common.Model.user;

import Common.Model.User;

public class Bidder extends User {
    public Bidder(String UID, String name, String username, String password, String role, long money) {
        super(UID, name, username, password, "BIDDER", money);
    }

    @Override
    public String getRole() {
        return "BIDDER";
    }
}
