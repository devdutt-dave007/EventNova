// ============================================
// menus/InstituteMenu.java
// ============================================
// ============================================
// menus/InstituteMenu.java — FULL REPLACEMENT
// ============================================
package menus;

import database.DBConnection;
import exception.InvalidDateException;
import model.*;
import service.*;
import util.ConsoleInput;
import util.FileExporter;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Scanner;

public class InstituteMenu {

    private final Scanner sc;
    private final ConsoleInput input;
    private final EventService eventService;
    private final RegistrationService registrationService;
    private final TransactionService transactionService;
    private final WalletService walletService;
    private final InstituteService instituteService;
    private final Institute loggedInInstitute;

    public InstituteMenu(Scanner sc, Institute loggedInInstitute) {
        this.sc = sc;
        this.input = new ConsoleInput(sc);
        this.eventService = new EventService();
        this.registrationService = new RegistrationService();
        this.transactionService = new TransactionService();
        this.walletService = new WalletService();
        this.instituteService = new InstituteService();
        this.loggedInInstitute = loggedInInstitute;
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println("\n===== INSTITUTE MENU (" + loggedInInstitute.getInstituteName() + ") =====");
            System.out.println("1. Create Event");
            System.out.println("2. Update Event");
            System.out.println("3. Delete Event");
            System.out.println("4. Close Registration (Manual)");
            System.out.println("5. Cancel Event (Full Refund to All)");
            System.out.println("6. View My Events");
            System.out.println("7. View Registrations for an Event");
            System.out.println("8. View Transaction Logs");
            //System.out.println("9. View Wallet History");
            System.out.println("9. Export Participant List");
            System.out.println("10. Search Events (Public View)");
            System.out.println("11. Check Current Balance");
            System.out.println("12. Reactivate Registration");
            System.out.println("0. Logout");

            int choice = input.readMenuChoice("Choose: ", 0, 13);

            try {
                switch (choice) {
                    case 1: createEvent(); break;
                    case 2: updateEvent(); break;
                    case 3: deleteEvent(); break;
                    case 4: closeRegistration(); break;
                    case 5: cancelEvent(); break;
                    case 6: viewMyEvents(); break;
                    case 7: viewRegistrations(); break;
                    case 8: viewTransactionLogs(); break;
                    //case 9: viewWalletHistory(); break;
                    case 9: exportParticipants(); break;
                    case 10: new EventSearchMenu(sc).show(); break;
                    case 11: checkBalance(); break;
                    case 12: reactivation();break;
                    case 0: running = false; break;
                }
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }

    private void createEvent() {
        try {
            String name = input.readEventName("Event Name: ");
            Event.Category category = input.readCategory();
            Event.Eligibility eligibility = input.readEligibility();

            System.out.println("Participation Type: 1. SOLO  2. TEAM");
            int pTypeChoice = input.readMenuChoice("Choose: ", 1, 2);
            Event.ParticipationType pType = pTypeChoice == 1 ? Event.ParticipationType.SOLO : Event.ParticipationType.TEAM;

            String description = input.readNonEmpty("Description: ");
            String venue = input.readNonEmpty("Venue: ");
            int capacity = input.readPositiveInt("Capacity: ");
            double price = input.readNonNegativeDouble("Ticket Price (0 for free): Rs. ");

            System.out.println("--- Registration Deadline ---");
            LocalDateTime deadline = input.readDateTime("Registration Deadline");
            boolean checkDate=true;LocalDate eventDate=LocalDate.now();
            while(checkDate){
            System.out.println("--- Event Schedule ---");
            eventDate = input.readDate("Event Date");
            LocalDate temp=deadline.toLocalDate();
            if(eventDate.isBefore(temp)||eventDate.isEqual(temp)) System.out.println("Event Date cannot be equal or before DeadLine ");
             else checkDate=false;
            }

            boolean check=true;LocalTime endTime=LocalTime.now(),startTime=LocalTime.now();
            while (check) {
                 startTime = input.readTime("Event Start Time");
                 endTime= input.readTime("Event End Time");
                if(endTime.isBefore(startTime)) System.out.println("[INVALID] These are not a valid Timings. Reason: Start Time cannot be after End Time.");
                else {
                    check = false;
                }
            }
            Event event = new Event(0, loggedInInstitute.getInstituteId(), name, category, eligibility,
                    pType, description, venue, capacity, price, eventDate, startTime, endTime,
                    deadline, Event.Status.OPEN);

            int id = eventService.createEvent(event);
            System.out.println("[SUCCESS] Event created with Event_ID: " + id);

        } catch (InvalidDateException e) {
            System.out.println("[FAILED] " + e.getMessage());
        } catch (java.sql.SQLException e) {
            System.out.println("[FAILED] Database error. Reason: " + e.getMessage());
        }
    }

    private void updateEvent() {
        try {
            int eventId = input.readPositiveInt("Enter Event ID to update: ");
            Event event = eventService.getById(eventId);
            if (event == null) {
                System.out.println("[FAILED] Event not found. Reason: no event exists with ID " + eventId + ".");
                return;
            }

            if (event.getInstituteId() != loggedInInstitute.getInstituteId()) {
                System.out.println("[FAILED] You are not authorized to modify this event. Reason: this event belongs to a different institute.");
                return;
            }

            String venue = event.getVenue();
            if (input.readYesNo("Change venue? (current: " + venue + ")")) {
                venue = input.readNonEmpty("New Venue: ");
            }

            int capacity = event.getCapacity();
            if (input.readYesNo("Change capacity? (current: " + capacity + ")")) {
                capacity = input.readPositiveInt("New Capacity: ");
            }

            Event updated = new Event(event.getEventId(), event.getInstituteId(), event.getEventName(),
                    event.getCategory(), event.getEligibility(), event.getParticipationType(),
                    event.getDescription(), venue, capacity, event.getTicketPrice(), event.getEventDate(),
                    event.getStartTime(), event.getEndTime(), event.getRegistrationDeadline(), event.getStatus());

            boolean success = eventService.updateEvent(updated);
            System.out.println(success ? "[SUCCESS] Event updated." : "[FAILED] Update failed. Reason: no rows affected.");

        } catch (InvalidDateException e) {
            System.out.println("[FAILED] " + e.getMessage());
        } catch (java.sql.SQLException e) {
            System.out.println("[FAILED] Database error. Reason: " + e.getMessage());
        }
    }

    private void deleteEvent() {
        try {
            int eventId = input.readPositiveInt("Enter Event ID to delete: ");
            Event event = eventService.getById(eventId);
            if (event == null) {
                System.out.println("[FAILED] Event not found. Reason: no event exists with ID " + eventId + ".");
                return;
            }

            if (event.getInstituteId() != loggedInInstitute.getInstituteId()) {
                System.out.println("[FAILED] You are not authorized to modify this event. Reason: this event belongs to a different institute.");
                return;
            }

            int activeRegs = eventService.getActiveRegistrationCount(eventId);
            if (activeRegs > 0) {
                System.out.println("[FAILED] Cannot delete — " + activeRegs +
                        " user(s) already registered. Use 'Cancel Event' instead to refund them properly.");
                return;
            }

            boolean deleted = eventService.deleteEvent(eventId);
            System.out.println(deleted ? "[SUCCESS] Event deleted." : "[FAILED] Delete failed. Reason: no event found with that ID.");

        } catch (java.sql.SQLException e) {
            System.out.println("[FAILED] Database error. Reason: " + e.getMessage());
        }
    }

    private void closeRegistration() {
        try {
            int eventId = input.readPositiveInt("Enter Event ID to close registrations for: ");
            Event event = eventService.getById(eventId);
            if (event == null) {
                System.out.println("[FAILED] Event not found. Reason: no event exists with ID " + eventId + ".");
                return;
            }

            if (event.getInstituteId() != loggedInInstitute.getInstituteId()) {
                System.out.println("[FAILED] You are not authorized to modify this event. Reason: this event belongs to a different institute.");
                return;
            }

            boolean success = eventService.closeRegistration(eventId);
            System.out.println(success ? "[SUCCESS] Registrations closed for this event."
                    : "[FAILED] Could not close registration. Reason: no rows affected.");

        } catch (java.sql.SQLException e) {
            System.out.println("[FAILED] Database error. Reason: " + e.getMessage());
        }
    }

    private void cancelEvent() {
        try {
            int eventId = input.readPositiveInt("Enter Event ID to cancel: ");
            Event event = eventService.getById(eventId);
            if (event == null) {
                System.out.println("[FAILED] Event not found. Reason: no event exists with ID " + eventId + ".");
                return;
            }

            if (event.getInstituteId() != loggedInInstitute.getInstituteId()) {
                System.out.println("[FAILED] You are not authorized to modify this event. Reason: this event belongs to a different institute.");
                return;
            }

            boolean confirm = input.readYesNo("This will refund ALL confirmed users in full. Confirm cancellation?");
            if (!confirm) {
                System.out.println("Cancellation aborted.");
                return;
            }

            EventCancellationResult result = registrationService.cancelEventByInstitute(eventId);
            if (result.isSuccess()) {
                System.out.println("[SUCCESS] " + result.getMessage());
                System.out.println("Users refunded: " + result.getRefundedCount());
            } else {
                System.out.println("[FAILED] " + result.getMessage());
            }

        } catch (java.sql.SQLException e) {
            System.out.println("[FAILED] Database error. Reason: " + e.getMessage());
        }
    }

    private void viewMyEvents() throws Exception {
        List<Event> events = eventService.getByInstitute(loggedInInstitute.getInstituteId());
        if (events.isEmpty()) System.out.println("No events found.");
        else for (Event e : events) System.out.println(e);
    }

    private void viewRegistrations() throws Exception {
        int eventId = input.readPositiveInt("Enter Event ID: ");
        List<Registration> regs = registrationService.getByEvent(eventId);
        if (regs.isEmpty()) System.out.println("No registrations found.");
        else for (Registration r : regs) System.out.println(r);
    }

    private void viewTransactionLogs() throws Exception {
        List<Transaction> transactions = transactionService.getByInstitute(loggedInInstitute.getInstituteId());
        if (transactions.isEmpty()) System.out.println("No transaction records found.");
        else for (Transaction t : transactions) System.out.println(t);
    }

    private void viewWalletHistory() throws Exception {
        List<WalletTransaction> entries = walletService.getInstituteHistory(loggedInInstitute.getInstituteId());
        if (entries.isEmpty()) {
            System.out.println("No wallet activity found.");
            return;
        }

        double totalReceived = 0;
        double totalRefunded = 0;

        for (WalletTransaction w : entries) {
            System.out.println(w);
            if (w.getEntryType() == WalletTransaction.EntryType.EVENT_PAYMENT_CREDIT) {
                totalReceived += w.getAmount();
            } else if (w.getEntryType() == WalletTransaction.EntryType.REFUND_DEBIT) {
                totalRefunded += w.getAmount();
            }
        }

        System.out.println("---------------------------------------------");
        System.out.println("Total Received from Registrations: Rs. " + totalReceived);
        System.out.println("Total Refunded to Users (Cancellations): Rs. " + totalRefunded);
    }

    private void exportParticipants() throws Exception {
        int eventId = input.readPositiveInt("Enter Event ID: ");
        List<Registration> regs = registrationService.getByEvent(eventId);
        FileExporter.exportParticipantList(eventId, regs);
    }

    private void checkBalance() throws java.sql.SQLException {
        double balance = instituteService.getBalance(loggedInInstitute.getInstituteId());
        System.out.println("Current EventNova Wallet Balance: Rs. " + balance);
    }
    public void reactivation() throws Exception {

        int eventId = input.readInt("Enter Event ID to Reactivate : ");

        Connection con = DBConnection.getConnection();

        PreparedStatement p = con.prepareStatement(
                "SELECT * FROM Event WHERE Event_ID=? AND Institute_ID=?");

        p.setInt(1, eventId);
        p.setInt(2, loggedInInstitute.getInstituteId());

        ResultSet rs = p.executeQuery();

        if (!rs.next()) {
            System.out.println("Event not found.");
            return;
        }

        String status = rs.getString("Status");

        if (status.equalsIgnoreCase("OPEN")) {
            System.out.println("Event already active.");
            return;
        }

        LocalDateTime current = LocalDateTime.now();

        LocalDateTime oldEventDate =
                rs.getTimestamp("Event_Date").toLocalDateTime();

        LocalDateTime oldDeadline =
                rs.getTimestamp("Registration_Deadline").toLocalDateTime();

        // If event has already finished
        if (!current.isBefore(oldEventDate)) {

            PreparedStatement ps = con.prepareStatement(
                    "UPDATE Event SET Status='COMPLETED' WHERE Event_ID=?");

            ps.setInt(1, eventId);
            ps.executeUpdate();

            System.out.println("Sorry! Event has already been completed. Registration cannot be reopened.");
            return;
        }

        boolean confirm = input.readYesNo("Are you sure you want to reopen registration?");

        if (!confirm)
            return;

        LocalDateTime newEventDate = oldEventDate;
        LocalDateTime newDeadline = oldDeadline;

        boolean deadlineExpired = !current.isBefore(oldDeadline);

        /* -------------------- EVENT DATE -------------------- */

        boolean changeEvent = input.readYesNo("Do you want to change Event Date?");

        if (changeEvent) {

            while (true) {

                newEventDate = input.readDateTime("New Event");

                if (newEventDate.isBefore(current)) {
                    System.out.println("Back dating not allowed.");
                    continue;
                }

                if (!newEventDate.isAfter(oldEventDate)) {
                    System.out.println("New Event Date must be after the original Event Date.");
                    continue;
                }

                break;
            }
        }

        /* -------------------- REGISTRATION DEADLINE -------------------- */

        if (deadlineExpired) {

            System.out.println();
            System.out.println("Registration deadline has already expired.");
            System.out.println("You must enter a new Registration Deadline.");

            while (true) {

                newDeadline = input.readDateTime("New Registration Deadline");

                if (newDeadline.isBefore(current)) {
                    System.out.println("Back dating not allowed.");
                    continue;
                }

                if (!newDeadline.isAfter(oldDeadline)) {
                    System.out.println("Registration Deadline must be after the original deadline.");
                    continue;
                }

                if (!newDeadline.isBefore(newEventDate)) {
                    System.out.println("Registration Deadline must be before Event Date.");
                    continue;
                }

                break;
            }

        } else {

            boolean changeDeadline =
                    input.readYesNo("Do you want to change Registration Deadline?");

            if (changeDeadline) {

                while (true) {

                    newDeadline = input.readDateTime("New Registration Deadline");

                    if (newDeadline.isBefore(current)) {
                        System.out.println("Back dating not allowed.");
                        continue;
                    }

                    if (!newDeadline.isAfter(oldDeadline)) {
                        System.out.println("Registration Deadline must be after the original deadline.");
                        continue;
                    }

                    if (!newDeadline.isBefore(newEventDate)) {
                        System.out.println("Registration Deadline must be before Event Date.");
                        continue;
                    }

                    break;
                }
            }
        }

        p = con.prepareStatement(
                "UPDATE Event SET Event_Date=?, Registration_Deadline=?, Status='OPEN' WHERE Event_ID=?");

        p.setTimestamp(1, Timestamp.valueOf(newEventDate));
        p.setTimestamp(2, Timestamp.valueOf(newDeadline));
        p.setInt(3, eventId);

        int rows = p.executeUpdate();

        if (rows > 0)
            System.out.println("\nRegistration reactivated successfully.");
        else
            System.out.println("\nFailed to reactivate registration.");
    }
            }