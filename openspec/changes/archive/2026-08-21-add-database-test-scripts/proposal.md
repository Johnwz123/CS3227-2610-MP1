## Why

Developers currently have to manually locate and alter BudgetBot's SQLite file before testing a clean state or demonstrating realistic activity. Repeatable reset and seed tooling will make local testing and demos quicker while protecting users from an accidental data wipe.

## What Changes

- Add a reset script that deletes and recreates the local BudgetBot database only after explicit confirmation or a non-interactive force option, then runs normal initialization so the schema, default settings, and default categories are present.
- Add a deterministic seed script that creates realistic categories, monthly budgets, and dated income and expense transactions for a ready-to-demo budget.
- Provide Windows and macOS/Linux entry points that run the same operations through the repository's supported Java/Gradle environment, without requiring a separate SQLite CLI or external service.
- Document how to run both scripts and how their target database is selected.

## Capabilities

### New Capabilities

- `database-demo-tooling`: Cross-platform developer commands for safely resetting local BudgetBot data and populating it with deterministic demo data.

### Modified Capabilities

- None.

## Impact

- New developer tooling and any supporting application entry points or Gradle tasks.
- Local SQLite database contents at the explicit target path.
- README and developer documentation.
