package dao;

import database.DBConnection;
import model.Registration;
import model.TeamMember;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RegistrationDAO {

    public Registration getById(int registrationId) throws SQLException {
        String sql = "SELECT * FROM Registration WHERE Registration_ID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, registrationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public void insertTeamMembers(int registrationId, List<TeamMember> members) throws SQLException {
        if (members == null || members.isEmpty()) return;
        String sql = "INSERT INTO Team_Member (Registration_ID, Name, Branch, Enrollment_No) VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (TeamMember m : members) {
                ps.setInt(1, registrationId);
                ps.setString(2, m.getName());
                ps.setString(3, m.getBranch());
                ps.setString(4, m.getEnrollmentNo());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<Registration> getByEvent(int eventId) throws SQLException {
        List<Registration> list = new ArrayList<>();
        String sql = "SELECT * FROM Registration WHERE Event_ID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Registration> getByUser(int userId) throws SQLException {
        List<Registration> list = new ArrayList<>();
        String sql = "SELECT * FROM Registration WHERE User_ID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private Registration mapRow(ResultSet rs) throws SQLException {
        return new Registration(
                rs.getInt("Registration_ID"),
                rs.getInt("User_ID"),
                rs.getInt("Event_ID"),
                rs.getTimestamp("Registration_Date").toLocalDateTime(),
                Registration.RegStatus.valueOf(rs.getString("Registration_Status")),
                rs.getString("Team_ID")
        );
    }
}