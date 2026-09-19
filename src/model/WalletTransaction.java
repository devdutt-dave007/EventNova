// ============================================
// model/WalletTransaction.java
// ============================================
package model;

import java.time.LocalDateTime;

public class WalletTransaction {

    public enum OwnerType { USER, INSTITUTE }
    public enum EntryType { BONUS, RECHARGE, EVENT_PAYMENT_DEBIT, EVENT_PAYMENT_CREDIT, REFUND_CREDIT, REFUND_DEBIT }

    private final int walletTxnId;
    private final EntryType entryType;
    private final double amount;
    private final double balanceAfter;
    private final String description;
    private final LocalDateTime entryDate;

    public WalletTransaction(int walletTxnId, EntryType entryType, double amount,
                             double balanceAfter, String description, LocalDateTime entryDate) {
        this.walletTxnId = walletTxnId;
        this.entryType = entryType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.entryDate = entryDate;
    }

    public double getAmount() { return amount; }
    public double getBalanceAfter() { return balanceAfter; }

    @Override
    public String toString() {
        return "[" + entryDate + "] " + entryType + " | Amount: Rs." + amount +
                " | " + description + " | Balance After: Rs." + balanceAfter;
    }
public EntryType getEntryType() { return entryType;}
}