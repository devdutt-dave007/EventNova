// ============================================
// dao/UserDAO.java
// Change #4: plaintext password
// Change #5: Rs.500 welcome bonus, atomic (User insert + Wallet_Transaction insert)
// ============================================
package dao;

import database.DBConnection;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    private static final double WELCOME_BONUS = 500.00;

    public int registerUser(String name, String email, String password, String phone,
                            User.Role role, Integer instituteId, String enrollmentNo, String employeeId)
            throws SQLException {

        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            String insertUserSql = "INSERT INTO User (Name, Email, Password, Phone, Role, Institute_ID, " +
                    "Enrollment_No, Employee_ID, Balance) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            int userId;
            try (PreparedStatement ps = con.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, password);
                ps.setString(4, phone);
                ps.setString(5, role.name());
                if (instituteId != null) ps.setInt(6, instituteId); else ps.setNull(6, Types.INTEGER);
                ps.setString(7, enrollmentNo);
                ps.setString(8, employeeId);
                ps.setDouble(9, WELCOME_BONUS);

                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    userId = rs.getInt(1);
                }
            }

            String ledgerSql = "INSERT INTO Wallet_Transaction (Owner_Type, Owner_ID, Entry_Type, Amount, " +
                    "Balance_After, Description) VALUES ('USER', ?, 'BONUS', ?, ?, 'Welcome bonus on registration')";
            try (PreparedStatement ps = con.prepareStatement(ledgerSql)) {
                ps.setInt(1, userId);
                ps.setDouble(2, WELCOME_BONUS);
                ps.setDouble(3, WELCOME_BONUS);
                ps.executeUpdate();
            }

            con.commit();
            return userId;

        } catch (SQLException e) {
            if (con != null) con.rollback();
            throw e;
        } finally {
            if (con != null) {
                con.setAutoCommit(true);
                con.close();
            }
        }
    }

    public User login(String email, String password) throws SQLException {
        String sql = "SELECT * FROM User WHERE Email = ? AND Password = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public User getById(int userId) throws SQLException {
        String sql = "SELECT * FROM User WHERE User_ID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM User WHERE Email = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public double getBalance(int userId) throws SQLException {
        String sql = "SELECT Balance FROM User WHERE User_ID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("Balance");
            }
        }
        return 0.0;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        Integer instituteId = rs.getObject("Institute_ID") != null ? rs.getInt("Institute_ID") : null;
        return new User(
                rs.getInt("User_ID"),
                rs.getString("Name"),
                rs.getString("Email"),
                rs.getString("Phone"),
                User.Role.valueOf(rs.getString("Role")),
                instituteId,
                rs.getString("Enrollment_No"),
                rs.getString("Employee_ID"),
                rs.getDouble("Balance")
        );
    }
}