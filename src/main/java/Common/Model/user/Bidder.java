package Common.Model.user;

import Common.Model.User;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;

    public Bidder(String UID, String name, String username, String password, long money) {
        super(UID, name, username, password, money);
    }

    @Override
    public String getRole() {
        return "BIDDER";
    }
}