package Common.DataBase;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import static org.junit.jupiter.api.Assertions.*;

public class DbConnectionTest {

    @Test
    public void testGetConnection() {
        System.out.println("Testing database connection pool...");
        try (Connection conn = DbConnection.getConnection()) {
            assertNotNull(conn, "Connection acquired from HikariCP pool should not be null");
            assertFalse(conn.isClosed(), "Connection should be active and not closed");
            System.out.println("Database connection pool is working perfectly!");
        } catch (Exception e) {
            fail("Database connection failed! Check your internet connection or DB credentials: " + e.getMessage());
        }
    }
}
