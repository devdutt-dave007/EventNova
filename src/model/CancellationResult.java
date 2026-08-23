// ============================================
// model/CancellationResult.java
// ============================================
package model;

public class CancellationResult {
    private final String status;
    private final String message;
    private final double refundAmount;

    public CancellationResult(String status, String message, double refundAmount) {
        this.status = status;
        this.message = message;
        this.refundAmount = refundAmount;
    }

    public boolean isSuccess() { return "SUCCESS".equals(status); }
    public String getMessage() { return message; }
    public double getRefundAmount() { return refundAmount; }
}