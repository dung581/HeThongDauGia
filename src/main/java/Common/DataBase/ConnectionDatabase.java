package Common.DataBase;
import java.sql.Connection;
import java.sql.DriverManager;

public class ConnectionDatabase{
    public Connection getConnection() throws Exception {
        String url = "jdbc:postgresql://localhost:8001/ducanh";
        String user = "ducanh";
        String password = "ducanh";
        return DriverManager.getConnection(url, user, password);
    }
}