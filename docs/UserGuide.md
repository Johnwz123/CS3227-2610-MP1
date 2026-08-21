---
id: user-guide
slug: /UserGuide
---

# User Guide

## Install, update, and launch

Download the installer that matches your computer from the project's GitHub **Releases** page. A
packaged BudgetBot release includes everything it needs to run; Java, Gradle, and a terminal are not
required.

- **Windows:** Open `BudgetBot-<version>-windows.msi` and complete the installer. Open BudgetBot from
  the Start menu. To update, run a newer `.msi` installer.
- **macOS:** Open `BudgetBot-<version>-macos.dmg`, drag BudgetBot into **Applications**, then launch it
  from Applications. To update, replace the existing app in Applications with the newer one.
- **Debian/Ubuntu Linux:** Open `BudgetBot-<version>-linux.deb` in the system software installer, or
  run `sudo apt install ./BudgetBot-<version>-linux.deb`. Launch it from the applications menu. To
  update, install the newer `.deb` package in the same way.

The packages are currently unsigned. Windows and macOS can show a trust warning; proceed only when
the package came from this repository's GitHub Release.

## Uninstalling BudgetBot

Normal uninstall removes the application but keeps your budget history at
`~/.budgetbot/budgetbot.db`. Use **Installed apps** on Windows, delete BudgetBot from **Applications**
on macOS, or run `sudo apt remove budgetbot` on Debian/Ubuntu. A later reinstall can use the retained
data.

### Remove all BudgetBot data (permanent)

Only do this if you intentionally want to erase your budget history. Uninstalling alone does not do
this. Close BudgetBot, then delete the `.budgetbot` directory in your home folder:

```text
Windows: %USERPROFILE%\.budgetbot
macOS/Linux: ~/.budgetbot
```

## Running the Application

Run the application from the repository root:

```text
gradlew.bat run
```

BudgetBot opens a local desktop window. It stores data on this computer and does not require an
account or internet connection.

## Dashboard

Use the month controls to view a different calendar month. The dashboard shows that month's net cash
flow (income minus expenses) and each category's spending against its fixed monthly budget. A category
warns at the configured threshold (80% by default) and becomes over budget at 100%.

## Transactions

Use **Transactions** to add, edit, or delete entries. Income increases net cash flow for its calendar
month and has no category. Expenses decrease net cash flow for their calendar month and must be
assigned to an expense category.

## Categories and budgets

BudgetBot starts with common expense categories. Use **Categories & budgets** to add or rename a
category, set its fixed monthly base amount, or remove it. When removing a category, choose another
category to receive its existing expenses first.

## Settings

Use **Settings** to choose a warning threshold from 1% to 99%. Changes are copied into the next month
that BudgetBot starts, leaving already started months unchanged. Every category's available amount is
its fixed monthly base amount; spending does not carry into another month.
