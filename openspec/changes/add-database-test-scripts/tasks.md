## 1. Shared database-tool foundation

- [x] 1.1 Extract or add a shared resolver for BudgetBot's default database path and normalized explicit target paths.
- [x] 1.2 Add a Java database-tool entry point that parses reset and seed commands, target-path input, and reset confirmation/force input, then reports the resolved target.
- [x] 1.3 Implement the confirmed reset operation to delete only the selected SQLite database and its matching sidecars, then initialize it through `BudgetDatabase`.

## 2. Deterministic demo data

- [x] 2.1 Define and implement the fixed current-month demo categories, category budgets, and dated income and expense transactions through application-supported persistence or service paths.
- [x] 2.2 Detect existing transactions before seeding and fail without modifying data when demo data would be duplicated.

## 3. Cross-platform developer entry points

- [x] 3.1 Add a Gradle `JavaExec` entry point for the shared database tool, including command and argument forwarding.
- [x] 3.2 Add documented PowerShell reset and seed launchers that invoke the Gradle wrapper and expose target-path and force options.
- [x] 3.3 Add documented POSIX-shell reset and seed launchers that invoke the Gradle wrapper and expose target-path and force options.

## 4. Documentation and verification

- [x] 4.1 Document the Windows and macOS/Linux commands, default and custom database paths, destructive-reset warning, and reset-then-seed workflow in the README and developer guide.
- [x] 4.2 Add isolated tests for path resolution, unconfirmed reset rejection, initialized post-reset schema/settings/default categories, sidecar cleanup, deterministic seeding, duplicate-seed refusal, and resulting current-month dashboard net cash flow.
- [x] 4.3 Run formatting and the complete Gradle quality check, then validate the OpenSpec change strictly.
