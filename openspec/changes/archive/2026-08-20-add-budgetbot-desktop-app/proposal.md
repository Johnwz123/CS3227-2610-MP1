# BudgetBot JavaFX desktop budget tracker

## Why

The current project is a Java application starter rather than a usable end-user product. BudgetBot will give a single desktop user a local, understandable way to record cash flow, set monthly spending limits, and see when their spending needs attention.

## What Changes

- Replace the starter application experience with BudgetBot, a JavaFX desktop application.
- Persist a single user's budget data locally in SQLite; no account, cloud synchronization, bank integration, or external service is required.
- Start new budgets with sensible expense categories and let users add, rename, and remove categories. Removing a category that has transactions requires reassignment first.
- Support income and expense transactions. Income affects only the overall balance; expenses require a category and count toward its monthly spending.
- Support a fixed monthly amount for each category, with a global rollover setting that is disabled by default. When enabled, the prior category balance, including a negative balance, is included in the next month's available amount.
- Show a dashboard with overall balance, recent activity, per-category budget progress, configurable warning states, and over-budget alerts.
- Add a global configurable warning threshold, defaulting to 80 percent. Crossing the threshold warns the user; spending at or over 100 percent is over budget.

## Non-goals

- Multi-user accounts, authentication, cloud synchronization, bank feeds, and recurring transactions.
- Income allocation to category budgets.
- Per-category rollover or per-category warning thresholds.
- Mobile or web clients.

## Impact

- `build.gradle` will need JavaFX and a SQLite JDBC driver in addition to the existing Java quality tooling.
- The Java starter entry point will be replaced with a JavaFX application and domain, persistence, service, and view layers.
- Documentation will be updated so the existing `gradlew.bat run` command and user/developer guides describe BudgetBot.
