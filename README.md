# EventNova

A console-based inter-college event management portal built with **Java** and **MySQL**, following a layered **Menus → Service → DAO** architecture.

## Overview

EventNova handles end-to-end event registration and payment for inter-college events — from eligibility checks and team formation to wallet-based payments, refunds, and certificate generation — all through a console interface backed by a relational database.

Institutes host events; students, faculty, and external participants register, pay, and manage their registrations. All money movement (registrations, cancellations, refunds, wallet recharges) is handled through MySQL stored procedures wrapped in transactions, so balances stay consistent even if something fails mid-operation.

## Architecture

```
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

## Project Structure

```
EventNova/
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
