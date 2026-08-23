// ============================================
// dao/InstituteDAO.java
// Change #4: plaintext password, no hashing
// ============================================
package dao;

import database.DBConnection;
import model.Institute;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InstituteDAO {

    public int registerInstitute(String name, String email, String password, String address) throws SQLException {
        String sql = "INSERT INTO Institute (Institute_Name, Email, Password, Address, Balance, Joining_Date) VALUES (?, ?, ?, ?, 0.00, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, password);
            ps.setString(4, address);
            ps.setDate(5, Date.valueOf(LocalDate.now()));

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public Institute login(String email, String password) throws SQLException {
        String sql = "SELECT * FROM Institute WHERE Email = ? AND Password = ?";
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

    public Institute getById(int instituteId) throws SQLException {
        String sql = "SELECT * FROM Institute WHERE Institute_ID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, instituteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<Institute> getAll() throws SQLException {
        List<Institute> institutes = new ArrayList<>();
        String sql = "SELECT * FROM Institute";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) institutes.add(mapRow(rs));
        }
        return institutes;
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM Institute WHERE Email = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public double getBalance(int instituteId) throws SQLException {
        String sql = "SELECT Balance FROM Institute WHERE Institute_ID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, instituteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("Balance");
            }
        }
        return 0.0;
    }

    private Institute mapRow(ResultSet rs) throws SQLException {
        return new Institute(
                rs.getInt("Institute_ID"),
                rs.getString("Institute_Name"),
                rs.getString("Email"),
                rs.getString("Address"),
                rs.getDouble("Balance"),
                rs.getDate("Joining_Date").toLocalDate()
        );
    }
}