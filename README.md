# IgirePay Payment Gateway System

> Desktop fintech project: a simple digital wallet inspired by mobile money services.

## Project Overview

IgirePay is a desktop Java application that simulates a digital wallet/payment gateway. It lets users register, authenticate with a PIN, manage wallet and savings accounts, transfer money, request loans, and view transaction history. The project uses JavaFX for the UI and PostgreSQL for persistence.

## Features

- Customer registration
- PIN authentication
- Wallet account (primary account)
- Savings account
- Deposit
- Withdraw
- Transfer money (within system)
- Loan request
- Transaction history
- CSV export of transactions
- Daily transaction summary
- Duplicate transaction prevention (reference IDs)
- PostgreSQL persistence

## Technologies Used

- Java (Maven)
- JavaFX
- JDBC
- PostgreSQL
- DAO pattern
- Git & GitHub

## Project Architecture

Simple layered structure:

JavaFX UI → Service Layer → DAO Layer → PostgreSQL

The UI (FXML + controllers) uses the services which contain business logic; services call DAOs to read/write the database.

## Database Setup

1. Install PostgreSQL and create a database for the project, for example `igirepay`.
2. Run the SQL schema to create tables:

```sql
-- from project root
psql -d igirepay -f src/main/resources/db/schema.sql
```

3. Update the database connection details if needed in `src/main/java/com/app/igirepay/lab2/dao/impl/DatabaseConnection.java`.

## How to Run the Project

1. Clone the repository:

```bash
git clone https://github.com/EmeranceU/Phase-One-Capstone-Project.git
cd Phase-One-Capstone-Project
```

2. Build with Maven (Windows):

```powershell
mvnw.cmd clean package -DskipTests
```

Or on macOS/Linux:

```bash
./mvnw clean package -DskipTests
```

3. Open the project in IntelliJ (recommended). Run the main JavaFX application `IgirePayApplication` from the IDE.

(If you prefer CLI-based run and the project is configured for it, run the appropriate Maven goal or use the IDE launcher.)

## Git Workflow

- `lab1` — initial object-oriented exercises and CLI components
- `lab2-jdbc-postgresql` — JDBC + PostgreSQL migration and DAO implementations
- `lab3` — JavaFX frontend (this branch contains the UI and related changes)

Work is done on feature branches and merged via Pull Requests into `main` after review.

## Challenges Faced

- Keeping PostgreSQL balances synchronized and consistent across operations.
- Preventing duplicate transactions using unique reference IDs.
- Updating the JavaFX dashboard reliably after background data changes.
- Maintaining a clean DAO/service separation while moving from file-based to DB persistence.

## Future Improvements

- Add an admin dashboard for monitoring and account management.
- Add better transaction filtering and date range queries in the UI.
- Add notifications or email receipts for large transactions.
- Implement transactional rollback for multi-step operations.

## Author

Emerance UMURERWA

---

Project maintained for academic / bootcamp use. See the repository for source files and the database schema.
