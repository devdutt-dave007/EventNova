// ============================================
// service/TransactionService.java — FINAL (unchanged, kept for completeness)
// ============================================
package service;

import dao.TransactionDAO;
import model.Transaction;

import java.sql.SQLException;
import java.util.List;

public class TransactionService {

    private final TransactionDAO transactionDAO = new TransactionDAO();

    public List<Transaction> getByUser(int userId) throws SQLException {
        return transactionDAO.getByUser(userId);
    }

    public List<Transaction> getByInstitute(int instituteId) throws SQLException {
        return transactionDAO.getByInstitute(instituteId);
    }

    public Transaction getById(int transactionId) throws SQLException {
        return transactionDAO.getById(transactionId);
    }
}