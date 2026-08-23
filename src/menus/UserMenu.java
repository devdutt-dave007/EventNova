// ============================================
// menus/UserMenu.java — FINAL, COMPLETE VERSION
// ============================================
package menus;

import exception.EligibilityException;
import exception.EventFullException;
import exception.InsufficientBalanceException;
import exception.PaymentFailedException;
import exception.RegistrationClosedException;
import model.*;
import service.*;
import util.ConsoleInput;
import util.InvoiceGenerator;
import model.TeamMember;
import java.util.*;
import java.util.Scanner;

public class UserMenu {

    private final Scanner sc;
    private final ConsoleInput input;
    private final EventService eventService;
    private final RegistrationService registrationService;
    private final TransactionService transactionService;
    private final WalletService walletService;
    private final UserService userService;
    private final User loggedInUser;

    public UserMenu(Scanner sc, User loggedInUser) {
        this.sc = sc;
        this.input = new ConsoleInput(sc);
        this.eventService = new EventService();
        this.registrationService = new RegistrationService();
        this.transactionService = new TransactionService();
        this.walletService = new WalletService();
        this.userService = new UserService();
        this.loggedInUser = loggedInUser;
    }

    public void show() {
        boolean running = true;
        while (running) {
            try {
                double balance = userService.getBalance(loggedInUser.getUserId());
                System.out.println("\n===== USER MENU (" + loggedInUser.getName() + ") =====");
                System.out.println("EventNova Wallet Balance: Rs. " + balance);
            } catch (Exception e) {
                System.out.println("\n===== USER MENU (" + loggedInUser.getName() + ") =====");
            }

            System.out.println("1. Search Events");
            System.out.println("2. View Recently Published Events");
            System.out.println("3. Register for Event");
            System.out.println("4. Cancel My Registration");
            System.out.println("5. My Registrations");
            System.out.println("6. My Transaction History");
           // System.out.println("7. My Wallet History");
            System.out.println("7. Recharge Wallet");
            System.out.println("0. Logout");

            int choice = input.readMenuChoice("Choose: ", 0, 8);

            try {
                switch (choice) {
                    case 1: new EventSearchMenu(sc).show(); break;
                    case 2: eventService.printRecentEvents(); break;
                    case 3: registerForEvent(); break;
                    case 4: cancelRegistration(); break;
                    case 5: viewMyRegistrations(); break;
                    case 6: viewTransactionHistory(); break;
                    //case 7: viewWalletHistory(); break;
                    case 7: rechargeWallet(); break;
                    case 0: running = false; break;
                }
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }
    Event event1=null;
    private void registerForEvent() throws Exception{
        try {
            int eventId = input.readPositiveInt("Enter Event ID: ");
            Event event = eventService.getById(eventId);
            event1=event;
            if (event == null) {
                System.out.println("[FAILED] Event not found. Reason: no event exists with ID " + eventId + ".");
                return;
            }

            if (!registrationService.checkEligibility(event, loggedInUser)) {
                System.out.println("[FAILED] You aren't eligible for this event.");
                return;
            }

            if(eventService.getActiveRegistrationCount(eventId)==event.getCapacity()){
                System.out.println("[FAILED] Capacity full for this Event !");
                return;
            }

            List<TeamMember> teamMembers = new ArrayList<>();
            int teamSize = 1;
            if (event.getParticipationType() == Event.ParticipationType.TEAM) {
                teamMembers = collectTeamMemberCredentials(eventId);
                if(teamMembers==null) return;
                teamSize = teamMembers.size() + 1;
            }

            String paymentModeStr = null;
            boolean isCash = false;
            double totalAmount = 0.0;

            if (!event.isFree()) {
                totalAmount = event.getTicketPrice() * teamSize;
                System.out.println("Ticket Price (per head): Rs. " + event.getTicketPrice());
                if (teamSize > 1) {
                    System.out.println("Team Size: " + teamSize + " | Total Amount: Rs. " + totalAmount);
                }
                Transaction.PaymentMode mode = input.readPaymentMode();
                paymentModeStr = mode.name();
                isCash = (mode == Transaction.PaymentMode.CASH);
            }

            RegistrationResult result = registrationService.register(loggedInUser, event, teamMembers, paymentModeStr, isCash);

            System.out.println("[SUCCESS] " + result.getMessage());

            if (isCash) {
                System.out.println("You can collect your pass at " + event.getVenue() +
                        " by " + event.getEventDate() + " by paying " + totalAmount + " rupees in cash.");
            }

            Transaction transaction = transactionService.getById(result.getTransactionId());
            Registration savedRegistration = registrationService.getById(result.getRegistrationId());
            String teamIdForInvoice = savedRegistration != null ? savedRegistration.getTeamId() : null;

            boolean wantsInvoice = input.readYesNo("Would you like to generate an invoice?");
            if (wantsInvoice) {
                InvoiceGenerator.generateInvoice(loggedInUser, event, transaction, result.getRegistrationId(), teamIdForInvoice);
            }

        } catch (EventFullException | RegistrationClosedException | EligibilityException e) {
            System.out.println("[FAILED] " + e.getMessage());
        } catch (InsufficientBalanceException e) {
            System.out.println("[FAILED] " + e.getMessage());
        } catch (PaymentFailedException e) {
            System.out.println("[FAILED] Payment failed. Reason: " + e.getMessage());
        } catch (java.sql.SQLException e) {
            System.out.println("[FAILED] Database error. Reason: " + e.getMessage());
        }
    }

    private void cancelRegistration() throws Exception {
        int regId = input.readPositiveInt("Enter Registration ID to cancel: ");
        CancellationResult result = registrationService.cancelByUser(regId);

        if (result.isSuccess()) {
            System.out.println("[SUCCESS] " + result.getMessage());
            if (result.getRefundAmount() > 0) {
                System.out.println("50% refund of Rs. " + result.getRefundAmount() + " credited to your EventNova wallet.");
            } else {
                System.out.println("No refund applicable (free event or payment was not completed).");
            }
        } else {
            System.out.println("[FAILED] " + result.getMessage());
        }
    }

    private void viewMyRegistrations() throws Exception {
        List<Registration> regs = registrationService.getByUser(loggedInUser.getUserId());
        if (regs.isEmpty()) {
            System.out.println("No registrations found.");
        } else {
            for (Registration r : regs) System.out.println(r);
        }
    }

    private void viewTransactionHistory() throws Exception {
        List<Transaction> transactions = transactionService.getByUser(loggedInUser.getUserId());
        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
        } else {
            for (Transaction t : transactions) System.out.println(t);
        }
    }

    private void viewWalletHistory() throws Exception {
        List<WalletTransaction> entries = walletService.getUserHistory(loggedInUser.getUserId());
        if (entries.isEmpty()) {
            System.out.println("No wallet activity found.");
        } else {
            for (WalletTransaction w : entries) System.out.println(w);
        }
        double currentBalance = userService.getBalance(loggedInUser.getUserId());
        System.out.println("---------------------------------------------");
        System.out.println("Current EventNova Wallet Balance: Rs. " + currentBalance);
    }

    private void rechargeWallet() throws Exception {
        double amount = input.readPositiveDouble("Enter amount to recharge: Rs. ");
        boolean success = userService.rechargeWallet(loggedInUser.getUserId(), amount);
        if (success) {
            double newBalance = userService.getBalance(loggedInUser.getUserId());
            System.out.println("[SUCCESS] Wallet recharged. New balance: Rs. " + newBalance);
        } else {
            System.out.println("[FAILED] Recharge failed. Reason: database update did not complete.");
        }
    }

    private List<TeamMember> collectTeamMemberCredentials(int eventId) throws Exception{
        List<TeamMember> teamMembers = new ArrayList<>();
        int totalMembers = input.readPositiveInt("Enter total number of team members (including yourself): ");
        int othersCount = Math.max(0, totalMembers - 1);
        int remainingSeats=event1.getCapacity()-eventService.getActiveRegistrationCount(eventId);
        if(totalMembers>remainingSeats){
            System.out.println("[FAILED] Team exceeds total Event capacity !");
            return null;
        }

        System.out.println("Enter details for the other " + othersCount + " team member(s):");
        for (int i = 1; i <= othersCount; i++) {
            System.out.println("--- Team Member " + i + " ---");
            String mName = input.readNonEmpty("Name: ");
            String mBranch = input.readNonEmpty("Branch: ");
            String mEnrollment = input.readNonEmpty("Enrollment Number: ");
            teamMembers.add(new TeamMember(0, 0, mName, mBranch, mEnrollment));
        }
        return teamMembers;
    }
}