// ============================================
// menus/MainMenu.java
// Change #4: plaintext password, passed straight through
// Change #5: welcome bonus message on successful user registration
// ============================================
package menus;

import exception.InvalidLoginException;
import model.Institute;
import model.User;
import service.InstituteService;
import service.UserService;
import util.ConsoleInput;

import java.util.Scanner;

public class MainMenu {

    private static final int MAX_LOGIN_ATTEMPTS = 5;

    private final Scanner sc = new Scanner(System.in);
    private final ConsoleInput input = new ConsoleInput(sc);
    private final InstituteService instituteService = new InstituteService();
    private final UserService userService = new UserService();

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n========== EventNova ==========");
            System.out.println("1. Institute Registration");
            System.out.println("2. Institute Login");
            System.out.println("3. User Registration");
            System.out.println("4. User Login");
            System.out.println("5. Search Events (No Login Required)");
            System.out.println("0. Exit");

            int choice = input.readMenuChoice("Choose: ", 0, 5);

            try {
                switch (choice) {
                    case 1: instituteRegistrationFlow(); break;
                    case 2: instituteLoginFlow(); break;
                    case 3: userRegistrationFlow(); break;
                    case 4: userLoginFlow(); break;
                    case 5: new EventSearchMenu(sc).show(); break;
                    case 0:
                        running = false;
                        System.out.println("Thank you for using EventNova.");
                        break;
                }
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }

    private void instituteRegistrationFlow() {
        try {
            registerInstitute();
            System.out.println("\nRegistration successful! Redirecting to login...");
            instituteLoginFlow();
        } catch (java.sql.SQLException e) {
            System.out.println("[FAILED] Registration failed. Reason: " + e.getMessage());
        }
    }

    private void registerInstitute() throws java.sql.SQLException {
        System.out.println("\n----- Institute Registration -----");
        String name = input.readNonEmpty("Institute Name: ");
        String email;
        while (true) {
            email = input.readInstituteEmail("Email (must end with .edu.in): ");
            if (instituteService.emailExists(email)) {
                System.out.println("[INVALID] This email is already registered. Please use a different email.");
                continue;
            }
            break;
        }

        String password = input.readPassword("Password (8 chars, 1 special char): ");
        String address = input.readNonEmpty("Address: ");

        int id = instituteService.registerInstitute(name, email, password, address);
        System.out.println("Institute registered successfully with ID: " + id);
    }

    private void instituteLoginFlow() throws java.sql.SQLException {
        int attempts = 0;
        while (attempts < MAX_LOGIN_ATTEMPTS) {
            System.out.println("\n----- Institute Login (Attempt " + (attempts + 1) + "/" + MAX_LOGIN_ATTEMPTS + ") -----");
            String email = input.readNonEmpty("Email: ");
            String password = input.readNonEmpty("Password: ");

            try {
                Institute institute = instituteService.login(email, password);
                System.out.println("[SUCCESS] Login successful. Welcome " + institute.getInstituteName());
                new InstituteMenu(sc, institute).show();
                return;
            } catch (InvalidLoginException e) {
                attempts++;
                System.out.println("[FAILED] " + e.getMessage() + " Attempts left: " + (MAX_LOGIN_ATTEMPTS - attempts));
            } catch (java.sql.SQLException e) {
                System.out.println("[FAILED] Could not reach the database. Reason: " + e.getMessage());
                return;
            }
        }

        System.out.println("\nToo many failed login attempts. Redirecting to Institute Registration...");
        instituteRegistrationFlow();
    }

    private void userRegistrationFlow() {
        try {
            registerUser();
            // Change #5: welcome bonus message
            System.out.println("You received a welcome bonus of rupees 500 in your EventNova wallet!");
            System.out.println("\nRedirecting to login...");
            userLoginFlow();
        } catch (java.sql.SQLException e) {
            System.out.println("[FAILED] Registration failed. Reason: " + e.getMessage());
        }
    }

    private void registerUser() throws java.sql.SQLException {
        System.out.println("\n----- User Registration -----");
        String name = input.readNonEmpty("Name: ");

        String email;
        while (true) {
            email = input.readGenericEmail("Email: ");
            if (userService.emailExists(email)) {
                System.out.println("[INVALID] This email is already registered. Please use a different email.");
                continue;
            }
            break;
        }

        String password = input.readPassword("Password (8+ chars, 1 special char): ");
        String phone = input.readPhone("Phone (10 digits): ");
        User.Role role = input.readUserRole();

        Integer instituteId = null;
        String enrollmentNo = null;
        String employeeId = null;

        if (role == User.Role.STUDENT) {
            instituteId = input.readPositiveInt("Institute ID: ");
            enrollmentNo = input.readNonEmpty("Enrollment No: ");
        } else if (role == User.Role.FACULTY) {
            instituteId = input.readPositiveInt("Institute ID: ");
            employeeId = input.readNonEmpty("Employee ID: ");
        }

        int id = userService.registerUser(name, email, password, phone, role, instituteId, enrollmentNo, employeeId);
        System.out.println("User registered successfully with ID: " + id);
    }

    private void userLoginFlow() throws java.sql.SQLException {
        int attempts = 0;
        while (attempts < MAX_LOGIN_ATTEMPTS) {
            System.out.println("\n----- User Login (Attempt " + (attempts + 1) + "/" + MAX_LOGIN_ATTEMPTS + ") -----");
            String email = input.readNonEmpty("Email: ");
            String password = input.readNonEmpty("Password: ");

            try {
                User user = userService.login(email, password);
                System.out.println("[SUCCESS] Login successful. Welcome " + user.getName());
                new UserMenu(sc, user).show();
                return;
            } catch (InvalidLoginException e) {
                attempts++;
                System.out.println("[FAILED] " + e.getMessage() + " Attempts left: " + (MAX_LOGIN_ATTEMPTS - attempts));
            } catch (java.sql.SQLException e) {
                System.out.println("[FAILED] Could not reach the database. Reason: " + e.getMessage());
                return;
            }
        }

        System.out.println("\nToo many failed login attempts. Redirecting to User Registration...");
        userRegistrationFlow();
    }
}

