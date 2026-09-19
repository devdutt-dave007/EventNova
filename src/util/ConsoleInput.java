// ============================================
// util/ConsoleInput.java
// Change #1: readCategory() replaces free-text category input
// Change #4: readPassword() still enforces strength rules, but the
// resulting string is stored as-is (no hashing applied anywhere).
// ============================================
package util;

import model.Event;
import model.User;
import model.Transaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class ConsoleInput {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-uuuu")
            .withResolverStyle(java.time.format.ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final Scanner sc;

    public ConsoleInput(Scanner sc)
    {
        this.sc = sc;
    }

    public String readNonEmpty(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (!input.isEmpty()) return input;
            System.out.println("[INVALID] Input cannot be empty. Please try again.");
        }
    }

    public String readInstituteEmail(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println("[INVALID] Email cannot be empty.");
                continue;
            }
            if (!input.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[a-zA-Z]{2,}$")) {
                System.out.println("[INVALID] Not a valid email format (e.g. name@college.edu.in).");
                continue;
            }
            if (!input.toLowerCase().endsWith(".edu.in")) {
                System.out.println("[INVALID] Institute email must end with '.edu.in'. You entered: " + input);
                continue;
            }
            return input;
        }
    }

    // util/ConsoleInput.java — readGenericEmail(), full replacement
    public String readGenericEmail(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (!input.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[a-zA-Z]{2,}$")) {
                System.out.println("[INVALID] Not a valid email format. Reason: must match pattern name@domain.extension");
                continue;
            }
            String localPart = input.substring(0, input.indexOf('@'));
            boolean startsWithDigit = Character.isDigit(localPart.charAt(0));
            boolean isGmail = input.toLowerCase().endsWith("@gmail.com");
            if (isGmail && startsWithDigit) {
                System.out.println("[INVALID] Gmail addresses cannot start with a number. You entered: " + input);
                continue;
            }
            return input;
        }
    }

    public String readPassword(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (input.length() < 8) {
                System.out.println("[INVALID] Password too short (" + input.length() +
                        " chars). Minimum required: 8 characters.");
                continue;
            }
            if (!input.matches(".*[!@#$%^&*()\\-_=+\\[\\]{};:'\",.<>/?\\\\|`~].*")) {
                System.out.println("[INVALID] Password must contain at least 1 special character (e.g. !@#$%).");
                continue;
            }
            return input;
        }
    }

    // util/ConsoleInput.java — readPhone(), full replacement
    public String readPhone(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (input.matches("^[1-9][0-9]{9}$")) return input;
            String reason;
            if (input.length() != 10) {
                reason = "wrong length (" + input.length() + ")";
            } else if (input.startsWith("0")) {
                reason = "cannot start with 0";
            } else {
                reason = "contains non-digit characters";
            }
            System.out.println("[INVALID] Phone number must be exactly 10 digits and cannot start with 0. Reason: " + reason);
        }
    }

    public String readEventName(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println("[INVALID] Event name cannot be empty.");
                continue;
            }
            if (!input.matches("^[a-zA-Z0-9 ]+$")) {
                System.out.println("[INVALID] Event name must not contain special characters. Only letters, numbers, and spaces are allowed.");
                continue;
            }
            return input;
        }
    }

    public int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("[INVALID] '" + input + "' is not a whole number. Reason: digits only expected.");
            }
        }
    }

    public int readPositiveInt(String prompt) {
        while (true) {
            int value = readInt(prompt);
            if (value > 0) return value;
            System.out.println("[INVALID] Value must be greater than 0. You entered: " + value);
        }
    }

    public double readNonNegativeDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                double value = Double.parseDouble(input);
                if (value < 0) {
                    System.out.println("[INVALID] Value cannot be negative. You entered: " + value);
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("[INVALID] '" + input + "' is not a valid number. Reason: wrong syntax.");
            }
        }
    }

    public double readPositiveDouble(String prompt) {
        while (true) {
            double value = readNonNegativeDouble(prompt);
            if (value > 0) return value;
            System.out.println("[INVALID] Value must be greater than 0.");
        }
    }

    public LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt + " (dd-MM-yyyy): ");
            String input = sc.nextLine().trim();
            try {
                LocalDate currentDate=LocalDate.now();
                LocalDate date=LocalDate.parse(input,DATE_FMT);
                if(date.isBefore(currentDate)) System.out.println("[INVALID] "+input+"is not a valid date. Reason: Back Dating not Allowed.");
                else return date;
            } catch (DateTimeParseException e) {
                System.out.println("[INVALID] '" + input + "' is not a valid date. Reason: expected format dd-MM-yyyy (e.g. 25-12-2026).");
            }
        }
    }

    public LocalTime readTime(String prompt) {
        while (true) {
            System.out.print(prompt + " (HH:mm, 24hr): ");
            String input = sc.nextLine().trim();
            try {
                return LocalTime.parse(input, TIME_FMT);
            } catch (DateTimeParseException e) {
                System.out.println("[INVALID] '" + input + "' is not a valid time. Reason: expected 24-hour format HH:mm.");
            }
        }
    }

    public LocalDateTime readDateTime(String label) {
        LocalDate date = readDate(label + " Date");
        LocalTime time = readTime(label + " Time");
        return LocalDateTime.of(date, time);
    }

    public boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt + " (Y/N): ");
            String input = sc.nextLine().trim().toUpperCase();
            if (input.equals("Y") || input.equals("YES")) return true;
            if (input.equals("N") || input.equals("NO")) return false;
            System.out.println("[INVALID] Please enter Y or N. Reason: unrecognized response '" + input + "'.");
        }
    }

    public int readMenuChoice(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                int choice = Integer.parseInt(input);
                if (choice < min || choice > max) {
                    System.out.println("[INVALID] Choice must be between " + min + " and " + max + ". You entered: " + choice);
                    continue;
                }
                return choice;
            } catch (NumberFormatException e) {
                System.out.println("[INVALID] '" + input + "' is not a number. Reason: menu choice must be numeric.");
            }
        }
    }

    // Change #1: fixed category list — no more free-text drift
    public Event.Category readCategory() {
        System.out.println("Select Category:");
        System.out.println("1. Fest");
        System.out.println("2. Music");
        System.out.println("3. Dance");
        System.out.println("4. Hackathon");
        System.out.println("5. Seminar");
        System.out.println("6. Tech Fest");
        System.out.println("7. Drama");
        System.out.println("8. Sports");
        int choice = readMenuChoice("Choice: ", 1, 8);
        switch (choice) {
            case 1: return Event.Category.FEST;
            case 2: return Event.Category.MUSIC;
            case 3: return Event.Category.DANCE;
            case 4: return Event.Category.HACKATHON;
            case 5: return Event.Category.SEMINAR;
            case 6: return Event.Category.TECH_FEST;
            case 7: return Event.Category.DRAMA;
            default: return Event.Category.SPORTS;
        }
    }

    public Event.Eligibility readEligibility() {
        System.out.println("Select Eligibility:");
        System.out.println("1. Student of native institute only");
        System.out.println("2. Faculty of native institute only");
        System.out.println("3. External (students/faculty of OTHER institutes)");
        System.out.println("4. All (anyone can register)");
        int choice = readMenuChoice("Choice: ", 1, 4);
        switch (choice) {
            case 1: return Event.Eligibility.STUDENT_NATIVE;
            case 2: return Event.Eligibility.FACULTY_NATIVE;
            case 3: return Event.Eligibility.EXTERNAL;
            default: return Event.Eligibility.ALL;
        }
    }

    public User.Role readUserRole() {
        System.out.println("Select Role:");
        System.out.println("1. Student");
        System.out.println("2. Faculty");
        System.out.println("3. External (from another institute)");
        int choice = readMenuChoice("Choice: ", 1, 3);
        switch (choice) {
            case 1: return User.Role.STUDENT;
            case 2: return User.Role.FACULTY;
            default: return User.Role.EXTERNAL;
        }
    }

    public Transaction.PaymentMode readPaymentMode() {
        System.out.println("Select Payment Mode:");
        System.out.println("1. CASH (pay at venue, wallet untouched)");
        System.out.println("2. UPI");
        System.out.println("3. CARD");
        System.out.println("4. NET_BANKING");
        int choice = readMenuChoice("Choice: ", 1, 4);
        switch (choice) {
            case 1: return Transaction.PaymentMode.CASH;
            case 2: return Transaction.PaymentMode.UPI;
            case 3: return Transaction.PaymentMode.CARD;
            default: return Transaction.PaymentMode.NET_BANKING;
        }
    }
}