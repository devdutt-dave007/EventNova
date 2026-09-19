// ============================================
// service/WalletService.java
// Change #7 / #9: thin service wrapper around the Wallet_Transaction
// ledger — every balance-affecting event (bonus, recharge, event
// payment debit/credit, refund credit/debit) for both Users and
// Institutes reads through here.
// ============================================
package service;

import dao.WalletDAO;
import model.WalletTransaction;

import java.sql.SQLException;
import java.util.List;

public class WalletService {

    private final WalletDAO walletDAO = new WalletDAO();

    /**
     * Full wallet ledger for a single user, oldest to newest.
     * UserMenu appends the live current balance after printing this list.
     */
    public List<WalletTransaction> getUserHistory(int userId) throws SQLException {
        return walletDAO.getByUser(userId);
    }

    /**
     * Full wallet ledger for a single institute, oldest to newest.
     * InstituteMenu appends the live current balance after printing this list.
     */
    public List<WalletTransaction> getInstituteHistory(int instituteId) throws SQLException {
        return walletDAO.getByInstitute(instituteId);
    }
}