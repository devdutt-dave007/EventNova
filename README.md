# EventNova

**EventNova** is an inter-college event management and registration platform with two interfaces:

* **Modern Web Portal** — Node.js, Express, vanilla HTML/CSS/JavaScript
* **Java Console Application** — layered Java application using `Menus → Service → DAO`

Both interfaces communicate with a **MySQL/MariaDB** database where registration, payment, wallet, waitlist, and refund operations are handled through transactional database logic and stored procedures.

---

## Overview

EventNova manages the complete lifecycle of inter-college events:

* Institute event hosting
* Participant authentication
* Event discovery
* Eligibility validation
* Solo and team registration
* Wallet-based payments
* FIFO waitlisting
* Registration cancellation
* Refund processing
* Transaction and wallet ledgers
* Invoice and certificate generation

The project is designed around a strict separation of responsibilities between the presentation, business, data-access, and database layers.

---

## Architecture

```text
                         ┌─────────────────────────────┐
                         │        User Clients         │
                         │                             │
                         │  Web Browser   │ Java CLI   │
                         └───────┬─────────┴─────┬──────┘
                                 │               │
                                 ▼               ▼
                    ┌──────────────────┐   ┌──────────────────┐
                    │   Express API    │   │   Menus Layer    │
                    │  web/server.js   │   │    src/menus/    │
                    └────────┬─────────┘   └────────┬─────────┘
                             │                      │
                             │                      ▼
                             │             ┌──────────────────┐
                             │             │  Service Layer   │
                             │             │   src/service/   │
                             │             └────────┬─────────┘
                             │                      │
                             │                      ▼
                             │             ┌──────────────────┐
                             │             │    DAO Layer     │
                             │             │     src/dao/     │
                             │             └────────┬─────────┘
                             │                      │
                             └──────────┬───────────┘
                                        ▼
                         ┌─────────────────────────────┐
                         │      MySQL / MariaDB       │
                         │                             │
                         │  • Stored Procedures       │
                         │  • Transactions             │
                         │  • Wallet Ledger            │
                         │  • Registration State       │
                         │  • Constraints / Triggers   │
                         └─────────────────────────────┘
```

### Java Application Layers

| Layer           | Responsibility                                                        |
| --------------- | --------------------------------------------------------------------- |
| **Menus**       | Console UI, input handling, and navigation                            |
| **Service**     | Business rules and application logic                                  |
| **DAO**         | SQL/database access through JDBC                                      |
| **Model**       | Domain objects such as users, events, registrations, and transactions |
| **Database**    | JDBC connection management                                            |
| **Collections** | Custom data structures such as the FIFO waiting list                  |
| **Exception**   | Domain-specific exception handling                                    |
| **Util**        | Supporting utilities including PDF generation and input helpers       |

---

## Key Features

### Institute-Based Event Hosting

Only registered educational institutes can create and manage events.

Participants can:

* Browse available events
* Check eligibility
* Register individually or as teams
* Manage existing registrations

### Authentication

Separate authentication flows are provided for:

* Institutes
* Students
* Faculty
* External participants

A guest browsing mode allows users to explore events without signing in.

### Eligibility Validation

Events can define eligibility rules such as:

* `STUDENT_NATIVE`
* `FACULTY_NATIVE`
* `EXTERNAL`
* `ALL`

Eligibility is checked before registration.

### Solo & Team Registration

EventNova supports both individual and team registrations.

Team registration records:

* Team leader
* Team members
* Participant details
* Shared registration/team identifier

### FIFO Waiting List

When an event reaches capacity, additional participants are placed into a custom FIFO waiting list.

The Java application implements the waiting list using a custom linked-list data structure.

### Wallet & Payment System

Participants use an in-app wallet for event payments.

The system tracks balance-changing operations through a wallet transaction ledger, including:

* Welcome bonuses
* Wallet recharges
* Event payment debits
* Institute payment credits
* Refunds

### Transactional Financial Operations

Financial and registration operations are handled through MySQL stored procedures and database transactions to maintain consistent balances and registration states.

### Cancellation & Refunds

Two cancellation flows are supported:

**Participant cancellation**

* Registration is cancelled
* A 50% refund is returned to the participant
* The remaining amount is retained as a cancellation/processing fee

**Institute event cancellation**

* Confirmed registrations receive full refunds
* Waiting-list entries are cleared
* Corresponding financial transactions are recorded

### Registration Reactivation

Previously cancelled registrations can be reactivated where applicable instead of creating an entirely new registration.

### PDF Generation

EventNova includes custom Java-based PDF generation for:

* Admission passes
* Invoices
* Participation certificates

The PDF writer operates at the raw byte level without relying on an external PDF library.

---

