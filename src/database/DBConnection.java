// ============================================
// database/DBConnection.java
// ============================================
package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/eventnova3";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) {
            throw new SQLException("MySQL/MariaDB JDBC Driver not found.", e);
        }
        // Fresh connection per call — safer for manual transaction/CallableStatement handling
        // than a single shared static Connection.
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}