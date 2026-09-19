// ============================================
// service/InstituteService.java
// ============================================
package service;

import dao.InstituteDAO;
import exception.InvalidLoginException;
import model.Institute;

import java.sql.SQLException;

public class InstituteService {

    private final InstituteDAO instituteDAO = new InstituteDAO();

    public boolean emailExists(String email) throws SQLException {
        return instituteDAO.emailExists(email);
    }

    public int registerInstitute(String name, String email, String password, String address) throws SQLException {
        return instituteDAO.registerInstitute(name, email, password, address);
    }

    public Institute login(String email, String password) throws SQLException, InvalidLoginException {
        Institute institute = instituteDAO.login(email, password);
        if (institute == null) throw new InvalidLoginException("Invalid email or password.");
        return institute;
    }

    public double getBalance(int instituteId) throws SQLException {
        return instituteDAO.getBalance(instituteId);
    }
}