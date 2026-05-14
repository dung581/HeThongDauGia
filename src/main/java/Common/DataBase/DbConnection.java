
package Common.DataBase;

import java.sql.Connection;
import java.sql.DriverManager;

public class DbConnection {

    private static final String URL =
            "jdbc:postgresql://localhost:5432/auction";

    private static final String USER =
            "postgres";

    private static final String PASSWORD =
            "123456";

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

}
/*
package Common.DataBase;

import java.sql.Connection;
import java.sql.DriverManager;

public class DbConnection {

    private static final String URL =
            "jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require";

    private static final String USER =
            "postgres.pqaqflhzgkgqjuoysxuy";

    private static final String PASSWORD =
            "baitaplon@@";

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

}

 */
