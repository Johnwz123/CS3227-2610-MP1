---
id: overview
slug: /
---

# BudgetBot

BudgetBot is a local JavaFX desktop budget tracker for a single user. It records income and
expenses, tracks fixed monthly category budgets, and highlights categories approaching or exceeding
their limits.

Budget data is stored locally at `~/.budgetbot/budgetbot.db`. BudgetBot does not require an account,
network connection, or external financial service.

## Prerequisites

- JDK 25
- Node.js 24 or later (only for the documentation site)

## Project layout

```text
src/main/java/budgetbot/
  BudgetBotApp.java                 application entry point
  model/                            immutable domain records and enums
  persistence/                      SQLite persistence
  service/                          business rules and use cases
  ui/                               JavaFX user interface
    BudgetBotWindow.java            window shell and navigation
    ViewCoordinator.java            selected month and active-view rendering
    dialogs/                        transaction, category, and budget dialogs
    tables/                         transaction and budget-summary table factories
    views/                          dashboard, transaction, budget, and settings views
```

## Common commands

On Windows, use `gradlew.bat`. On macOS/Linux, use `./gradlew`.

```text
gradlew.bat run                 # Run the application
gradlew.bat check               # Test and run all Java quality checks
gradlew.bat spotlessApply       # Format Java sources
gradlew.bat jacocoTestReport    # Generate the coverage report
gradlew.bat openJacocoReport    # Generate and open the coverage report
gradlew.bat javadoc             # Generate API documentation
gradlew.bat openJavadoc         # Generate and open API documentation
```

## Resetting and seeding local data

The database tools create a repeatable local state for testing and demonstrations. Reset is
destructive: it permanently deletes the selected database and its SQLite sidecar files, then recreates
the normal first-start database with all tables, the default settings, and default categories. Seed
adds a fixed current-month demo budget, income, and expenses; run reset before seeding again.

On Windows PowerShell:

```powershell
.\scripts\reset-database.ps1
.\scripts\seed-demo-data.ps1
```

Type `RESET` when prompted. For a non-interactive reset, use `-Force`; both scripts accept
`-DatabasePath` to work with a disposable database instead of the normal local one:

```powershell
.\scripts\reset-database.ps1 -Force -DatabasePath "$env:TEMP\budgetbot-demo.db"
.\scripts\seed-demo-data.ps1 -DatabasePath "$env:TEMP\budgetbot-demo.db"
```

On macOS/Linux, invoke the POSIX-shell launchers with `sh` (so no executable-bit setup is needed):

```sh
sh scripts/reset-database.sh
sh scripts/seed-demo-data.sh
```

Enter `RESET` when prompted by the reset script. Pass one optional database path to either command to
use a disposable target, for example `sh scripts/reset-database.sh /tmp/budgetbot-demo.db`.

When no path is supplied, every launcher targets `~/.budgetbot/budgetbot.db`. Close BudgetBot before
running either operation so SQLite does not hold the database open.

To run the documentation site locally:

```text
cd website
npm install
npm start
```

The published guides are built from [DeveloperGuide.md](docs/DeveloperGuide.md) and
[UserGuide.md](docs/UserGuide.md).

## Highlights

- Seeded expense categories that can be added, renamed, or removed after reassignment.
- Income and categorized expense transactions that determine selected-month net cash flow.
- Fixed monthly category budgets that reset to their configured base amount each month.
- A configurable global spending-warning threshold (80% by default) and over-budget alerts.
