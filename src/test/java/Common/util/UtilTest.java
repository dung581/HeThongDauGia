package Common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GIAI ĐOẠN 7 — TIỆN ÍCH (PasswordUtil)
 *
 * Test thuần, không cần fake (không đụng database).
 * Bản mới không sử dụng mã hóa mật khẩu.
 */
@DisplayName("Giai đoạn 7: Tiện ích (PasswordUtil)")
class UtilTest {

    @Test
    @DisplayName("Mật khẩu không mã hóa được giữ nguyên")
    void hash_keepsPlainText() {
        String hashed = PasswordUtil.hash("matkhau");
        assertEquals("matkhau", hashed);
        assertFalse(PasswordUtil.isBCryptHash(hashed));
    }

    @Test
    @DisplayName("verify trả về true với mật khẩu đúng")
    void verify_correct() {
        String hashed = PasswordUtil.hash("matkhau123");
        assertTrue(PasswordUtil.verify("matkhau123", hashed));
    }

    @Test
    @DisplayName("verify trả về false với mật khẩu sai")
    void verify_wrong() {
        String hashed = PasswordUtil.hash("matkhau123");
        assertFalse(PasswordUtil.verify("saimatkhau", hashed));
    }

    @Test
    @DisplayName("Hai lần băm cùng mật khẩu cho chuỗi bằng nhau và verify đúng")
    void hash_returnsSameString() {
        String h1 = PasswordUtil.hash("abc123");
        String h2 = PasswordUtil.hash("abc123");
        assertEquals(h1, h2);
        assertTrue(PasswordUtil.verify("abc123", h1));
        assertTrue(PasswordUtil.verify("abc123", h2));
    }

    @Test
    @DisplayName("verify trả về false khi một trong hai tham số null")
    void verify_nullArgs() {
        assertFalse(PasswordUtil.verify(null, "x"));
        assertFalse(PasswordUtil.verify("x", null));
    }

    @Test
    @DisplayName("isBCryptHash luôn trả về false")
    void isBCryptHash_alwaysFalse() {
        assertFalse(PasswordUtil.isBCryptHash("plaintext"));
        assertFalse(PasswordUtil.isBCryptHash(null));
    }
}