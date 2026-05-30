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

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000); // 30 seconds
        config.setConnectionTimeout(20000); // 20 seconds
        config.setMaxLifetime(1800000); // 30 minutes
        config.setDriverClassName("org.postgresql.Driver");

        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws Exception {
        return dataSource.getConnection();
    }
}
