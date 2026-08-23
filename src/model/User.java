// ============================================
// model/User.java
// ============================================
package model;

public class User {

    public enum Role { STUDENT, FACULTY, EXTERNAL }

    private final int userId;
    private final String name;
    private final String email;
    private final String phone;
    private final Role role;
    private final Integer instituteId;
    private final String enrollmentNo;
    private final String employeeId;
    private final double balance;

    public User(int userId, String name, String email, String phone, Role role,
                Integer instituteId, String enrollmentNo, String employeeId, double balance) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.instituteId = instituteId;
        this.enrollmentNo = enrollmentNo;
        this.employeeId = employeeId;
        this.balance = balance;
    }

    public int getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Role getRole() { return role; }
    public Integer getInstituteId() { return instituteId; }
    public double getBalance() { return balance; }
    @Override
    public String toString() {
        return "User_ID: " + userId + " | Name: " + name + " | Email: " + email +
                " | Role: " + role + " | Balance: Rs." + balance;
    }
}