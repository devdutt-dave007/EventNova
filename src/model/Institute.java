// ============================================
// model/Institute.java
// ============================================
package model;

import java.time.LocalDate;

public class Institute {
    private final int instituteId;
    private final String instituteName;
    private final String email;
    private final String address;
    private final double balance;
    private final LocalDate joiningDate;

    public Institute(int instituteId, String instituteName, String email,
                     String address, double balance, LocalDate joiningDate) {
        this.instituteId = instituteId;
        this.instituteName = instituteName;
        this.email = email;
        this.address = address;
        this.balance = balance;
        this.joiningDate = joiningDate;
    }

    public int getInstituteId() { return instituteId; }
    public String getInstituteName() { return instituteName; }
    public String getEmail() { return email; }
    public String getAddress() { return address; }
    public double getBalance() { return balance; }

    @Override
    public String toString() {
        return "Institute_ID: " + instituteId + " | Name: " + instituteName +
                " | Email: " + email + " | Balance: Rs." + balance;
    }
}