# \# EventNova

# 

# A console-based inter-college event management portal built with \*\*Java\*\* and \*\*MySQL\*\*, following a layered \*\*Menus → Service → DAO\*\* architecture.

# 

# \## Overview

# 

# EventNova handles end-to-end event registration and payment for inter-college events — from eligibility checks and team formation to wallet-based payments and certificate generation — all through a console interface backed by a relational database.

# 

# \## Architecture

# 

# ```

# Menus (User Interaction)

# &#x20;  │

# &#x20;  ▼

# Service Layer (Business Logic)

# &#x20;  │

# &#x20;  ▼

# DAO Layer (Database Access)

# &#x20;  │

# &#x20;  ▼

# MySQL Database

# ```

# 

# \- \*\*Menus\*\* — handle all console I/O and user interaction

# \- \*\*Service\*\* — enforces business rules (eligibility, payment logic, backdating checks)

# \- \*\*DAO\*\* — isolates all SQL/database access from business logic

# 

# \## Key Features

# 

# \- \*\*Eligibility validation\*\* — checks whether a participant/team qualifies for an event before allowing registration

# \- \*\*Team-aware registration\*\* — collects credentials for every team member, not just the registrant

# \- \*\*Wallet \& transaction system\*\* — in-app wallet balance with transaction history, backed by MySQL stored procedures for every money-moving operation

# \- \*\*Custom waiting list\*\* — implemented with a hand-built singly linked list (no built-in collection used) for FIFO waitlist handling when an event fills up

# \- \*\*Backdating prevention\*\* — blocks registrations or transactions from being recorded against past dates

# \- \*\*Reactivate registration\*\* — allows a previously cancelled registration to be reinstated instead of forcing a fresh one

# \- \*\*Certificate generation\*\* — participation certificates generated as PDFs using raw PDF byte-level generation written in pure Java, with no external PDF libraries

# 

# \## Tech Stack

# 

# \- \*\*Language:\*\* Java

# \- \*\*Database:\*\* MySQL

# \- \*\*Database logic:\*\* Stored procedures, cursors, triggers, and access control for all financial operations

# \- \*\*PDF generation:\*\* Custom raw-byte PDF writer (pure Java, no libraries)

# 

# \## Project Structure

# 

# ```

# EventNova/

# ├── README.md

# ├── src/

# │   └── EventNova.java        # Application source

# ├── sql/

# │   └── eventnova\_schema.sql  # Schema, stored procedures, triggers

# └── docs/                     # Supplementary notes (optional)

# ```

# 

# \## Setup

# 

# 1\. \*\*Database setup\*\*

# &#x20;  - Create a MySQL database (e.g. `eventnova`)

# &#x20;  - Run `sql/eventnova\_schema.sql` against it to create tables, stored procedures, and triggers

# 

# 2\. \*\*Configure connection\*\*

# &#x20;  - Update the database connection details (host, username, password, database name) in the Java source to match your local MySQL setup

# 

# 3\. \*\*Compile and run\*\*

# &#x20;  ```bash

# &#x20;  javac src/EventNova.java

# &#x20;  java -cp src EventNova

# &#x20;  ```

# 

# \## Notes

# 

# This project was built as a Semester 2 academic project, focused on layered application design and moving all financial/transactional logic into the database layer via stored procedures rather than handling it in application code.

