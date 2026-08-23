// ============================================
// model/Registration.java
// ============================================
package model;

import java.time.LocalDateTime;

public class Registration {

    public enum RegStatus { CONFIRMED, WAITLISTED, CANCELLED }

    private final int registrationId;
    private final int userId;
    private final int eventId;
    private final LocalDateTime registrationDate;
    private final RegStatus registrationStatus;
    private final String teamId;

    public Registration(int registrationId, int userId, int eventId,
                        LocalDateTime registrationDate, RegStatus registrationStatus, String teamId) {
        this.registrationId = registrationId;
        this.userId = userId;
        this.eventId = eventId;
        this.registrationDate = registrationDate;
        this.registrationStatus = registrationStatus;
        this.teamId = teamId;
    }

    public int getRegistrationId() { return registrationId; }
    public int getUserId() { return userId; }
    public int getEventId() { return eventId; }
    public RegStatus getRegistrationStatus() { return registrationStatus; }
    public String getTeamId() { return teamId; }

    @Override
    public String toString() {
        return "Registration_ID: " + registrationId + " | User_ID: " + userId +
                " | Event_ID: " + eventId + " | Status: " + registrationStatus +
                " | Date: " + registrationDate;
    }
}