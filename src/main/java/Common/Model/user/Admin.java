package Common.Model.user;

import Common.Model.User;

/**
 * Admin có full quyền với mọi Item (theo quyết định thiết kế của nhóm).
 * ItemService sẽ kiểm tra instanceof Admin để bỏ qua check ownership.
 */
public class Admin extends User {
    private static final long serialVersionUID = 1L;

    public Admin(String UID, String name, String username, String password, long money) {
        super(UID, name, username, password, money);
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }
}