---
id: developer-guide
slug: /DeveloperGuide
---

# Developer Guide

## Technology Stack

### Application Development

- **Language and API documentation**: Java 25 and Javadoc
- **Build and dependency management**: Gradle 9, using the Gradle Wrapper
- **Desktop UI**: JavaFX Controls
- **Local persistence**: SQLite through the Xerial JDBC driver

### Verification and Code Quality

- **Unit testing and mocking**: JUnit 6 and Mockito
- **Code formatting**: Spotless with Google Java Format
- **Style linting**: Checkstyle
- **Static code analysis**: PMD
- **Code coverage**: JaCoCo

### Documentation

- **Documentation site generator**: Docusaurus
- **Source guides**: Markdown files in the repository `docs` directory

### Collaboration and Delivery

- **Version control and hosting**: Git and GitHub
- **Continuous integration and deployment**: GitHub Actions
- **Automated dependency updates**: Dependabot

## Local Development

Install JDK 25 and use the Gradle Wrapper; no system Gradle installation is required.
Run `gradlew.bat check` on Windows (or `./gradlew check` on macOS/Linux) before opening a pull request.

For same-repository pull requests, CI also posts one updated JaCoCo instruction-coverage comment and
adds the same summary to the workflow run. Pull requests from forks retain the workflow summary and
downloadable report artifact, but do not receive a comment because their workflow token is read-only.

For the documentation site, install Node.js 24 or later, run `npm install` in `website`, then run `npm start`.

## Native release packaging

Use a full JDK 25 that includes `jpackage`; a runtime-only JDK cannot build installers. Run the
following from the repository root, replacing the version with the release version:

```text
gradlew.bat packageAppImage "-PreleaseVersion=1.2.3"
gradlew.bat packageNative "-PreleaseVersion=1.2.3"
```

`packageAppImage` is useful for inspecting the bundled application and runtime. `packageNative`
creates exactly one host-native installer in `build/packages/`: a Windows `.msi`, macOS `.dmg`, or
Debian/Ubuntu `.deb`. On Windows, install WiX Toolset and ensure its executables are on `PATH` before
creating an MSI. The packaged application uses the normal per-user data path, so the installer input
does not include `~/.budgetbot/budgetbot.db`.

To publish a release, first ensure the commit passes the normal quality suite. Push an annotated or
lightweight tag in the exact format `v<major>.<minor>.<patch>` (for example, `v1.2.3`). The release
workflow validates the tag, runs formatting, checks, coverage, and Javadoc on Windows, macOS, and
Ubuntu, then builds the native installer on each matching runner. It publishes one GitHub Release with
these three assets only after every matrix entry succeeds:

```text
BudgetBot-1.2.3-windows.msi
BudgetBot-1.2.3-macos.dmg
BudgetBot-1.2.3-linux.deb
```

The release workflow is the only workflow with permission to write GitHub Releases; ordinary CI
remains read-only. The initial packages are unsigned and not notarized, so platform trust warnings are
expected until a future signing change is implemented.

## System Overview

BudgetBot uses a layered JavaFX design: views call `BudgetService`, which owns validation and monthly
calculations, and `BudgetDatabase`, which owns the SQLite schema and queries. The default application
database is `~/.budgetbot/budgetbot.db`; tests create an isolated temporary database. Monetary values
are represented with `BigDecimal`, never floating point.

The database initializer is repeatable and seeds default expense categories only for a new database.
Monthly budget snapshots retain the base amount and warning threshold used for that month, so later
threshold changes do not rewrite historical dashboard calculations. The unreleased schema has no data
migration path; delete a database created by an earlier development build before running this version.

## Database reset and demo data

Use the repository scripts to prepare a clean SQLite database for tests or demonstrations. The reset
operation is irreversible: it removes only the selected database file and its matching SQLite sidecars,
then opens `BudgetDatabase` to recreate the schema, default settings, and default categories. The seed
operation creates a fixed current-month scenario with category budgets plus income and expense
transactions. It refuses to add a second scenario to a database that already contains transactions.

Close the application first. On Windows PowerShell, run:

```powershell
.\scripts\reset-database.ps1
.\scripts\seed-demo-data.ps1
```

The reset launcher requires the literal `RESET` confirmation, or its `-Force` switch for automated
use. Both launchers accept `-DatabasePath`, for example:

```powershell
.\scripts\reset-database.ps1 -Force -DatabasePath "$env:TEMP\budgetbot-demo.db"
.\scripts\seed-demo-data.ps1 -DatabasePath "$env:TEMP\budgetbot-demo.db"
```

On macOS/Linux, run the POSIX launchers through `sh`:

```sh
sh scripts/reset-database.sh
sh scripts/seed-demo-data.sh
```

Reset prompts for `RESET`; supply one optional database path as the sole argument to target a
disposable file, for example `sh scripts/reset-database.sh /tmp/budgetbot-demo.db`. Without an
explicit path, the scripts use the application's default `~/.budgetbot/budgetbot.db` location. The
launchers delegate to the Gradle Wrapper and Java tool, so no system SQLite client is required.
