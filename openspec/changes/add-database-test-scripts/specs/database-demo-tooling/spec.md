## ADDED Requirements

### Requirement: Cross-platform database-tooling launchers
The repository SHALL provide discoverable reset and seed launchers for Windows PowerShell and macOS/Linux POSIX shells. Each platform launcher SHALL invoke the same Java/Gradle-backed database tool and SHALL require no separately installed SQLite client or external service.

#### Scenario: Run reset on Windows
- **WHEN** a developer invokes the documented PowerShell reset launcher
- **THEN** it runs the reset operation through the repository Gradle wrapper

#### Scenario: Run seed on macOS or Linux
- **WHEN** a developer invokes the documented POSIX-shell seed launcher
- **THEN** it runs the seed operation through the repository Gradle wrapper

### Requirement: Explicit and observable database target
The database tool SHALL accept an optional database-path argument for both reset and seed operations. If no path is supplied, it SHALL target the same default database path used by BudgetBot. Before it modifies the database, the tool SHALL report the normalized target path.

#### Scenario: Use the application default database
- **WHEN** a developer runs a database operation without a target-path argument
- **THEN** the operation targets BudgetBot's standard local database path

#### Scenario: Use a disposable database target
- **WHEN** a developer supplies a database-path argument
- **THEN** the operation modifies only that normalized database path

### Requirement: Safe initialized-database reset
The reset operation SHALL require an explicit confirmation or non-interactive force option before deleting data. When authorized, it SHALL remove only the selected SQLite database and its sidecar files for that exact database, then run the normal BudgetBot database initialization. The resulting database SHALL contain the complete schema, default settings, and default categories that exist after the application's first startup.

#### Scenario: Reject an unconfirmed reset
- **WHEN** a developer invokes reset without the required confirmation or force option
- **THEN** the tool exits without changing the target database and explains how to confirm the reset

#### Scenario: Reset a populated database
- **WHEN** a developer confirms reset for a database containing budgets and transactions
- **THEN** the prior data is removed and the recreated database contains initialized tables, default settings, and default categories

#### Scenario: Reset a database with SQLite sidecars
- **WHEN** a developer confirms reset for a target that has SQLite write-ahead-log or shared-memory sidecar files
- **THEN** the tool removes only the target database and its matching sidecar files before initialization

### Requirement: Predictable demo-data seeding
The seed operation SHALL populate an initialized target database with a representative, deterministic demo scenario using application-supported data paths. The scenario SHALL include category budgets and multiple dated income and expense transactions that appear in the current month-oriented dashboard, with fixed amounts and descriptions suitable for testing and demonstrations.

#### Scenario: Seed a freshly reset database
- **WHEN** a developer runs seed against a database in the initialized first-start state
- **THEN** the tool creates the documented demo budgets and income and expense transactions successfully

#### Scenario: View the seeded current month
- **WHEN** a developer opens BudgetBot for the seeded current month
- **THEN** the dashboard shows category budget activity and a net cash flow derived from the seeded income and expenses

### Requirement: Protect against duplicate demo data
The seed operation SHALL refuse to append its demo scenario when the selected database already contains transaction data. It SHALL explain that the developer must reset the target database before seeding again.

#### Scenario: Repeat a seed operation
- **WHEN** a developer runs seed against a database that already contains transactions
- **THEN** the tool leaves the existing data unchanged and reports that reset is required before reseeding

### Requirement: Document database demo workflow
The project documentation SHALL describe the Windows and macOS/Linux commands, the default and optional target-path behavior, the required reset confirmation, and the recommended reset-then-seed workflow. It SHALL state that reset irreversibly deletes data at the selected target.

#### Scenario: Follow documented setup steps
- **WHEN** a developer follows the documented reset and seed commands
- **THEN** they can create an initialized, repeatable demo database without manually editing SQLite files
