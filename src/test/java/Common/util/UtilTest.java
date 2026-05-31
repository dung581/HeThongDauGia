package Common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GIAI ĐOẠN 7 — TIỆN ÍCH (PasswordUtil)
 *
 * Test thuần, không cần fake (không đụng database).
 * Bản mới chỉ còn PasswordUtil (hash / verify / isBCryptHash).
 */
@DisplayName("Giai đoạn 7: Tiện ích (PasswordUtil)")
class UtilTest {

    @Test
    @DisplayName("Mật khẩu băm khác mật khẩu gốc và có tiền tố BCrypt")
    void hash_producesBcrypt() {
        String hashed = PasswordUtil.hash("matkhau");
        assertNotEquals("matkhau", hashed);
        assertTrue(PasswordUtil.isBCryptHash(hashed));
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
    @DisplayName("Hai lần băm cùng mật khẩu cho chuỗi khác nhau (salt ngẫu nhiên) nhưng đều verify đúng")
    void hash_usesRandomSalt() {
        String h1 = PasswordUtil.hash("abc123");
        String h2 = PasswordUtil.hash("abc123");
        assertNotEquals(h1, h2);
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
    @DisplayName("verify so sánh plaintext khi hash chưa phải BCrypt (tương thích dữ liệu cũ)")
    void verify_plaintextFallback() {
        assertTrue(PasswordUtil.verify("plain123", "plain123"));
        assertFalse(PasswordUtil.verify("plain123", "khac"));
    }

    @Test
    @DisplayName("isBCryptHash nhận diện đúng định dạng băm")
    void isBCryptHash_detects() {
        assertTrue(PasswordUtil.isBCryptHash(PasswordUtil.hash("x")));
        assertFalse(PasswordUtil.isBCryptHash("plaintext"));
        assertFalse(PasswordUtil.isBCryptHash(null));
    }
}