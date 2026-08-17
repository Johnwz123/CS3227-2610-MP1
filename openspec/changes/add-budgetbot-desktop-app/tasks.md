# Implementation Tasks

## 1. Application foundation

- [x] 1.1 Add JavaFX and SQLite JDBC dependencies while retaining the Java 25 Gradle toolchain and existing quality tasks.
- [x] 1.2 Replace the starter entry point with the BudgetBot JavaFX application bootstrap and establish view navigation.
- [x] 1.3 Add repeatable SQLite database initialization, schema migration, and isolated test-database support.

## 2. Domain and persistence

- [x] 2.1 Implement money-safe domain types for categories, transactions, monthly budgets, and global settings.
- [x] 2.2 Implement SQLite repositories and seed the default expense categories for a new budget.
- [x] 2.3 Implement category add, rename, reassignment, and removal persistence rules.
- [x] 2.4 Implement transaction create, edit, delete, and retrieval persistence for income and expenses.

## 3. Budget services

- [x] 3.1 Implement fixed monthly category budget creation and historical monthly snapshots.
- [x] 3.2 Implement global rollover behavior, including negative carryover and next-unstarted-month setting changes.
- [x] 3.3 Implement overall balance, category spending, remaining amount, warning, and over-budget calculations.
- [x] 3.4 Implement validation and user-facing error results for invalid monetary values, dates, transaction types, and category operations.

## 4. JavaFX user experience

- [x] 4.1 Build the selected-month dashboard with overall balance, recent transactions, per-category progress, and visible warning/over-budget states.
- [x] 4.2 Build transaction forms and management interactions for income and categorized expenses.
- [x] 4.3 Build category and monthly-budget management interactions, including reassignment before deletion.
- [x] 4.4 Build global settings interactions for rollover and warning threshold.

## 5. Verification and documentation

- [x] 5.1 Add unit and repository tests for transaction, category, budget, rollover, and warning-threshold scenarios.
- [x] 5.2 Add service-level tests for dashboard calculations and historical-month stability.
- [x] 5.3 Update README and user/developer guides for BudgetBot setup, run, data storage, and workflows.
- [ ] 5.4 Run `gradlew.bat check --no-daemon` and manually verify the principal JavaFX workflows.
