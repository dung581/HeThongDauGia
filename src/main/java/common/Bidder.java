package common;

public class Bidder extends User{
    public Bidder(String UID, String name, String username, String password, String role, long money) {
        super(UID, name, username, password, "BIDDER", money);
    }

    @Override
    String getRole() {
        return "BIDDER";
    }
}
