package Common.Model.user;

import Common.Model.Item;
import Common.Model.User;

public class Seller extends User {
    private static final long serialVersionUID = 1L;

    public Seller(String UID, String name, String username, String password, long money) {
        super(UID, name, username, password, money);
    }

    @Override
    public String getRole() {
        return "SELLER";
    }

    /**
     * Helper kiểm tra quyền: Seller chỉ được sửa/xóa item do chính mình đăng.
     * Dùng trong ItemService trước khi thực hiện update/delete.
     */
    public boolean owns(Item item) {
        return item != null && getUID().equals(item.getSellerId());
    }
}