-- ============================================
-- EventNova 3.0 Database Schema
-- Database: eventnova3
-- Inter-college event management system
-- ============================================

DROP DATABASE IF EXISTS eventnova3;
CREATE DATABASE eventnova3;
USE eventnova3;

-- ============================================
-- TABLES
-- ============================================

-- Institute: hosts events, holds a running balance from ticket sales
CREATE TABLE Institute (
    Institute_ID INT AUTO_INCREMENT PRIMARY KEY,
    Institute_Name VARCHAR(150) NOT NULL,
    Email VARCHAR(100) NOT NULL UNIQUE,
    Password VARCHAR(255) NOT NULL,
    Address VARCHAR(255) NOT NULL,
    Balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    Joining_Date DATE NOT NULL DEFAULT (CURRENT_DATE),
    CONSTRAINT chk_institute_email CHECK (Email LIKE '%.edu.in'),
    CONSTRAINT chk_institute_balance CHECK (Balance >= 0)
) ENGINE=InnoDB;

-- User: students, faculty, or external participants
CREATE TABLE User (
    User_ID INT AUTO_INCREMENT PRIMARY KEY,
    Name VARCHAR(100) NOT NULL,
    Email VARCHAR(100) NOT NULL UNIQUE,
    Password VARCHAR(255) NOT NULL,
    Phone VARCHAR(15) NOT NULL,
    Role ENUM('STUDENT', 'FACULTY', 'EXTERNAL') NOT NULL,
    Institute_ID INT NULL,
    Enrollment_No VARCHAR(50) NULL,
    Employee_ID VARCHAR(50) NULL,
    Balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_user_institute FOREIGN KEY (Institute_ID)
        REFERENCES Institute(Institute_ID)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT chk_user_balance CHECK (Balance >= 0)
) ENGINE=InnoDB;

