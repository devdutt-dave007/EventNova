// ============================================
// dao/TransactionDAO.java
// Event-payment ledger reads only (used for invoices + payment history)
// ============================================
package dao;

import database.DBConnection;
import model.Transaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    public List<Transaction> getByUser(int userId) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM Transaction WHERE User_ID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) transactions.add(mapRow(rs));
            }
        }
        return transactions;
    }

    public List<Transaction> getByInstitute(int instituteId) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM Transaction WHERE Institute_ID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, instituteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) transactions.add(mapRow(rs));
            }
        }
        return transactions;
    }

    public Transaction getById(int transactionId) throws SQLException {
        String sql = "SELECT * FROM Transaction WHERE Transaction_ID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getInt("Transaction_ID"),
                rs.getInt("Registration_ID"),
                rs.getDouble("Amount"),
                Transaction.PaymentMode.valueOf(rs.getString("Payment_Mode")),
                Transaction.TransactionStatus.valueOf(rs.getString("Transaction_Status")),
                rs.getTimestamp("Transaction_Date").toLocalDateTime()
        );
    }
}