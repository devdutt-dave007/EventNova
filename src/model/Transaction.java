// ============================================
// model/Transaction.java
// Fixes Bug #10: object is always fully constructed (including date)
// at the moment we learn the DB-generated ID — never mutated later.
// ============================================
package model;

import java.time.LocalDateTime;

public class Transaction {

    public enum PaymentMode { CASH, UPI, CARD, NET_BANKING, FREE }
    public enum TransactionStatus { SUCCESS, FAILED, PENDING, REFUNDED }

    private final int transactionId;
    private final int registrationId;
    private final double amount;
    private final PaymentMode paymentMode;
    private final TransactionStatus transactionStatus;
    private final LocalDateTime transactionDate;

    public Transaction(int transactionId, int registrationId, double amount,
                       PaymentMode paymentMode, TransactionStatus transactionStatus,
                       LocalDateTime transactionDate) {
        this.transactionId = transactionId;
        this.registrationId = registrationId;
        this.amount = amount;
        this.paymentMode = paymentMode;
        this.transactionStatus = transactionStatus;
        this.transactionDate = transactionDate;
    }

    public int getTransactionId() { return transactionId; }
    public double getAmount() { return amount; }
    public PaymentMode getPaymentMode() { return paymentMode; }
    public TransactionStatus getTransactionStatus() { return transactionStatus; }
    public LocalDateTime getTransactionDate() { return transactionDate; }

    @Override
    public String toString() {
        return "Transaction_ID: " + transactionId + " | Amount: Rs." + amount +
                " | Mode: " + paymentMode + " | Status: " + transactionStatus +
                " | Date: " + transactionDate;
    }
}