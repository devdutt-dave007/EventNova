<<<<<<< HEAD
# EventNova 3.0

An inter-college event management and registration platform built with a dual interface:
1. **Modern Monochrome Web Portal** — Built with Node.js, Express, and a high-contrast dark/light monochrome UI connected live to MySQL on `http://localhost:5000`.
2. **Layered Java Console Application** — Classical CLI following a strict **Menus → Service → DAO → Stored Procedures** architecture.

Backed by **MySQL / MariaDB (`eventnova3`)** where all financial balance movements, waitlisting, and registration states are atomically enforced via stored procedures and double-entry transaction ledgers.

---
=======
# EventNova

A console-based inter-college event management portal built with **Java** and **MySQL**, following a layered **Menus → Service → DAO** architecture.

## Overview

EventNova handles end-to-end event registration and payment for inter-college events — from eligibility checks and team formation to wallet-based payments, refunds, and certificate generation — all through a console interface backed by a relational database.

Institutes host events; students, faculty, and external participants register, pay, and manage their registrations. All money movement (registrations, cancellations, refunds, wallet recharges) is handled through MySQL stored procedures wrapped in transactions, so balances stay consistent even if something fails mid-operation.
>>>>>>> e99efdfe90541f0d76454ca58978bb91dd786604

## Architecture

```
<<<<<<< HEAD
                               ┌──────────────────────────────────────────────┐
                               │                 User Clients                 │
                               │  Web Browser (UI)   │    Terminal (Java CLI) │
                               └──────────┬──────────┴───────────┬────────────┘
                                          │                      │
                                          ▼                      ▼
                               ┌─────────────────────┐┌───────────────────────┐
                               │   Express REST API  ││     Menus Layer       │
                               │    (web/server.js)  ││ (src/menus/MainMenu)  │
                               └──────────┬──────────┘└──────────┬────────────┘
                                          │                      ▼
                                          │           ┌───────────────────────┐
                                          │           │     Service Layer     │
                                          │           │  (src/service/*.java) │
                                          │           └──────────┬────────────┘
                                          │                      ▼
                                          │           ┌───────────────────────┐
                                          │           │       DAO Layer       │
                                          │           │    (src/dao/*.java)   │
                                          │           └──────────┬────────────┘
                                          │                      │
                                          ▼                      ▼
                               ┌──────────────────────────────────────────────┐
                               │           MySQL Database Engine              │
                               │             (Database: eventnova3)           │
                               │  ──────────────────────────────────────────  │
                               │  • Stored Procedures (Atomic Money Flow)     │
                               │  • Double-Entry Wallet Ledger                │
                               │  • InnoDB ACID Transactions & Foreign Keys   │
                               └──────────────────────────────────────────────┘
```

---

## Key Features

- **Strict Role Enforcement (Institute-Only Event Hosting)**:
  - Only registered **Educational Institutes** (e.g., LJ University, Nirma University) are authorized to host and organize campus events.
  - Normal users (**Students**, **Faculty**, **External**) are restricted to discovering events, forming teams, and managing registrations.
- **Dedicated Authentication**:
  - Distinct sign-in flows for Institutes and Participants.
  - Guest browsing mode allows exploring all campus events without requiring login.
  - New user registration automatically credits an instant **₹500 Welcome Bonus** to their in-app wallet, logged in the double-entry ledger.
- **Eligibility Validation**:
  - Dynamically enforces eligibility constraints: `STUDENT_NATIVE`, `FACULTY_NATIVE`, `EXTERNAL`, or `ALL`.
- **Solo & Team Registration**:
  - Captures leader and all teammate credentials (name, branch, roll/enrollment number) grouped under an auto-generated `TEAM-<Registration_ID>`.
- **Custom FIFO Waitlisting**:
  - Automatically places registrants on a FIFO waiting list once capacity is saturated.
- **Deterministic Refund Logic**:
  - **Self-Cancellation by User**: Automatically credits a **50% refund** back to the user's wallet, with 50% retained by the hosting institute as a processing fee (`sp_cancel_registration_by_user`).
  - **Event Cancellation by Institute**: Automatically issues **100% full refunds** to all confirmed ticket holders and clears the waiting list (`sp_cancel_event_by_institute`).
