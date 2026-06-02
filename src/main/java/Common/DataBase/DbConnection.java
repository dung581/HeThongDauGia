package Common.DataBase;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;

public class DbConnection {

    /*
    // Localhost configuration
    private static final String URL =
            "jdbc:postgresql://localhost:5432/auction";

    private static final String USER =
            "postgres";

    private static final String PASSWORD =
            "123456";
    */

    // Supabase configuration
    private static final String URL =
            "jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require";

    private static final String USER =
            "postgres.pqaqflhzgkgqjuoysxuy";

    private static final String PASSWORD =
            "baitaplon@@";

    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);

        // Supabase session pooler đang giới hạn số client, nên app chỉ giữ ít connection DB.
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(10000); // 10 seconds
        config.setConnectionTimeout(10000); // 10 seconds
        config.setMaxLifetime(300000); // 5 minutes
        // Nếu DB đang kín session, không để FXMLLoader chết ngay khi dựng controller.
        config.setInitializationFailTimeout(-1);
        config.setDriverClassName("org.postgresql.Driver");

        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws Exception {
        return dataSource.getConnection();
    }

    // Đóng pool khi tắt app để trả connection về Supabase, tránh lần chạy sau bị đầy session.
    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
