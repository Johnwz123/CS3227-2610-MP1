## 1. Simplify the budget model and persistence schema

- [x] 1.1 Remove rollover and carryover fields from `BudgetSettings` and `MonthlyBudget`, and make monthly availability equal the fixed base amount.
- [x] 1.2 Remove rollover controls from the settings service and JavaFX settings view while preserving configurable warning thresholds.
- [x] 1.3 Replace the settings and monthly-budget SQLite table definitions and repository queries with the unreleased simplified schema; do not implement migration compatibility.

## 2. Provide selected-month net cash flow

- [x] 2.1 Add a persistence calculation for income minus expenses constrained to a supplied `YearMonth`.
- [x] 2.2 Replace dashboard snapshot overall-balance and recent-transaction data with selected-month net cash flow in `BudgetService` and its callers.
- [x] 2.3 Update the dashboard UI to label and display `Net cash flow` and remove the Recent activity table and its now-unused dependencies.

## 3. Align documentation and automated tests

- [x] 3.1 Update model, persistence, and service tests for the simplified records and schema, fixed monthly availability, and month-scoped net cash flow including zero-activity months.
- [x] 3.2 Update JavaFX tests to assert the dashboard's Net cash flow display and absence of Overall balance and Recent activity, and to remove rollover-setting expectations.
- [x] 3.3 Update the README and user/developer guides to describe fixed monthly budgets, warning thresholds, and selected-month net cash flow.

## 4. Verify the change

- [x] 4.1 Run `openspec validate simplify-monthly-dashboard --strict` and resolve artifact validation failures.
- [x] 4.2 Run `gradlew.bat spotlessApply` and `gradlew.bat check`, then review `git diff --check` and the scoped diff.
