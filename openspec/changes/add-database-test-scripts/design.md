## Context

BudgetBot stores its local data in SQLite and initializes its schema, settings, and
default categories when `BudgetDatabase` is opened. Developers currently need to
locate and manipulate that database manually to return to a clean state or prepare
a realistic demo. The requested tooling must work from Windows and macOS/Linux,
without relying on a separately installed SQLite command-line client.

The reset operation is intentionally destructive, so it must act only on one
resolved database file and require an explicit opt-in. Its result is not an empty
file: it is the same initialized state produced by a first application startup,
including all tables, default settings, and default categories.

## Goals / Non-Goals

**Goals:**

- Provide two named developer operations: reset the local database and seed it
  with predictable demo data.
- Make each operation available through PowerShell and POSIX-shell launchers that
  delegate to one shared Java implementation through the Gradle wrapper.
- Reuse the application's database initialization path so reset always produces
  the normal first-start state.
- Make the target database explicit, with the application's standard database
  path as the default, and protect reset with an explicit force/confirmation
  argument.
- Supply a coherent, repeatable set of categories, monthly budgets, and income
  and expense transactions that make the month-oriented dashboard useful in a
  demo.

**Non-Goals:**

- Migrating, backing up, or preserving the prior database contents during reset.
- Providing production data-management, import/export, or database administration
  tools.
- Supporting a system-wide SQLite installation or an external service.
- Changing BudgetBot's normal user-facing database behaviour or dashboard rules.

## Decisions

### Pair platform launchers with one Java database tool

Each operation will have a small platform-specific launcher in `scripts/`: a
PowerShell launcher for Windows and a POSIX-shell launcher for macOS/Linux. The
launchers will call the repository's Gradle wrapper and pass through target-path
and force options to a single Java command implementation. Gradle is already the
project's supported way to resolve the Java runtime and dependencies, so this
avoids platform-specific database logic and a new runtime dependency.

The Java command will expose `reset` and `seed` subcommands, while Gradle supplies
a `JavaExec` entry point for the launchers. Keeping file deletion, schema
initialization, and data insertion in Java lets both operating systems share the
same validation and failure behaviour.

Alternatives considered:

- Separate PowerShell and shell implementations using `sqlite3` were rejected
  because they duplicate behaviour and require an external executable.
- Gradle tasks alone were rejected because the request is for discoverable reset
  and seed scripts; the launchers provide those named entry points while still
  using Gradle underneath.

### Resolve one explicit database path and share the application's default

The tool will accept an optional database-path argument. When omitted, it will use
the same `~/.budgetbot/budgetbot.db` resolution as the desktop application,
factored into a shared helper if necessary. Before modifying anything, it will
normalize the supplied path, create only its parent directories as needed, and
report the exact target.

This makes demo use convenient while allowing tests or developers to direct the
tool at a disposable database. It also prevents the scripts from discovering or
deleting databases through broad directory scans.

### Reset by deleting only the target database, then use normal initialization

The reset subcommand will require an explicit force/confirmation option. Once
provided, it will remove only the resolved database file (and SQLite sidecar files
for that exact file when present), then construct and close `BudgetDatabase`.
That construction runs the existing schema and default-category initialization,
so the resulting database is equivalent to a newly started application rather
than an uninitialized or schema-less file.

Using the application's initializer is preferred over maintaining separate SQL
schema and category fixtures in the scripts: changes to normal first startup are
automatically reflected in reset.

### Seed a fixed, idempotence-protected demo scenario

The seed subcommand will create a documented mock scenario using normal
repositories/services: default and demo-relevant categories, monthly category
budgets, and dated income and expense transactions. Amounts, descriptions, and
relative month placement will be fixed by the tool so repeated demos present a
consistent narrative while still appearing in the current month-oriented view.

Seeding will detect existing transaction data and refuse to append a second demo
set unless the caller explicitly requests replacement through the reset workflow.
The intended workflow is therefore `reset` followed by `seed`, which prevents
duplicated transactions and preserves predictable totals.

Alternatives considered:

- Always resetting as part of seed was rejected because reset is destructive and
  must remain an explicit, separately confirmed operation.
- Blindly appending seed rows was rejected because every rerun would distort
  dashboard totals and undermine demos.

## Risks / Trade-offs

- A reset can erase a developer's real local data if aimed at the default path
  accidentally -> Require an explicit force/confirmation argument, print the
  resolved path, and support an explicit disposable target path.
- SQLite may leave write-ahead-log or shared-memory sidecar files -> Remove only
  sidecars derived from the exact resolved database filename before initialization.
- Shell execution permissions can block a POSIX launcher on a fresh checkout ->
  Document invoking it with `sh` and retain Gradle-wrapper commands as the
  underlying portable fallback.
- A fixed demo scenario can become stale as the category model evolves -> Create
  data through application repositories/services and keep its expected contents
  covered by tests.
- A database can be locked by a running BudgetBot instance -> Surface the SQLite
  failure clearly and instruct the developer to close the application before
  rerunning the script.

## Migration Plan

No database migration is required. Add the launchers, shared tool, Gradle entry
point, tests, and documentation. Existing application databases remain untouched
unless a developer explicitly runs reset against them. Rollback consists of
removing the new tooling; a reset cannot restore data that its caller chose to
delete.

## Open Questions

- None. The seed scenario will be specified in the requirements artifact and
  implemented as a representative current-month demo dataset.
