package Common.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    /**
     * Băm mật khẩu (Dùng khi Đăng ký)
     */
    public static String hash(String plainPassword) {
        // Tạo chuỗi salt ngẫu nhiên và băm mật khẩu
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    /**
     * Kiểm tra mật khẩu (Dùng khi Đăng nhập)
     */
    public static boolean verify(String plainPassword, String hashedPassword) {
        if (hashedPassword == null || !hashedPassword.startsWith("$2a$")) {
            return false; // Tránh lỗi nếu DB đang chứa mật khẩu chưa mã hóa
        }
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
