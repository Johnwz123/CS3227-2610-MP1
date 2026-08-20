## Why

The dashboard is navigated by month, but its lifetime overall-balance figure and globally sourced recent activity make the selected month ambiguous. Rollover adds a second, less predictable budget model where a category's available amount can differ from its configured monthly budget.

## What Changes

- Replace the dashboard's lifetime **Overall balance** with a selected-month **Net cash flow** figure: income minus expenses for that calendar month.
- Remove the dashboard's **Recent activity** section so all remaining dashboard content is scoped to the selected month.
- **BREAKING** Remove the global rollover preference and stop carrying category remainder or deficit into later monthly budgets. Each category's available amount starts from its fixed monthly base amount every month.

## Capabilities

### New Capabilities

- None.

### Modified Capabilities

- `budget-tracking`: Change dashboard cash-flow and content scope, and replace optional global rollover with fixed monthly category budgets.

## Impact

- Dashboard service calculations, snapshot model, and JavaFX dashboard view.
- Monthly-budget and settings persistence schema, which will remove rollover settings and carryover values.
- Budget settings UI, automated tests, README, and user/developer documentation.
