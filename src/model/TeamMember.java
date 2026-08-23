package model;

public class TeamMember {
    private final int teamMemberId;
    private final int registrationId;
    private final String name;
    private final String branch;
    private final String enrollmentNo;

    public TeamMember(int teamMemberId, int registrationId, String name, String branch, String enrollmentNo) {
        this.teamMemberId = teamMemberId;
        this.registrationId = registrationId;
        this.name = name;
        this.branch = branch;
        this.enrollmentNo = enrollmentNo;
    }

    public int getTeamMemberId() { return teamMemberId; }
    public int getRegistrationId() { return registrationId; }
    public String getName() { return name; }
    public String getBranch() { return branch; }
    public String getEnrollmentNo() { return enrollmentNo; }

    @Override
    public String toString() {
        return "Name: " + name + " | Branch: " + branch + " | Enrollment No: " + enrollmentNo;
    }
}