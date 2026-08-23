// ============================================
// util/FileExporter.java
// ============================================
package util;

import model.Registration;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class FileExporter {

    public static void exportParticipantList(int eventId, List<Registration> registrations) {
        String fileName = "Participants_Event_" + eventId + ".txt";

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName)))) {
            writer.println("Participant List - Event ID: " + eventId);
            writer.println("=========================================");
            for (Registration r : registrations) {
                writer.println("Registration_ID: " + r.getRegistrationId() +
                        " | User_ID: " + r.getUserId() +
                        " | Status: " + r.getRegistrationStatus());
            }
            writer.println("=========================================");
            writer.println("Total Registrations: " + registrations.size());
            System.out.println("Participant list exported to " + fileName);
        } catch (IOException e) {
            System.out.println("Failed to export participant list: " + e.getMessage());
        }
    }
}