- **Double-Entry Financial Audit Trail**:
  - All balance-altering actions (`BONUS`, `RECHARGE`, `EVENT_PAYMENT_DEBIT`, `EVENT_PAYMENT_CREDIT`, `REFUND_CREDIT`, `REFUND_DEBIT`) are recorded in `Wallet_Transaction`.
- **Custom Raw-Byte PDF Invoices & Passes**:
  - Digital admission pass and invoice generation using pure Java byte-level PDF rendering without external PDF dependencies.

---

## Tech Stack

- **Web Frontend**: Vanilla HTML5, Modern CSS (Obsidian, charcoal, slate, and pure white design system), JavaScript (ES6+).
- **Web Backend**: Node.js, Express, `mysql2` connection pooling.
- **Console Application**: Java 17+ (layered OOP architecture, custom singly-linked waitlist).
- **Database**: MySQL 5.5+ / MariaDB 10.4+ with InnoDB engine, stored procedures, and triggers.

---
=======
Menus (User Interaction)
   │
   ▼
Service Layer (Business Logic)
   │
   ▼
DAO Layer (Database Access)
   │
   ▼
MySQL Database
```

- **Menus** — console I/O and user interaction
- **Service** — business rules (eligibility, payment logic, backdating checks)
- **DAO** — isolates all SQL/database access from business logic
- **Model** — data classes representing Institutes, Users, Events, Registrations, Transactions
- **Database** — connection handling
- **Exception** — custom exception types for domain-specific error handling
- **Collections / Util** — supporting data structures and helper utilities

## Key Features

- **Eligibility validation** — checks whether a participant/team qualifies for an event (native student, faculty, external, or open to all) before allowing registration
- **Team-aware registration** — collects credentials for every team member, not just the registrant; team registrations are grouped under a shared Team ID
- **Wallet & transaction system** — in-app wallet balance for both users and institutes, with a full ledger of every balance-affecting event (bonuses, payments, refunds, recharges), backed by MySQL stored procedures for every money-moving operation
- **Custom waiting list** — implemented with a hand-built singly linked list (no built-in collection used) for FIFO waitlist handling when an event fills up
- **Backdating prevention** — event dates are constrained to fall after the registration deadline at the database level
- **Cancellation & refunds** — users can self-cancel (50% refund, institute keeps the rest as a cancellation fee); institutes can cancel an entire event (full refund to all confirmed registrants, waitlisted entries simply cleared)
- **Reactivate registration** — allows a previously cancelled registration to be reinstated instead of forcing a fresh one
- **Certificate generation** — participation certificates generated as PDFs using raw PDF byte-level generation written in pure Java, with no external PDF libraries

## Tech Stack

- **Language:** Java
- **Database:** MySQL
- **Database logic:** Stored procedures, transactions with savepoints, cursors, and constraint-level validation for all financial and registration operations
- **PDF generation:** Custom raw-byte PDF writer (pure Java, no libraries)
>>>>>>> e99efdfe90541f0d76454ca58978bb91dd786604

## Project Structure

```
EventNova/
<<<<<<< HEAD
├── README.md                 # Project documentation
├── .gitignore                # Git ignore rules (node_modules, classes, logs)
├── start_web.bat             # One-click Windows launcher for Web Portal
├── start_cli.bat             # One-click Windows compiler & launcher for Java CLI
├── sql/
│   └── eventnova_schema.sql  # Database schema, procedures & seed data
├── src/                      # Java Console Application
│   ├── main/                 # Entry point (Main.java)
│   ├── menus/                # Terminal UI & navigation stack
│   ├── service/              # Business logic layer
│   ├── dao/                  # Database access layer
│   ├── model/                # Domain models (Event, Institute, User, etc.)
│   ├── database/             # JDBC connection handling
│   ├── collections/          # Custom data structures (CustomLinkedList, EventCache)
│   ├── exception/            # Domain-specific custom exceptions
│   └── util/                 # Helpers (raw PDF invoice generator, console input)
└── web/                      # Full-Stack Web Application
    ├── package.json          # Node.js dependencies (express, mysql2, cors)
    ├── server.js             # Express API server connecting to MySQL
    ├── .env.example          # Environment variable template
    └── public/               # Frontend single-page application
        ├── index.html        # Semantic HTML layout & dialogs
        ├── style.css         # Luxury monochrome aesthetic design system
        └── app.js            # Client-side controller & state management
