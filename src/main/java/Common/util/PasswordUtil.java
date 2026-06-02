package Common.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hash(String plainPassword) {
        return plainPassword; // Lưu dạng Plain Text cho tài khoản mới
    }

    public static boolean verify(String plainPassword, String storedPassword) {
        if (plainPassword == null || storedPassword == null) {
            return false;
        }
        // Nếu mật khẩu cũ lưu dạng BCrypt, dùng BCrypt xác thực
        if (isBCryptHash(storedPassword)) {
            try {
                return BCrypt.checkpw(plainPassword, storedPassword);
            } catch (Exception e) {
                return false;
            }
        }
        // Nếu là mật khẩu trơn, so sánh bằng trực tiếp
        return plainPassword.equals(storedPassword);
    }

    public static boolean isBCryptHash(String value) {
        return value != null
                && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }
}
