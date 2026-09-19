// ============================================
// dao/ProcedureDAO.java  (NEW — Change #2)
// All money-moving operations go through CallableStatement into the
// stored procedures defined in Phase 2. This is the DB-transaction
// layer: START TRANSACTION / SAVEPOINT / COMMIT / ROLLBACK all live
// inside the procedures themselves, not in Java.
// ============================================
package dao;

import database.DBConnection;
import model.CancellationResult;
import model.EventCancellationResult;
import model.RegistrationResult;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

public class ProcedureDAO {

    public RegistrationResult registerAndPay(int userId, int eventId, int instituteId, double amount,
                                             String paymentMode, boolean isTeam, boolean isCash) throws SQLException {
        String call = "{call sp_register_and_pay(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";
        try (Connection con = DBConnection.getConnection();
             CallableStatement cs = con.prepareCall(call)) {

            cs.setInt(1, userId);
            cs.setInt(2, eventId);
            cs.setInt(3, instituteId);
            cs.setDouble(4, amount);
            cs.setString(5, paymentMode);
            cs.setBoolean(6, isTeam);
            cs.setBoolean(7, isCash);
            cs.registerOutParameter(8, Types.INTEGER);   // registration_id
            cs.registerOutParameter(9, Types.INTEGER);   // transaction_id
            cs.registerOutParameter(10, Types.VARCHAR);  // status
            cs.registerOutParameter(11, Types.VARCHAR);  // message

            cs.execute();

            return new RegistrationResult(
                    cs.getInt(8), cs.getInt(9), cs.getString(10), cs.getString(11));
        }
    }

    public RegistrationResult registerFree(int userId, int eventId, int instituteId, boolean isTeam) throws SQLException {
        String call = "{call sp_register_free(?, ?, ?, ?, ?, ?, ?, ?)}";
        try (Connection con = DBConnection.getConnection();
             CallableStatement cs = con.prepareCall(call)) {

            cs.setInt(1, userId);
            cs.setInt(2, eventId);
            cs.setInt(3, instituteId);
            cs.setBoolean(4, isTeam);
            cs.registerOutParameter(5, Types.INTEGER);
            cs.registerOutParameter(6, Types.INTEGER);
            cs.registerOutParameter(7, Types.VARCHAR);
            cs.registerOutParameter(8, Types.VARCHAR);

            cs.execute();

            return new RegistrationResult(
                    cs.getInt(5), cs.getInt(6), cs.getString(7), cs.getString(8));
        }
    }

    public RegistrationResult registerFree(int userId, int eventId, int instituteId, String teamId) throws SQLException {
        String call = "{call sp_register_free(?, ?, ?, ?, ?, ?, ?, ?)}";
        try (Connection con = DBConnection.getConnection();
             CallableStatement cs = con.prepareCall(call)) {

            cs.setInt(1, userId);
            cs.setInt(2, eventId);
            cs.setInt(3, instituteId);
            cs.setString(4, teamId);
            cs.registerOutParameter(5, Types.INTEGER);
            cs.registerOutParameter(6, Types.INTEGER);
            cs.registerOutParameter(7, Types.VARCHAR);
            cs.registerOutParameter(8, Types.VARCHAR);

            cs.execute();

            return new RegistrationResult(
                    cs.getInt(5), cs.getInt(6), cs.getString(7), cs.getString(8));
        }
    }

    public boolean rechargeWallet(int userId, double amount) throws SQLException {
        String call = "{call sp_recharge_wallet(?, ?, ?, ?)}";
        try (Connection con = DBConnection.getConnection();
             CallableStatement cs = con.prepareCall(call)) {

            cs.setInt(1, userId);
            cs.setDouble(2, amount);
            cs.registerOutParameter(3, Types.VARCHAR);
            cs.registerOutParameter(4, Types.VARCHAR);

            cs.execute();
            return "SUCCESS".equals(cs.getString(3));
        }
    }

    public CancellationResult cancelRegistrationByUser(int registrationId) throws SQLException {
        String call = "{call sp_cancel_registration_by_user(?, ?, ?, ?)}";
        try (Connection con = DBConnection.getConnection();
             CallableStatement cs = con.prepareCall(call)) {

            cs.setInt(1, registrationId);
            cs.registerOutParameter(2, Types.VARCHAR);
            cs.registerOutParameter(3, Types.VARCHAR);
            cs.registerOutParameter(4, Types.DECIMAL);

            cs.execute();

            return new CancellationResult(cs.getString(2), cs.getString(3), cs.getDouble(4));
        }
    }

    public EventCancellationResult cancelEventByInstitute(int eventId) throws SQLException {
        String call = "{call sp_cancel_event_by_institute(?, ?, ?, ?)}";
        try (Connection con = DBConnection.getConnection();
             CallableStatement cs = con.prepareCall(call)) {

            cs.setInt(1, eventId);
            cs.registerOutParameter(2, Types.VARCHAR);
            cs.registerOutParameter(3, Types.VARCHAR);
            cs.registerOutParameter(4, Types.INTEGER);

            cs.execute();

            return new EventCancellationResult(cs.getString(2), cs.getString(3), cs.getInt(4));
        }
    }
}