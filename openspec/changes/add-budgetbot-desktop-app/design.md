# Design: BudgetBot JavaFX desktop budget tracker

## Overview

BudgetBot is a local, single-user JavaFX desktop application. It records transactions and category budgets in a SQLite database and derives dashboard figures from that durable data. The app deliberately separates cash flow from spending limits: income changes the overall balance but is never assigned to a budget category.

```
JavaFX views
    |
controllers / view models
    |
budget and transaction services
    |
SQLite repositories
    |
budgetbot.db
```

## Architecture

- **JavaFX presentation**: a dashboard, transaction management view, category-and-budget management view, and settings view. Views expose validation feedback instead of writing directly to the database.
- **Application services**: own validation and the calculations for monthly spending, available amounts, warnings, and overall balance.
- **Domain model**: `Category`, `Transaction`, `MonthlyBudget`, and `BudgetSettings`. Monetary values use `BigDecimal` (or integer minor units consistently) rather than binary floating point.
- **Persistence**: SQLite repositories own SQL and connection handling. Database initialization and schema migration must be safe to repeat.

## Core data and calculations

### Transactions

Every transaction stores an identifier, type, positive amount, date, optional description, and creation metadata. An `EXPENSE` must reference a category. An `INCOME` has no category.

```
overall balance  = sum(income amounts) - sum(expense amounts)
category spent   = sum(expense amounts for category and month)
```

### Monthly budgets and history

Each category has a fixed base amount for a calendar month. Budget values used for a started month are saved as monthly records so later edits do not rewrite prior results.

The settings view changes global settings for the next unstarted month. A started month retains the rollover policy and warning threshold used for its calculations.

```
available amount (rollover off) = monthly base amount
available amount (rollover on)  = monthly base amount + prior month's remaining amount
remaining amount                = available amount - category spent
```

Negative remaining amounts roll forward when rollover is enabled.

### Categories

A fresh database seeds: Housing & Utilities, Groceries, Dining, Transport, Health, Entertainment, Shopping, Education, and Miscellaneous. Users can add and rename categories. A category with referenced expenses cannot be removed until the user chooses a replacement category and its transactions are reassigned; removing the final available category is rejected.

### Budget states

The global warning threshold is a percentage from 1 through 99, defaulting to 80. For each category:

- `NORMAL`: spending is below the warning threshold.
- `WARNING`: spending is at or above the warning threshold and below 100 percent.
- `OVER_BUDGET`: spending is at or above the available amount.

Dashboard styling and accessible text expose the state; the initial release does not require operating-system notifications.

## Error handling and validation

- Amounts must be positive monetary values, dates must be valid, and an expense must select a category.
- Base budgets must be non-negative monetary values.
- Invalid edits show an actionable message and leave stored data unchanged.
- Database initialization or persistence failures are reported to the user without corrupting stored data.

## Testing strategy

- Unit-test monetary calculations, rollover behavior, setting-effective-month behavior, category deletion/reassignment rules, and validation.
- Test SQLite repositories against an isolated test database.
- Test service-level dashboard states and transaction/category workflows.
- Retain the repository's existing Gradle quality checks and coverage gate.
