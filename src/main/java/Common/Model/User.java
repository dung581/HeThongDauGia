package Common.Model;

/**
 * Lớp User trừu tượng - người dùng hệ thống.
 *
 * FIX so với bản cũ:
 *   1. Constructor cũ gán money = 100 tỉ cứng (bỏ qua tham số) - đã sửa.
 *   2. Bỏ field role kiểu String (đã có abstract getRole()).
 *   3. Thêm getter còn thiếu (password, UID, username) đã có; thêm setter hợp lý.
 *   4. Cài Serializable thông qua Entity (User giờ kế thừa Entity để thống nhất).
 *
 * LƯU Ý KIẾN TRÚC: User giờ kế thừa Entity, IID được dùng làm UID.
 * Điều này giúp User và Item dùng chung lớp cơ sở Entity - đúng yêu cầu đề bài.
 */
public abstract class User extends Entity {
    private static final long serialVersionUID = 1L;

    private String name;
    private String username;
    private String password;
    private long money;

    public User(String UID, String name, String username, String password, long money) {
        super(UID);
        this.name = name;
        this.username = username;
        this.password = password;
        // FIX: dùng tham số money thay vì gán 100 tỉ cứng.
        this.money = money;
    }

    // getUID() được kế thừa từ Entity.
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

    /** Cộng dồn số tiền (dùng amount âm để trừ). */
    public void updateMoney(long amount) {
        this.money += amount;
    }

    public boolean canBid(long price) {
        return money >= price;
    }

    /** Mỗi subclass phải trả về vai trò cụ thể (BIDDER, SELLER, ADMIN). */
    public abstract String getRole();
}