```

---

## Setup & Running

### 1. Database Setup
1. Start your local MySQL / MariaDB server (e.g. via XAMPP on port `3306`).
2. Import the schema script into MySQL:
   ```bash
   mysql -u root -p < sql/eventnova_schema.sql
   ```
   *(Creates the `eventnova3` database, tables, stored procedures, and default demo records)*.

---

### 2. Running the Web Portal (Recommended)

#### Option A: One-Click Launcher (Windows)
Double-click **`start_web.bat`**.

#### Option B: Terminal Command
```bash
cd web
npm install
npm start
```

Open your browser and visit:
👉 **`http://localhost:5000`**

---

### 3. Running the Java Console Application

#### Option A: One-Click Launcher (Windows)
Double-click **`start_cli.bat`**.

#### Option B: Manual Compilation
```bash
javac -d out $(find src -name "*.java")
java -cp out main.Main
```

---

## Demo Test Credentials

The database comes pre-seeded with test accounts for immediate verification:

### Institute Accounts (Can host events & process refunds)
| Institute Name | Email | Password |
| :--- | :--- | :--- |
| **LJ University** | `admin@lju.edu.in` | `Lj@2026Secure` |
| **Nirma University** | `admin@nirma.edu.in` | `Nirma@2026Pass` |
| **Gujarat University** | `admin@gu.edu.in` | `Gu@2026Secure` |

### Participant Accounts (Cannot host events; can register & form teams)
| Name | Role | Email | Password | Starting Wallet |
| :--- | :--- | :--- | :--- | :--- |
| **Devdutt Sharma** | Student @ LJ | `devdutt@lju.edu.in` | `Devdutt@123` | ₹5,500.00 |
| **Priya Mehta** | Student @ Nirma | `priya@nirma.edu.in` | `Priya@123` | ₹3,500.00 |
| **Dr. Anil Kumar** | Faculty @ LJ | `anil.kumar@lju.edu.in` | `Anil@1234` | ₹10,500.00 |
| **Rahul Shah** | External Participant | `rahul.shah@gmail.com` | `Rahul@123` | ₹2,500.00 |

*(The web interface also provides one-click demo credential chips for instant sign-in)*.
=======
├── README.md
├── src/
│   ├── main/          # Application entry point
│   ├── menus/         # Console UI / user interaction
│   ├── service/       # Business logic layer
│   ├── dao/           # Database access layer
│   ├── model/         # Data classes (Institute, User, Event, Registration, Transaction...)
│   ├── database/      # Database connection handling
│   ├── collections/   # Custom data structures (e.g. waiting list)
│   ├── exception/     # Custom exception types
│   └── util/          # Helper utilities
└── sql/
    └── eventnova_schema.sql  # Full schema: tables, stored procedures, seed data
```

## Database Schema

The database (`eventnova3`) consists of:

- **Institute** — hosts events, holds a running balance from ticket sales
- **User** — students, faculty, or external participants, each with a wallet balance
- **Event** — hosted by an institute, with category, eligibility, capacity, and pricing
- **Registration** — one row per registrant, tracks status (confirmed/waitlisted/cancelled) and team grouping
- **Team_Member** — additional team members under a team registration
- **Transaction** — event-payment ledger (drives invoices)
- **Wallet_Transaction** — complete ledger of every balance-affecting event, for both users and institutes

Stored procedures handle all state-changing operations: `sp_register_and_pay`, `sp_register_free`, `sp_recharge_wallet`, `sp_cancel_registration_by_user`, and `sp_cancel_event_by_institute`.

## Setup

1. **Database setup**
   - Create/import the schema by running `sql/eventnova_schema.sql` in MySQL — it creates the `eventnova3` database, all tables, procedures, and seed data in one go
   - (The script starts with `DROP DATABASE IF EXISTS eventnova3`, so it's safe to re-run for a clean reset during development)

2. **Configure connection**
   - Update the database connection details (host, username, password) in the `database` package to match your local MySQL setup

3. **Compile and run**
   ```bash
   javac -d out $(find src -name "*.java")
   java -cp out main.Main
   ```
   (Adjust the entry-point class name to match your actual `main` package)

## Notes

Built as a Semester 2 academic project, focused on layered application design and moving all financial/transactional logic into the database layer via stored procedures rather than handling it in application code.
>>>>>>> e99efdfe90541f0d76454ca58978bb91dd786604
