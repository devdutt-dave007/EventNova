// ============================================
// model/RegistrationResult.java
// DTO carrying stored-procedure OUT params back to the service layer
// ============================================
package model;

public class RegistrationResult {
    private final int registrationId;
    private final int transactionId;
    private final String status;
    private final String message;

    public RegistrationResult(int registrationId, int transactionId, String status, String message) {
        this.registrationId = registrationId;
        this.transactionId = transactionId;
        this.status = status;
        this.message = message;
    }

    public int getRegistrationId() { return registrationId; }
    public int getTransactionId() { return transactionId; }
    public boolean isSuccess() { return "SUCCESS".equals(status); }
    public String getMessage() { return message; }
}