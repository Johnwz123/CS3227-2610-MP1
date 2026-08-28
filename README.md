---
id: overview
slug: /
---

# BudgetBot

BudgetBot is a local, single-user JavaFX desktop application for recording income and expenses, and monitoring fixed monthly spending budgets. It stores data in a SQLite database on the user's computer. An account, network connection, cloud service, bank integration, or separately installed financial service is not required.

The detailed documentation is split into two guides:

- [User Guide](docs/UserGuide.md)
- [Developer Guide](docs/DeveloperGuide.md)

## Product at a glance

| View         | Features                                                                                                                                             |
|--------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| Dashboard    | Shows selected-month **Net cash flow** and category budget progress.                                                                                 |
| Transactions | Create, edit, and delete income and expense entries. The table is newest first and supports description, date, category, type, and amount filtering. |
| Categories   | Starts with nine default expense categories. Categories can be added, renamed, and removed after their expenses are reassigned.                      |
| Budgets      | Creates and stores a base budget snapshot for each category when a calendar month is opened.                                                         |
| Settings     | Stores a warning threshold from 1% to 99%; the default is 80%.                                                                                       |

## Installation

Open the repository's [GitHub Releases](https://github.com/johnwz123/CS3227-2610-MP1/releases) page and download the package for the corresponding operating system. Packaged releases include the runtime, so users do not need Java, Gradle, or a terminal.

- **Windows:** Download `BudgetBot-<version>-windows.msi`, open it, complete the installer, and launch BudgetBot from the Start menu.
- **macOS:** Download `BudgetBot-<version>-macos.dmg`, open it, drag BudgetBot to **Applications**, and launch it from Applications.
- **Debian/Ubuntu Linux:** Download `BudgetBot-<version>-linux.deb` and install it with the system software installer, or run `sudo apt install ./BudgetBot-<version>-linux.deb`. Launch BudgetBot from the applications menu.

Packages are currently unsigned and macOS packages are not notarized. A trust warning can therefore be expected. Continue only after verifying that the package came from this repository's GitHub Release.

Installing a newer package over an existing installation preserves the separate local database. A normal uninstall removes application files but keeps the database. To clear all local data, close BudgetBot and delete the `.budgetbot` directory in the user's home directory.

## Repository layout

```text
.
├── src/main/java/budgetbot/
│   ├── BudgetBotApp.java                  JavaFX entry point and composition root
│   ├── DatabasePaths.java                 default and normalized database paths
│   ├── model/                             immutable records and enums
│   ├── persistence/                       SQLite facade, repositories, and schema
│   ├── service/                           validation, use cases, and calculations
│   ├── tools/                             reset and demo-seed command implementation
│   └── ui/                                JavaFX window, views, dialogs, and tables
├── src/main/resources/budgetbot/          application stylesheet
├── src/test/java/budgetbot/               model, service, persistence, tool, UI, and architecture tests
├── docs/                                  User Guide, Developer Guide, and reflection workspace
├── openspec/                              specifications and archived proposal/design/task artifacts
├── scripts/                               cross-platform database launchers and CI coverage formatter
├── config/                                Checkstyle, PMD, and SpotBugs configuration
├── .github/workflows/                     CI, GitHub Pages, and native release workflows
├── website/                               Docusaurus documentation site
├── build.gradle                           Java, quality, coverage, database-tool, and packaging tasks
└── gradle/wrapper/                        pinned Gradle Wrapper distribution
```