-- Event: hosted by an institute, fixed category/eligibility enums
CREATE TABLE Event (
    Event_ID INT AUTO_INCREMENT PRIMARY KEY,
    Institute_ID INT NOT NULL,
    Event_Name VARCHAR(150) NOT NULL,
    Category ENUM('FEST', 'MUSIC', 'DANCE', 'HACKATHON', 'SEMINAR',
                   'TECH_FEST', 'DRAMA', 'SPORTS') NOT NULL,
    Eligibility ENUM('STUDENT_NATIVE', 'FACULTY_NATIVE', 'EXTERNAL', 'ALL') NOT NULL,
    Participation_Type ENUM('SOLO', 'TEAM') NOT NULL,
    Description TEXT NOT NULL,
    Venue VARCHAR(150) NOT NULL,
    Capacity INT NOT NULL,
    Ticket_Price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    Event_Date DATE NOT NULL,
    Start_Time TIME NOT NULL,
    End_Time TIME NOT NULL,
    Registration_Deadline DATETIME NOT NULL,
    Status ENUM('OPEN', 'CLOSED', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'OPEN',
    CONSTRAINT fk_event_institute FOREIGN KEY (Institute_ID)
        REFERENCES Institute(Institute_ID)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT chk_event_capacity CHECK (Capacity > 0),
    CONSTRAINT chk_event_price CHECK (Ticket_Price >= 0),
    CONSTRAINT chk_event_date_after_deadline CHECK (Event_Date > DATE(Registration_Deadline)),
    INDEX idx_event_category (Category),
    INDEX idx_event_date (Event_Date),
    INDEX idx_event_status (Status),
    INDEX idx_event_name (Event_Name)
) ENGINE=InnoDB;

-- Registration: one row per registrant. Team_ID groups team registrations
-- (auto-generated as TEAM-<Registration_ID> when the registration is a team entry).
CREATE TABLE Registration (
    Registration_ID INT AUTO_INCREMENT PRIMARY KEY,
    User_ID INT NOT NULL,
    Event_ID INT NOT NULL,
    Registration_Date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    Registration_Status ENUM('CONFIRMED', 'WAITLISTED', 'CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
    Team_ID VARCHAR(50) NULL,
    CONSTRAINT fk_registration_user FOREIGN KEY (User_ID)
        REFERENCES User(User_ID)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_registration_event FOREIGN KEY (Event_ID)
        REFERENCES Event(Event_ID)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT uq_user_event UNIQUE (User_ID, Event_ID),
    INDEX idx_registration_status (Registration_Status)
) ENGINE=InnoDB;

-- Team_Member: additional members under a team registration (leader is the Registration row itself)
CREATE TABLE Team_Member (
    Team_Member_ID INT AUTO_INCREMENT PRIMARY KEY,
    Registration_ID INT NOT NULL,
    Name VARCHAR(100) NOT NULL,
    Branch VARCHAR(100) NOT NULL,
    Enrollment_No VARCHAR(50) NOT NULL,
    CONSTRAINT fk_teammember_registration FOREIGN KEY (Registration_ID)
        REFERENCES Registration(Registration_ID)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    INDEX idx_teammember_registration (Registration_ID)
) ENGINE=InnoDB;

-- Transaction: event-payment ledger only (one row per registration, drives invoices)
CREATE TABLE Transaction (
    Transaction_ID INT AUTO_INCREMENT PRIMARY KEY,
    Registration_ID INT NOT NULL UNIQUE,
    User_ID INT NOT NULL,
    Institute_ID INT NOT NULL,
    Amount DECIMAL(10,2) NOT NULL,
    Payment_Mode ENUM('CASH', 'UPI', 'CARD', 'NET_BANKING', 'FREE') NOT NULL,
    Transaction_Status ENUM('SUCCESS', 'FAILED', 'PENDING', 'REFUNDED') NOT NULL DEFAULT 'SUCCESS',
    Transaction_Date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_registration FOREIGN KEY (Registration_ID)
        REFERENCES Registration(Registration_ID)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_transaction_user FOREIGN KEY (User_ID)
        REFERENCES User(User_ID)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_transaction_institute FOREIGN KEY (Institute_ID)
        REFERENCES Institute(Institute_ID)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT chk_transaction_amount CHECK (Amount >= 0),
    INDEX idx_transaction_status (Transaction_Status)
) ENGINE=InnoDB;

-- Wallet_Transaction: complete ledger of every balance-affecting event, for
-- both Users and Institutes (Owner_Type + Owner_ID identifies whose wallet).
-- Kept separate from Transaction, which is purely event-payment/invoice records.
CREATE TABLE Wallet_Transaction (
    Wallet_Txn_ID INT AUTO_INCREMENT PRIMARY KEY,
    Owner_Type ENUM('USER', 'INSTITUTE') NOT NULL,
    Owner_ID INT NOT NULL,
    Entry_Type ENUM('BONUS', 'RECHARGE', 'EVENT_PAYMENT_DEBIT', 'EVENT_PAYMENT_CREDIT',
                     'REFUND_CREDIT', 'REFUND_DEBIT') NOT NULL,
    Amount DECIMAL(12,2) NOT NULL,
    Balance_After DECIMAL(12,2) NOT NULL,
    Registration_ID INT NULL,
    Description VARCHAR(255) NOT NULL,
    Entry_Date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_registration FOREIGN KEY (Registration_ID)
        REFERENCES Registration(Registration_ID)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT chk_wallet_amount CHECK (Amount >= 0),
    INDEX idx_wallet_owner (Owner_Type, Owner_ID),
    INDEX idx_wallet_entry_type (Entry_Type)
) ENGINE=InnoDB;

-- ============================================
-- STORED PROCEDURES
-- (final versions only — all money movement is wrapped in transactions
--  with rollback-on-error handlers)
-- ============================================

DELIMITER $$

-- sp_register_and_pay
-- Registers a user for a PAID event and moves wallet money atomically.
-- p_amount is the TOTAL amount due (per-head price * team size for team events).
-- p_is_team = TRUE assigns a Team_ID of the form TEAM-<Registration_ID>.
-- CASH payments are recorded as PENDING with no wallet movement (settled offline).
CREATE PROCEDURE sp_register_and_pay (
    IN p_user_id INT,
    IN p_event_id INT,
    IN p_institute_id INT,
    IN p_amount DECIMAL(10,2),
    IN p_payment_mode VARCHAR(20),
    IN p_is_team BOOLEAN,
    IN p_is_cash BOOLEAN,
    OUT p_registration_id INT,
    OUT p_transaction_id INT,
    OUT p_status VARCHAR(20),
    OUT p_message VARCHAR(255)
)
proc_body: BEGIN
    DECLARE v_current_balance DECIMAL(12,2);
    DECLARE v_new_user_balance DECIMAL(12,2);
    DECLARE v_new_inst_balance DECIMAL(12,2);
    DECLARE v_txn_status VARCHAR(20);

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_status = 'FAILED';
        SET p_message = 'Database error during registration/payment.';
    END;

    START TRANSACTION;

    IF EXISTS (SELECT 1 FROM Registration
               WHERE User_ID = p_user_id AND Event_ID = p_event_id
               AND Registration_Status != 'CANCELLED') THEN
        SET p_status = 'FAILED';
        SET p_message = 'User already registered for this event.';
        ROLLBACK;
        LEAVE proc_body;
    END IF;

    IF p_is_cash = FALSE AND p_amount > 0 THEN
        SELECT Balance INTO v_current_balance FROM User WHERE User_ID = p_user_id FOR UPDATE;

        IF v_current_balance < p_amount THEN
            SET p_status = 'FAILED';
            SET p_message = CONCAT('Insufficient balance. Current: Rs.', v_current_balance,
                                    ', Required: Rs.', p_amount);
            ROLLBACK;
            LEAVE proc_body;
        END IF;

        UPDATE User SET Balance = Balance - p_amount WHERE User_ID = p_user_id;
        UPDATE Institute SET Balance = Balance + p_amount WHERE Institute_ID = p_institute_id;

        SELECT Balance INTO v_new_user_balance FROM User WHERE User_ID = p_user_id;
        SELECT Balance INTO v_new_inst_balance FROM Institute WHERE Institute_ID = p_institute_id;

        INSERT INTO Wallet_Transaction (Owner_Type, Owner_ID, Entry_Type, Amount, Balance_After, Description)
        VALUES ('USER', p_user_id, 'EVENT_PAYMENT_DEBIT', p_amount, v_new_user_balance, 'Event registration payment');

        INSERT INTO Wallet_Transaction (Owner_Type, Owner_ID, Entry_Type, Amount, Balance_After, Description)
        VALUES ('INSTITUTE', p_institute_id, 'EVENT_PAYMENT_CREDIT', p_amount, v_new_inst_balance, 'Event registration received');
    END IF;

    INSERT INTO Registration (User_ID, Event_ID, Registration_Status, Team_ID)
    VALUES (p_user_id, p_event_id, 'CONFIRMED', NULL);
    SET p_registration_id = LAST_INSERT_ID();

    IF p_is_team THEN
        UPDATE Registration SET Team_ID = CONCAT('TEAM-', p_registration_id)
        WHERE Registration_ID = p_registration_id;
    END IF;

    SET v_txn_status = IF(p_is_cash, 'PENDING', 'SUCCESS');

    INSERT INTO Transaction (Registration_ID, User_ID, Institute_ID, Amount, Payment_Mode, Transaction_Status)
    VALUES (p_registration_id, p_user_id, p_institute_id, p_amount, p_payment_mode, v_txn_status);
    SET p_transaction_id = LAST_INSERT_ID();

    COMMIT;
    SET p_status = 'SUCCESS';
    SET p_message = 'Registration and payment completed successfully.';
END$$

-- sp_register_free
-- Registers a user for a FREE event — no wallet/Transaction money movement,
-- but still logs a Rs.0 Transaction row for consistent invoice/history behavior.
CREATE PROCEDURE sp_register_free (
    IN p_user_id INT,
    IN p_event_id INT,
    IN p_institute_id INT,
    IN p_is_team BOOLEAN,
    OUT p_registration_id INT,
    OUT p_transaction_id INT,
    OUT p_status VARCHAR(20),
    OUT p_message VARCHAR(255)
)
proc_body: BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_status = 'FAILED';
        SET p_message = 'Database error during free registration.';
    END;

    START TRANSACTION;

    IF EXISTS (SELECT 1 FROM Registration
               WHERE User_ID = p_user_id AND Event_ID = p_event_id
               AND Registration_Status != 'CANCELLED') THEN
        SET p_status = 'FAILED';
        SET p_message = 'User already registered for this event.';
        ROLLBACK;
        LEAVE proc_body;
    END IF;

    INSERT INTO Registration (User_ID, Event_ID, Registration_Status, Team_ID)
    VALUES (p_user_id, p_event_id, 'CONFIRMED', NULL);
    SET p_registration_id = LAST_INSERT_ID();

    IF p_is_team THEN
        UPDATE Registration SET Team_ID = CONCAT('TEAM-', p_registration_id)
        WHERE Registration_ID = p_registration_id;
    END IF;

    INSERT INTO Transaction (Registration_ID, User_ID, Institute_ID, Amount, Payment_Mode, Transaction_Status)
    VALUES (p_registration_id, p_user_id, p_institute_id, 0.00, 'FREE', 'SUCCESS');
    SET p_transaction_id = LAST_INSERT_ID();

    COMMIT;
    SET p_status = 'SUCCESS';
    SET p_message = 'Free registration completed successfully.';
END$$

-- sp_recharge_wallet
-- Simulated wallet top-up (no real payment gateway — future scope).
CREATE PROCEDURE sp_recharge_wallet (
    IN p_user_id INT,
    IN p_amount DECIMAL(12,2),
    OUT p_status VARCHAR(20),
    OUT p_message VARCHAR(255)
)
proc_body: BEGIN
    DECLARE v_new_balance DECIMAL(12,2);

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_status = 'FAILED';
        SET p_message = 'Database error during recharge.';
    END;

    START TRANSACTION;

    UPDATE User SET Balance = Balance + p_amount WHERE User_ID = p_user_id;
    SELECT Balance INTO v_new_balance FROM User WHERE User_ID = p_user_id;

    INSERT INTO Wallet_Transaction (Owner_Type, Owner_ID, Entry_Type, Amount, Balance_After, Description)
    VALUES ('USER', p_user_id, 'RECHARGE', p_amount, v_new_balance, 'Wallet recharge (simulated)');

    COMMIT;
    SET p_status = 'SUCCESS';
    SET p_message = 'Wallet recharged successfully.';
END$$

-- sp_cancel_registration_by_user
-- 50% refund to the user, institute keeps 50% as a cancellation fee.
-- Free events / cash-pending registrations: no money movement, just status change.
CREATE PROCEDURE sp_cancel_registration_by_user (
    IN p_registration_id INT,
    OUT p_status VARCHAR(20),
    OUT p_message VARCHAR(255),
    OUT p_refund_amount DECIMAL(10,2)
)
proc_body: BEGIN
    DECLARE v_user_id INT;
    DECLARE v_event_id INT;
    DECLARE v_institute_id INT;
    DECLARE v_amount DECIMAL(10,2);
    DECLARE v_payment_mode VARCHAR(20);
    DECLARE v_txn_status VARCHAR(20);
    DECLARE v_new_user_balance DECIMAL(12,2);
    DECLARE v_new_inst_balance DECIMAL(12,2);

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_status = 'FAILED';
        SET p_message = 'Database error during cancellation.';
    END;

    START TRANSACTION;

    SELECT User_ID, Event_ID INTO v_user_id, v_event_id
    FROM Registration WHERE Registration_ID = p_registration_id AND Registration_Status = 'CONFIRMED';

    IF v_user_id IS NULL THEN
        SET p_status = 'FAILED';
        SET p_message = 'Registration not found or already cancelled.';
        ROLLBACK;
        LEAVE proc_body;
    END IF;

    SELECT Institute_ID INTO v_institute_id FROM Event WHERE Event_ID = v_event_id;
    SELECT Amount, Payment_Mode, Transaction_Status INTO v_amount, v_payment_mode, v_txn_status
    FROM Transaction WHERE Registration_ID = p_registration_id;

    UPDATE Registration SET Registration_Status = 'CANCELLED' WHERE Registration_ID = p_registration_id;

    -- Only refund if money actually moved (SUCCESS, non-cash, non-zero)
    IF v_txn_status = 'SUCCESS' AND v_amount > 0 THEN
        SET p_refund_amount = ROUND(v_amount * 0.5, 2);

        UPDATE User SET Balance = Balance + p_refund_amount WHERE User_ID = v_user_id;
        UPDATE Institute SET Balance = Balance - p_refund_amount WHERE Institute_ID = v_institute_id;

        SELECT Balance INTO v_new_user_balance FROM User WHERE User_ID = v_user_id;
        SELECT Balance INTO v_new_inst_balance FROM Institute WHERE Institute_ID = v_institute_id;

        INSERT INTO Wallet_Transaction (Owner_Type, Owner_ID, Entry_Type, Amount, Balance_After, Registration_ID, Description)
        VALUES ('USER', v_user_id, 'REFUND_CREDIT', p_refund_amount, v_new_user_balance, p_registration_id, '50% refund on self-cancellation');

        INSERT INTO Wallet_Transaction (Owner_Type, Owner_ID, Entry_Type, Amount, Balance_After, Registration_ID, Description)
        VALUES ('INSTITUTE', v_institute_id, 'REFUND_DEBIT', p_refund_amount, v_new_inst_balance, p_registration_id, '50% refund issued to user');

        UPDATE Transaction SET Transaction_Status = 'REFUNDED' WHERE Registration_ID = p_registration_id;
    ELSE
        -- Free event or cash/pending — no money movement, refund amount is 0
        SET p_refund_amount = 0.00;
        IF v_txn_status = 'SUCCESS' THEN
            UPDATE Transaction SET Transaction_Status = 'REFUNDED' WHERE Registration_ID = p_registration_id;
        END IF;
    END IF;

    COMMIT;
    SET p_status = 'SUCCESS';
    SET p_message = 'Registration cancelled.';
END$$

-- sp_cancel_event_by_institute
-- Full refund to ALL confirmed users. Waitlisted users are simply cancelled
-- with no transaction (they never paid). Validates the event exists and
-- isn't already cancelled before doing anything.
CREATE PROCEDURE sp_cancel_event_by_institute (
    IN p_event_id INT,
    OUT p_status VARCHAR(20),
    OUT p_message VARCHAR(255),
    OUT p_refunded_count INT
)
proc_body: BEGIN
    DECLARE v_done INT DEFAULT FALSE;
    DECLARE v_reg_id INT;
    DECLARE v_user_id INT;
    DECLARE v_institute_id INT;
    DECLARE v_amount DECIMAL(10,2);
    DECLARE v_txn_status VARCHAR(20);
    DECLARE v_new_user_balance DECIMAL(12,2);
    DECLARE v_new_inst_balance DECIMAL(12,2);

    DECLARE cur CURSOR FOR
        SELECT r.Registration_ID, r.User_ID, t.Amount, t.Transaction_Status
        FROM Registration r
        JOIN Transaction t ON r.Registration_ID = t.Registration_ID
        WHERE r.Event_ID = p_event_id AND r.Registration_Status = 'CONFIRMED';

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = TRUE;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_status = 'FAILED';
        SET p_message = 'Database error during event cancellation.';
    END;

    START TRANSACTION;

    IF NOT EXISTS (SELECT 1 FROM Event WHERE Event_ID = p_event_id) THEN
        SET p_status = 'FAILED';
        SET p_message = 'Event not found. It may have been deleted.';
        ROLLBACK;
        LEAVE proc_body;
    END IF;

    IF (SELECT Status FROM Event WHERE Event_ID = p_event_id) = 'CANCELLED' THEN
        SET p_status = 'FAILED';
        SET p_message = 'Event is already cancelled.';
        ROLLBACK;
        LEAVE proc_body;
    END IF;

    SELECT Institute_ID INTO v_institute_id FROM Event WHERE Event_ID = p_event_id;
    SET p_refunded_count = 0;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO v_reg_id, v_user_id, v_amount, v_txn_status;
        IF v_done THEN
            LEAVE read_loop;
        END IF;

        IF v_txn_status = 'SUCCESS' AND v_amount > 0 THEN
            UPDATE User SET Balance = Balance + v_amount WHERE User_ID = v_user_id;
            UPDATE Institute SET Balance = Balance - v_amount WHERE Institute_ID = v_institute_id;

            SELECT Balance INTO v_new_user_balance FROM User WHERE User_ID = v_user_id;
            SELECT Balance INTO v_new_inst_balance FROM Institute WHERE Institute_ID = v_institute_id;

            INSERT INTO Wallet_Transaction (Owner_Type, Owner_ID, Entry_Type, Amount, Balance_After, Registration_ID, Description)
            VALUES ('USER', v_user_id, 'REFUND_CREDIT', v_amount, v_new_user_balance, v_reg_id, 'Full refund - event cancelled by institute');

            INSERT INTO Wallet_Transaction (Owner_Type, Owner_ID, Entry_Type, Amount, Balance_After, Registration_ID, Description)
            VALUES ('INSTITUTE', v_institute_id, 'REFUND_DEBIT', v_amount, v_new_inst_balance, v_reg_id, 'Full refund issued - event cancelled');

            UPDATE Transaction SET Transaction_Status = 'REFUNDED' WHERE Registration_ID = v_reg_id;
            SET p_refunded_count = p_refunded_count + 1;
        ELSEIF v_txn_status = 'SUCCESS' THEN
            -- Free event registration: log Rs.0 refund for record-keeping
            UPDATE Transaction SET Transaction_Status = 'REFUNDED' WHERE Registration_ID = v_reg_id;
        END IF;

        UPDATE Registration SET Registration_Status = 'CANCELLED' WHERE Registration_ID = v_reg_id;
    END LOOP;
    CLOSE cur;

    -- Waitlisted registrations: simply cancelled, no transaction ever existed
    UPDATE Registration SET Registration_Status = 'CANCELLED'
    WHERE Event_ID = p_event_id AND Registration_Status = 'WAITLISTED';

    UPDATE Event SET Status = 'CANCELLED' WHERE Event_ID = p_event_id;

    COMMIT;
    SET p_status = 'SUCCESS';
    SET p_message = 'Event cancelled and refunds processed.';
END$$

DELIMITER ;

-- ============================================
-- SEED DATA
-- Passwords are plaintext for demo/viva purposes only — not production practice.
-- ============================================

INSERT INTO Institute (Institute_Name, Email, Password, Address, Balance, Joining_Date) VALUES
('LJ University', 'admin@lju.edu.in', 'Lj@2026Secure', 'Sarkhej-Sanand Road, Ahmedabad', 0.00, '2026-01-10'),
('Nirma University', 'admin@nirma.edu.in', 'Nirma@2026Pass', 'Sarkhej-Gandhinagar Highway, Ahmedabad', 0.00, '2026-01-15'),
('Gujarat University', 'admin@gu.edu.in', 'Gu@2026Secure', 'Navrangpura, Ahmedabad', 0.00, '2026-01-20');

-- Users get a Rs.500 welcome bonus, reflected directly in seed Balance
INSERT INTO User (Name, Email, Password, Phone, Role, Institute_ID, Enrollment_No, Employee_ID, Balance) VALUES
('Devdutt Sharma', 'devdutt@lju.edu.in', 'Devdutt@123', '9876543210', 'STUDENT', 1, 'LJ2026IT001', NULL, 5500.00),
('Priya Mehta', 'priya@nirma.edu.in', 'Priya@123', '9876543211', 'STUDENT', 2, 'NR2026CS045', NULL, 3500.00),
('Dr. Anil Kumar', 'anil.kumar@lju.edu.in', 'Anil@1234', '9876543212', 'FACULTY', 1, NULL, 'LJEMP0021', 10500.00),
('Rahul Shah', 'rahul.shah@gmail.com', 'Rahul@123', '9876543213', 'EXTERNAL', NULL, NULL, NULL, 2500.00);

-- Corresponding welcome bonus ledger entries
INSERT INTO Wallet_Transaction (Owner_Type, Owner_ID, Entry_Type, Amount, Balance_After, Description) VALUES
('USER', 1, 'BONUS', 500.00, 5500.00, 'Welcome bonus on registration'),
('USER', 2, 'BONUS', 500.00, 3500.00, 'Welcome bonus on registration'),
('USER', 3, 'BONUS', 500.00, 10500.00, 'Welcome bonus on registration'),
('USER', 4, 'BONUS', 500.00, 2500.00, 'Welcome bonus on registration');

INSERT INTO Event (Institute_ID, Event_Name, Category, Eligibility, Participation_Type,
    Description, Venue, Capacity, Ticket_Price, Event_Date, Start_Time, End_Time,
    Registration_Deadline, Status) VALUES
(1, 'CodeStorm Hackathon', 'HACKATHON', 'STUDENT_NATIVE', 'TEAM',
 '24-hour hackathon on full-stack development', 'LJ Auditorium', 3, 0.00,
 '2026-08-15', '09:00:00', '21:00:00', '2026-08-10 23:59:59', 'OPEN'),

(2, 'AI Symposium 2026', 'SEMINAR', 'ALL', 'SOLO',
 'A symposium on modern AI trends and applications', 'Nirma Convention Hall', 2, 199.00,
 '2026-09-05', '10:00:00', '16:00:00', '2026-09-01 23:59:59', 'OPEN'),

(1, 'TechNova Fest', 'TECH_FEST', 'STUDENT_NATIVE', 'SOLO',
 'Annual technical exhibition and showcase', 'LJ Seminar Hall', 5, 0.00,
 '2026-08-20', '11:00:00', '15:00:00', '2026-08-18 23:59:59', 'OPEN'),

(3, 'Inter-College Sports Meet', 'SPORTS', 'EXTERNAL', 'TEAM',
 'Open sports meet for students of other institutes', 'GU Stadium', 8, 50.00,
 '2026-09-12', '08:00:00', '18:00:00', '2026-09-08 23:59:59', 'OPEN');

-- ============================================
-- VERIFICATION QUERIES (optional — run manually to inspect seed data)
-- ============================================
-- SELECT * FROM Institute;
-- SELECT * FROM User;
-- SELECT * FROM Event;
-- SELECT * FROM Wallet_Transaction;
