// ============================================
// model/EventCancellationResult.java
// ============================================
package model;

public class EventCancellationResult {
    private final String status;
    private final String message;
    private final int refundedCount;

    public EventCancellationResult(String status, String message, int refundedCount) {
        this.status = status;
        this.message = message;
        this.refundedCount = refundedCount;
    }

    public boolean isSuccess() { return "SUCCESS".equals(status); }
    public String getMessage() { return message; }
    public int getRefundedCount() { return refundedCount; }
}