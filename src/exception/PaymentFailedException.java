// ============================================
// exception/PaymentFailedException.java
// General failure surfaced from stored procedures (p_status = 'FAILED')
// ============================================
package exception;

public class PaymentFailedException extends Exception {
    public PaymentFailedException(String message) { super(message); }
}