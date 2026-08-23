// ============================================
// dao/WalletDAO.java
// ============================================
package dao;

import database.DBConnection;
import model.WalletTransaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WalletDAO {

    public List<WalletTransaction> getByUser(int userId) throws SQLException {
        return getByOwner("USER", userId);
    }

    public List<WalletTransaction> getByInstitute(int instituteId) throws SQLException {
        return getByOwner("INSTITUTE", instituteId);
    }

    private List<WalletTransaction> getByOwner(String ownerType, int ownerId) throws SQLException {
        List<WalletTransaction> entries = new ArrayList<>();
        String sql = "SELECT * FROM Wallet_Transaction WHERE Owner_Type = ? AND Owner_ID = ? ORDER BY Entry_Date";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ownerType);
            ps.setInt(2, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) entries.add(mapRow(rs));
            }
        }
        return entries;
    }

    private WalletTransaction mapRow(ResultSet rs) throws SQLException {
        return new WalletTransaction(
                rs.getInt("Wallet_Txn_ID"),
                WalletTransaction.EntryType.valueOf(rs.getString("Entry_Type")),
                rs.getDouble("Amount"),
                rs.getDouble("Balance_After"),
                rs.getString("Description"),
                rs.getTimestamp("Entry_Date").toLocalDateTime()
        );
    }
}