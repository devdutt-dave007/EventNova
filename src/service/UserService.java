// ============================================
// service/UserService.java
// ============================================
package service;

import dao.ProcedureDAO;
import dao.UserDAO;
import exception.InvalidLoginException;
import model.User;

import java.sql.SQLException;

public class UserService {

    private final UserDAO userDAO = new UserDAO();
    private final ProcedureDAO procedureDAO = new ProcedureDAO();

    public boolean emailExists(String email) throws SQLException {
        return userDAO.emailExists(email);
    }

    public int registerUser(String name, String email, String password, String phone,
                            User.Role role, Integer instituteId, String enrollmentNo, String employeeId)
            throws SQLException {
        return userDAO.registerUser(name, email, password, phone, role, instituteId, enrollmentNo, employeeId);
    }

    public User login(String email, String password) throws SQLException, InvalidLoginException {
        User user = userDAO.login(email, password);
        if (user == null) throw new InvalidLoginException("Invalid email or password.");
        return user;
    }

    public double getBalance(int userId) throws SQLException {
        return userDAO.getBalance(userId);
    }

    public boolean rechargeWallet(int userId, double amount) throws SQLException {
        return procedureDAO.rechargeWallet(userId, amount);
    }
}