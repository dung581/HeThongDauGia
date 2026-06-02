package Common.util;

public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hash(String plainPassword) {
        return plainPassword;
    }

    public static boolean verify(String plainPassword, String storedPassword) {
        if (plainPassword == null || storedPassword == null) {
            return false;
        }
        return plainPassword.equals(storedPassword);
    }

    public static boolean isBCryptHash(String value) {
        return false;
    }
}
