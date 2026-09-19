// ============================================
// dao/EventDAO.java  (corrected)
// ============================================
package dao;

import database.DBConnection;
import model.Event;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EventDAO {

    public int createEvent(Event event) throws SQLException {
        String sql = "INSERT INTO Event (Institute_ID, Event_Name, Category, Eligibility, Participation_Type, " +
                "Description, Venue, Capacity, Ticket_Price, Event_Date, Start_Time, End_Time, " +
                "Registration_Deadline, Status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, event.getInstituteId());
            ps.setString(2, event.getEventName());
            ps.setString(3, event.getCategory().name());
            ps.setString(4, event.getEligibility().name());
            ps.setString(5, event.getParticipationType().name());
            ps.setString(6, event.getDescription());
            ps.setString(7, event.getVenue());
            ps.setInt(8, event.getCapacity());
            ps.setDouble(9, event.getTicketPrice());
            ps.setDate(10, Date.valueOf(event.getEventDate()));
            ps.setTime(11, Time.valueOf(event.getStartTime()));
            ps.setTime(12, Time.valueOf(event.getEndTime()));
            ps.setTimestamp(13, Timestamp.valueOf(event.getRegistrationDeadline()));
            ps.setString(14, event.getStatus().name());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public boolean updateEvent(Event event) throws SQLException {
        String sql = "UPDATE Event SET Event_Name=?, Category=?, Eligibility=?, Participation_Type=?, " +
                "Description=?, Venue=?, Capacity=?, Ticket_Price=?, Event_Date=?, Start_Time=?, " +
                "End_Time=?, Registration_Deadline=?, Status=? WHERE Event_ID=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, event.getEventName());
            ps.setString(2, event.getCategory().name());
            ps.setString(3, event.getEligibility().name());
            ps.setString(4, event.getParticipationType().name());
            ps.setString(5, event.getDescription());
            ps.setString(6, event.getVenue());
            ps.setInt(7, event.getCapacity());
            ps.setDouble(8, event.getTicketPrice());
            ps.setDate(9, Date.valueOf(event.getEventDate()));
            ps.setTime(10, Time.valueOf(event.getStartTime()));
            ps.setTime(11, Time.valueOf(event.getEndTime()));
            ps.setTimestamp(12, Timestamp.valueOf(event.getRegistrationDeadline()));
            ps.setString(13, event.getStatus().name());
            ps.setInt(14, event.getEventId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteEvent(int eventId) throws SQLException {
        String sql = "DELETE FROM Event WHERE Event_ID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            return ps.executeUpdate() > 0;
        }
    }

    // Used by InstituteMenu's "Close Registration" option — genuine spec feature
    // (listed in the original Event Module requirements), now wired up.
    public boolean updateStatus(int eventId, Event.Status status) throws SQLException {
        String sql = "UPDATE Event SET Status = ? WHERE Event_ID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, eventId);
            return ps.executeUpdate() > 0;
        }
    }

    public Event getById(int eventId) throws SQLException {
        String sql = "SELECT * FROM Event WHERE Event_ID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<Event> getAll() throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM Event";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) events.add(mapRow(rs));
        }
        return events;
    }

    public List<Event> searchByName(String keyword) throws SQLException {
        return searchGeneric("Event_Name LIKE ?", "%" + keyword + "%");
    }

    public List<Event> searchByCategory(Event.Category category) throws SQLException {
        return searchGeneric("Category = ?", category.name());
    }

    public List<Event> searchByDate(LocalDate date) throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM Event WHERE Event_Date = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) events.add(mapRow(rs));
            }
        }
        return events;
    }

    public List<Event> searchByInstitute(int instituteId) throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM Event WHERE Institute_ID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, instituteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) events.add(mapRow(rs));
            }
        }
        return events;
    }

    public List<Event> searchByFeeStatus(boolean free) throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = free ? "SELECT * FROM Event WHERE Ticket_Price = 0" : "SELECT * FROM Event WHERE Ticket_Price > 0";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) events.add(mapRow(rs));
        }
        return events;
    }

    private List<Event> searchGeneric(String whereClause, String param) throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM Event WHERE " + whereClause;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) events.add(mapRow(rs));
            }
        }
        return events;
    }

    public int getConfirmedRegistrationCount(int eventId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Registration WHERE Event_ID = ? AND Registration_Status = 'CONFIRMED'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private Event mapRow(ResultSet rs) throws SQLException {
        return new Event(
                rs.getInt("Event_ID"),
                rs.getInt("Institute_ID"),
                rs.getString("Event_Name"),
                Event.Category.valueOf(rs.getString("Category")),
                Event.Eligibility.valueOf(rs.getString("Eligibility")),
                Event.ParticipationType.valueOf(rs.getString("Participation_Type")),
                rs.getString("Description"),
                rs.getString("Venue"),
                rs.getInt("Capacity"),
                rs.getDouble("Ticket_Price"),
                rs.getDate("Event_Date").toLocalDate(),
                rs.getTime("Start_Time").toLocalTime(),
                rs.getTime("End_Time").toLocalTime(),
                rs.getTimestamp("Registration_Deadline").toLocalDateTime(),
                Event.Status.valueOf(rs.getString("Status"))
        );
    }
    public int getActiveRegistrationCount(int eventId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Registration WHERE Event_ID = ? AND Registration_Status != 'CANCELLED'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }
}