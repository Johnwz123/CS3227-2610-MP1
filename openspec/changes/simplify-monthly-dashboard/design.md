## Context

BudgetBot currently renders a dashboard for a selected `YearMonth`, but `DashboardSnapshot` contains an all-time overall balance and the eight newest transactions across every month. Monthly budgets also persist a carryover amount and a copied global rollover preference, so a category's available amount can differ from its configured base amount.

This change keeps the application local, SQLite-backed, and single-user. It retains monthly category budget snapshots and the warning-threshold snapshot, but gives every category a fixed available amount equal to its base amount for that month.

## Goals / Non-Goals

**Goals:**

- Make every figure shown on the dashboard describe its selected calendar month.
- Display selected-month income minus expenses as **Net cash flow**.
- Remove the global rollover preference and carryover calculations.
- Simplify the unreleased SQLite schema by removing obsolete rollover and carryover fields.

**Non-Goals:**

- Add account balances, bank reconciliation, or cross-month cash-flow reporting.
- Rework category budget editing, warning-threshold behaviour, or month navigation.
- Preserve data from databases created by earlier development builds or support downgrade compatibility.

## Decisions

### Replace the dashboard aggregate with a month-scoped net figure

`DashboardSnapshot` will expose a `netCashFlow` value for its selected month rather than an all-time `overallBalance`; it will no longer include recent transactions. `BudgetService.dashboard(month)` will obtain the value from a persistence operation constrained to that month, and `DashboardView` will show only the label `Net cash flow` and the category budget table.

This prevents the selected-month heading from being paired with global data. Retaining the label "Overall balance" was rejected because it conventionally denotes an account-wide position, not a period total.

### Make monthly budget availability equal the base amount

The monthly-budget domain model and repository will no longer store, calculate, or read carryover and rollover state. `availableAmount()` will be the month's fixed base amount. The global settings model, repository, and settings view will retain only the warning threshold.

This removes an optional second budgeting model and makes the amount shown for every category directly traceable to the configured monthly budget. Keeping rollover but hiding its setting was rejected because it would preserve unexplained availability differences.

### Simplify the unreleased SQLite schema directly

The settings table will contain only the warning threshold, and the monthly-budgets table will omit `carryover` and `rollover_enabled`. The associated records, repositories, and schema initialization will use only this simplified shape.

BudgetBot has not been released, so migration and downgrade compatibility are deliberately out of scope. Developers with an older local database must delete it and start with a fresh database; this is preferable to retaining fields for a feature the product no longer has.

### Update the existing budget-tracking contract and tests together

The `budget-tracking` delta specification will replace rollover, overall-balance, and recent-activity requirements. Service, persistence, and JavaFX tests will cover month boundaries, no-transaction months, and fixed availability despite prior over/underspending. README and user-guide wording will describe fixed monthly budgets and monthly net cash flow.

## Risks / Trade-offs

- [A user expects an all-time cash position] → Label the new metric explicitly as **Net cash flow** and leave a future all-time account-position feature as separate scope.
- [A developer has a database from an earlier build] → Document that it must be deleted because migration is intentionally unsupported before release.
- [A month has no transactions] → Return and display a monetary zero, not an empty or null value.
- [A refactor leaves old global queries in the dashboard] → Remove `recentTransactions` and `overallBalance` from the dashboard snapshot so UI code cannot accidentally render them.
