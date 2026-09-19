// ============================================
// util/InvoiceGenerator.java
// Bug #10 fix: the Transaction object passed here is ALWAYS freshly
// read from the database (TransactionDAO.getById), so transactionDate
// is guaranteed to be the real DB-generated timestamp, never null.
// ============================================
// util/InvoiceGenerator.java — full replacement
package util;

import model.Event;
import model.Transaction;
import model.User;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class InvoiceGenerator {

    public static void generateInvoice(User user, Event event, Transaction transaction, int registrationId, String teamId) {
        String fileName = "Invoice_TXN" + transaction.getTransactionId() + ".txt";

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName)))) {
            writer.println("================ EVENTNOVA INVOICE ================");
            writer.println("Registration ID  : " + registrationId);
            writer.println("Transaction ID   : " + transaction.getTransactionId());
            writer.println("Transaction Date : " + transaction.getTransactionDate());
            writer.println("Payment          : " + transaction.getPaymentMode());
            writer.println("Status           : " + transaction.getTransactionStatus());
            writer.println("-----------------------------------------------------");
            writer.println("User Name        : " + user.getName());
            writer.println("User Email       : " + user.getEmail());
            if (teamId != null && !teamId.isEmpty()) {
                writer.println("Team ID          : " + teamId);
            }
            writer.println("-----------------------------------------------------");
            writer.println("Event Name       : " + event.getEventName());
            writer.println("Event Date       : " + event.getEventDate());
            writer.println("Venue            : " + event.getVenue());
            writer.println("-----------------------------------------------------");
            writer.println("Amount Paid      : Rs. " + transaction.getAmount());
            writer.println("=====================================================");
            writer.println("Thank you for registering through EventNova!");
            writer.println("Keep your Registration ID safe — you will need it to cancel this registration later.");

            System.out.println("Invoice generated: " + fileName);
        } catch (IOException e) {
            System.out.println("Failed to generate invoice: " + e.getMessage());
        }
    }
}