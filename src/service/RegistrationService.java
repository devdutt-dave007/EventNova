// ============================================
// service/RegistrationService.java — FINAL, COMPLETE VERSION
// ============================================
package service;

import collections.WaitingListManager;
import dao.EventDAO;
import dao.ProcedureDAO;
import dao.RegistrationDAO;
import exception.EligibilityException;
import exception.EventFullException;
import exception.InsufficientBalanceException;
import exception.PaymentFailedException;
import exception.RegistrationClosedException;
import model.CancellationResult;
import model.Event;
import model.EventCancellationResult;
import model.Registration;
import model.RegistrationResult;
import model.User;

import java.sql.SQLException;
import java.util.List;

public class RegistrationService {

    private final EventDAO eventDAO = new EventDAO();
    private final RegistrationDAO registrationDAO = new RegistrationDAO();
    private final ProcedureDAO procedureDAO = new ProcedureDAO();
    private final WaitingListManager waitingListManager = new WaitingListManager();

    public boolean checkEligibility(Event event, User user) {
        switch (event.getEligibility()) {
            case ALL:
                return true;
            case STUDENT_NATIVE:
                return user.getRole() == User.Role.STUDENT
                        && user.getInstituteId() != null
                        && user.getInstituteId() == event.getInstituteId();
            case FACULTY_NATIVE:
                return user.getRole() == User.Role.FACULTY
                        && user.getInstituteId() != null
                        && user.getInstituteId() == event.getInstituteId();
            case EXTERNAL:
                return user.getInstituteId() == null
                        || user.getInstituteId() != event.getInstituteId();
            default:
                return false;
        }
    }


    public RegistrationResult register(User user, Event event, java.util.List<model.TeamMember> teamMembers,
                                       String paymentModeOrNull, boolean isCash)
            throws SQLException, RegistrationClosedException, EligibilityException,
            EventFullException, InsufficientBalanceException, PaymentFailedException {

        if (event.getStatus() != Event.Status.OPEN) {
            throw new RegistrationClosedException("Registrations are closed for this event. Current status: " + event.getStatus());
        }

        if (!checkEligibility(event, user)) {
            throw new EligibilityException("You do not meet the eligibility criteria (" + event.getEligibility() +
                    ") for this event. Reason: your role/institute does not match the requirement.");
        }

        int confirmedCount = eventDAO.getConfirmedRegistrationCount(event.getEventId());
        if (confirmedCount >= event.getCapacity()) {
            waitingListManager.addToWaitingList(event.getEventId(), user.getUserId());
            throw new EventFullException("Event is full (" + confirmedCount + "/" + event.getCapacity() +
                    "). You have been added to the waiting list.");
        }

        boolean isTeam = event.getParticipationType() == Event.ParticipationType.TEAM;
        int teamSize = isTeam ? 1 + (teamMembers == null ? 0 : teamMembers.size()) : 1;
        double totalAmount = event.getTicketPrice() * teamSize;

        RegistrationResult result;
        if (event.isFree()) {
            result = procedureDAO.registerFree(user.getUserId(), event.getEventId(), event.getInstituteId(), isTeam);
        } else {
            result = procedureDAO.registerAndPay(user.getUserId(), event.getEventId(), event.getInstituteId(),
                    totalAmount, paymentModeOrNull, isTeam, isCash);
        }

        if (!result.isSuccess()) {
            if (result.getMessage() != null && result.getMessage().toLowerCase().contains("insufficient balance")) {
                throw new InsufficientBalanceException(result.getMessage());
            }
            throw new PaymentFailedException(result.getMessage());
        }

        if (isTeam && teamMembers != null && !teamMembers.isEmpty()) {
            registrationDAO.insertTeamMembers(result.getRegistrationId(), teamMembers);
        }

        return result;
    }

    public Registration getById(int registrationId) throws SQLException {
        return registrationDAO.getById(registrationId);
    }

    public boolean hasWaitingUsers(int eventId) {
        return waitingListManager.hasWaitingUsers(eventId);
    }

    public Integer promoteNext(int eventId) {
        return waitingListManager.promoteNext(eventId);
    }

    public CancellationResult cancelByUser(int registrationId) throws SQLException {
        return procedureDAO.cancelRegistrationByUser(registrationId);
    }

    public EventCancellationResult cancelEventByInstitute(int eventId) throws SQLException {
        return procedureDAO.cancelEventByInstitute(eventId);
    }

    public List<Registration> getByUser(int userId) throws SQLException {
        return registrationDAO.getByUser(userId);
    }

    public List<Registration> getByEvent(int eventId) throws SQLException {
        return registrationDAO.getByEvent(eventId);
    }
}