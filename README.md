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
- Income transactions that affect overall balance and categorized expenses that affect budgets.
- Fixed monthly category budgets with optional, global rollover.
- A configurable global spending-warning threshold (80% by default) and over-budget alerts.
