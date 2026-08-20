---
id: user-guide
slug: /UserGuide
---

# User Guide

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
