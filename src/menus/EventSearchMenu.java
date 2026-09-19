// ============================================
// menus/EventSearchMenu.java
// Change #1: category search now uses the fixed enum picker
// ============================================
package menus;

import model.Event;
import service.EventService;
import util.ConsoleInput;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class EventSearchMenu {

    private final ConsoleInput input;
    private final EventService eventService;

    public EventSearchMenu(Scanner sc) {
        this.input = new ConsoleInput(sc);
        this.eventService = new EventService();
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println("\n===== PUBLIC EVENT SEARCH (No Login Required) =====");
            System.out.println("1. Search by Name");
            System.out.println("2. Search by Category");
            System.out.println("3. Search by Date");
            System.out.println("4. Search by Institute");
            System.out.println("5. Search by Fee Status (Paid/Free)");
            System.out.println("6. View All Events (sorted by date)");
            System.out.println("0. Back");

            int choice = input.readMenuChoice("Choose: ", 0, 6);

            try {
                switch (choice) {
                    case 1: printResults(eventService.searchByName(input.readNonEmpty("Enter keyword: "))); break;
                    case 2: printResults(eventService.searchByCategory(input.readCategory())); break;
                    case 3: printResults(eventService.searchByDate(input.readDate("Enter event date"))); break;
                    case 4: printResults(eventService.searchByInstitute(input.readPositiveInt("Enter Institute ID: "))); break;
                    case 5: {
                        System.out.println("1. Free Events  2. Paid Events");
                        int c = input.readMenuChoice("Choose: ", 1, 2);
                        printResults(eventService.searchByFeeStatus(c == 1));
                        break;
                    }
                    case 6: printResults(eventService.getSortedByDate()); break;
                    case 0: running = false; break;
                }
            } catch (Exception e) {
                System.out.println("[ERROR] Search failed. Reason: " + e.getMessage());
            }
        }
    }

    private void printResults(List<Event> events) {
        if (events.isEmpty()) {
            System.out.println("No events found matching your search.");
            return;
        }
        System.out.println("---- " + events.size() + " Event(s) Found ----");
        for (Event e : events) System.out.println(e);
    }
}