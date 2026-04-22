package Common.DataBase;
import Exceptions.DataAccessException;
import Exceptions.ReturnMessage;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Quản lý kết nối SQL Server theo Singleton pattern.
 *
 * Thiết kế:
 *   - Đọc cấu hình từ src/main/resources/db.properties (KHÔNG hardcode credentials).
 *   - getConnection() tạo connection MỚI mỗi lần gọi.
 *     Lý do: Connection KHÔNG thread-safe; nhiều thread share 1 Connection
 *     dẫn tới race condition khi có giao dịch song song (đúng kịch bản đấu giá!).
 *     Mỗi DAO method nên làm: try (Connection c = DbConnection.getInstance().getConnection()) {...}
 *   - Để đơn giản (BTL không yêu cầu), chưa dùng connection pool thật như HikariCP.
 *     Nếu cần pool, chỉ cần thay implementation getConnection() — interface không đổi.
 *
 * Pattern: lazy-initialization holder idiom — thread-safe + lazy không cần synchronized.
 */
public final class DbConnection {

    private final String url;
    private final String username;
    private final String password;

    public DbConnection() {
        Properties props = loadProperties();
        this.url      = props.getProperty("db.url");
        this.username = props.getProperty("db.username");
        this.password = props.getProperty("db.password");

        if (url == null || username == null) {
            throw new IllegalStateException(
                    "Thiếu cấu hình DB trong db.properties (cần db.url và db.username)");
        }
    }

    /** Holder idiom: class chỉ được load khi getInstance() lần đầu được gọi. */
    private static class Holder {
        static final DbConnection INSTANCE = new DbConnection();
    }

    public static DbConnection getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Mở connection MỚI tới DB. Caller phải close (dùng try-with-resources).
     */
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new DataAccessException(ReturnMessage.DATA_ACCESS, e);
        }
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = DbConnection.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                        "Không tìm thấy file db.properties trong classpath. "
                                + "Đảm bảo file ở src/main/resources/db.properties");
            }
            props.load(in);
            return props;
        } catch (IOException e) {
            throw new IllegalStateException("Lỗi đọc db.properties", e);
        }
    }
}