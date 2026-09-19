# EventNova 3.0

An inter-college event management and registration platform built with a dual interface:
1. **Modern Monochrome Web Portal** — Built with Node.js, Express, and a high-contrast dark/light monochrome UI connected live to MySQL on `http://localhost:5000`.
2. **Layered Java Console Application** — Classical CLI following a strict **Menus → Service → DAO → Stored Procedures** architecture.

Backed by **MySQL / MariaDB (`eventnova3`)** where all financial balance movements, waitlisting, and registration states are atomically enforced via stored procedures and double-entry transaction ledgers.

---

## Architecture

```
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

## Project Structure

```
EventNova/
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
