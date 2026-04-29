package Server.service.Exceptions;

public class ReturnMessage {
    // Auction
    public static final String AUCTION_CLOSED = "Phiên đấu giá đang đóng";
    public static final String DATA_ACCESS = "Dữ liệu không hợp lệ";
    public static final String INVALID_BID = "Đặt giá không hợp lệ";
    public static final String NOT_ENOUGH_MONEY = "Vui lòng nạp thêm tiền";
    public static final String UN_AUTHORIZED = "Không có quyền truy cập";

    // Item
    public static final String ITEM_LOCKED = "Vật phẩm đấu giá đã bị khóa";
    public static final String ITEM_NOT_FOUND = "Không tìm thấy vật phẩm đấu giá";

    // User
    public static final String USERNAME_IS_BLANK = "Tên tài khoản không được để trống";
    public static final String PASSWORD_IS_BLANK = "Mật khẩu không được để trống";
    public static final String PASSWORD_LENGTH_INVALID = "Mật khẩu phải có từ 6 đến 10 ký tự";
    public static final String USERNAME_ALREADY_EXISTS = "Tên tài khoản đã tồn tại";
    public static final String WRONG_PASSWORD = "Sai mật khẩu";
    public static final String USER_NOT_FOUND = "Tài khoản không tồn tại";
}
