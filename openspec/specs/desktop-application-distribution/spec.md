# desktop-application-distribution Specification

## Purpose
TBD - created by archiving change add-desktop-release-packaging. Update Purpose after archive.
## Requirements
### Requirement: Self-contained native installers
BudgetBot SHALL provide self-contained native installation packages that include the runtime needed to launch the application without a separately installed JDK, JavaFX runtime, Gradle, or terminal. The initial release SHALL provide a Windows `.msi` installer, a macOS `.dmg` application image, and a Debian/Ubuntu Linux `.deb` package.

#### Scenario: Download the correct Windows package
- **WHEN** a Windows user opens a BudgetBot GitHub Release
- **THEN** the release provides a clearly named Windows `.msi` asset for that version

#### Scenario: Download the correct macOS package
- **WHEN** a macOS user opens a BudgetBot GitHub Release
- **THEN** the release provides a clearly named macOS `.dmg` asset for that version

#### Scenario: Download the correct Linux package
- **WHEN** a Debian or Ubuntu user opens a BudgetBot GitHub Release
- **THEN** the release provides a clearly named Linux `.deb` asset for that version

### Requirement: Standard installation and launch experience
The project documentation SHALL describe how users install and launch each supported native package using their platform's standard user experience. It SHALL state that a user does not need to install Java or run Gradle to use a packaged application.

#### Scenario: Install BudgetBot on Windows
- **WHEN** a Windows user opens the downloaded BudgetBot `.msi` file
- **THEN** the standard installer installs BudgetBot and makes it available to launch from the operating system

#### Scenario: Install BudgetBot on macOS
- **WHEN** a macOS user opens the downloaded BudgetBot `.dmg` file and places the app in Applications
- **THEN** the user can launch BudgetBot from Applications without configuring Java

#### Scenario: Install BudgetBot on Debian or Ubuntu
- **WHEN** a Debian or Ubuntu user installs the downloaded BudgetBot `.deb` package
- **THEN** the user can launch BudgetBot without configuring Java

### Requirement: Upgrade preserves local budget data
Installing a newer BudgetBot package over an existing installation SHALL preserve the user's local database at `~/.budgetbot/budgetbot.db`.

#### Scenario: Upgrade BudgetBot
- **WHEN** a user installs a newer release of BudgetBot
- **THEN** the new installation can access the user's pre-existing local budget data

### Requirement: Uninstall preserves local budget data by default
The documented Windows, macOS, and Linux uninstall operations SHALL remove the installed application without deleting `~/.budgetbot/budgetbot.db` or other BudgetBot user data. The documentation SHALL give separately labelled instructions for users who intentionally want to remove all local BudgetBot data.

#### Scenario: Uninstall the application only
- **WHEN** a user removes BudgetBot through the platform's standard uninstall mechanism
- **THEN** the installed application is removed and the user's local budget database remains intact

#### Scenario: Explicitly remove all data
- **WHEN** a user follows the documented full-data-removal instructions
- **THEN** they can deliberately remove the local BudgetBot data directory after uninstalling the application