## Technology Stack

### Web Application

* **Frontend:** HTML5, CSS3, Vanilla JavaScript
* **Backend:** Node.js
* **API:** Express.js
* **Database Driver:** `mysql2`
* **Database:** MySQL / MariaDB

### Console Application

* **Language:** Java 17+
* **Database Access:** JDBC
* **Architecture:** Menus → Service → DAO
* **Data Structures:** Custom linked-list implementation
* **PDF Generation:** Custom Java implementation

### Database

* MySQL 5.5+ / MariaDB 10.4+
* InnoDB
* Stored procedures
* Transactions
* Foreign keys
* Constraints
* Triggers

---

## Project Structure

```text
EventNova/
│
├── README.md
├── .gitignore
│
├── start_web.bat
├── start_cli.bat
│
├── sql/
│   └── eventnova_schema.sql
│
├── src/
│   ├── main/
│   │   └── Main.java
│   │
│   ├── menus/
│   │   ├── MainMenu.java
│   │   ├── UserMenu.java
│   │   ├── InstituteMenu.java
│   │   ├── EventSearchMenu.java
│   │   └── NavigationStack.java
│   │
│   ├── service/
│   │   ├── EventService.java
│   │   ├── InstituteService.java
│   │   ├── RegistrationService.java
│   │   ├── TransactionService.java
│   │   └── UserService.java
│   │
│   ├── dao/
│   │   ├── EventDAO.java
│   │   ├── InstituteDAO.java
│   │   ├── RegistrationDAO.java
│   │   ├── TransactionDAO.java
│   │   └── UserDAO.java
│   │
│   ├── model/
│   │   ├── Event.java
│   │   ├── Institute.java
│   │   ├── Registration.java
│   │   ├── Transaction.java
│   │   ├── User.java
│   │   └── ...
│   │
│   ├── database/
│   │   └── DBConnection.java
│   │
│   ├── collections/
│   │   ├── CustomLinkedList.java
│   │   ├── EventCache.java
│   │   ├── RecentEventTracker.java
│   │   └── WaitingListManager.java
│   │
│   ├── exception/
│   │   └── ...
│   │
│   └── util/
│       ├── ConsoleInput.java
│       ├── FileExporter.java
│       ├── InvoiceGenerator.java
│       └── Logger.java
│
└── web/
    ├── package.json
    ├── package-lock.json
    ├── server.js
    ├── .env.example
    │
    └── public/
        ├── index.html
        ├── style.css
        └── app.js
```

---

## Database

The default database is:

```text
eventnova3
```

The schema contains entities for:

* Institutes
* Users
* Events
* Registrations
* Team Members
* Transactions
* Wallet Transactions

### Important Stored Procedures

The database contains stored procedures for operations such as:

```text
sp_register_and_pay
sp_register_free
sp_recharge_wallet
sp_cancel_registration_by_user
sp_cancel_event_by_institute
```

These procedures centralize important state-changing operations and help maintain transactional consistency.

---

## Setup

### 1. Clone the Repository

```bash
git clone https://github.com/devdutt-dave007/EventNova.git
cd EventNova
```

### 2. Database Setup

Start a local MySQL/MariaDB server.

Import the schema:

```bash
mysql -u root -p < sql/eventnova_schema.sql
```

The schema creates the required database, tables, procedures, constraints, and seed data.

> **Warning:** The schema is intended for development/testing and may recreate the database when executed. Review the SQL file before running it against an existing database containing important data.

---

## Running the Web Portal

### Option A — Windows Launcher

Run:

```text
start_web.bat
```

### Option B — Manual

```bash
cd web
npm install
npm start
```

Then open:

```text
http://localhost:5000
```

Configure database credentials using the environment configuration described by `.env.example`.

---

## Running the Java Console Application

### Option A — Windows Launcher

Run:

```text
start_cli.bat
```

### Option B — Manual Compilation

On Linux/macOS:

```bash
javac -d out $(find src -name "*.java")
java -cp out main.Main
```

On Windows, use the included launcher or compile the Java source files using your preferred Java development environment.

---

## Database Design Principles

EventNova intentionally moves important financial and registration state transitions into the database layer.

The system uses:

* Stored procedures
* ACID transactions
* Foreign-key constraints
* Transactional wallet operations
* Ledger-based financial tracking
* Database-level validation

This keeps critical state changes consistent even when an operation involves multiple database updates.

---

## Academic Context

EventNova was developed as a **Semester 2 academic project** with a focus on:

* Java OOP
* Data structures
* JDBC
* MySQL
* Database design
* Stored procedures
* Transaction management
* Layered application architecture
* Web application integration

The project combines a traditional Java console architecture with a modern web interface while maintaining a shared database backend.
