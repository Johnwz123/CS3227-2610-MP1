## Why

The Transactions view currently shows every transaction in the selected month, which makes it difficult to locate a particular entry or examine a meaningful subset as transaction history grows. Users need to narrow that history by date, category, cash-flow type, and amount without altering their saved transactions.

## What Changes

- Add a text search for transaction descriptions and composable filters for an inclusive date range, expense category, income/expense type, and minimum/maximum amount.
- Let users combine any active search and filters; a transaction appears only when it satisfies every active criterion.
- Provide a clear-filters action and a visible result count so users can recover the unfiltered view and understand the active result set.
- Validate entered date and amount ranges before applying them, with clear feedback for invalid bounds.
- Keep transaction-form validation feedback fully visible by wrapping it within the dialog instead of truncating it.
- Resize a transaction dialog after validation feedback changes so its Save and Cancel controls remain visible.
- Keep related filters legible as the window narrows and coordinate type and category selections so users cannot construct an inapplicable Income-plus-category filter.
- Preserve the existing newest-first ordering and all transaction create, edit, delete, monthly-budget, and net-cash-flow behavior.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `budget-tracking`: Extend transaction-history requirements so a user can search descriptions and filter the displayed transaction set by date range, category, type, and amount.

## Impact

- Affects the JavaFX Transactions view and its table refresh behavior, including the selected-month navigation context.
- Extends transaction retrieval through `BudgetService`, `BudgetDatabase`, and `TransactionRepository`; no change to the persisted transaction schema is expected.
- Requires service, persistence, and JavaFX-facing tests for independent and combined criteria, inclusive bounds, invalid ranges, clearing filters, and unchanged unfiltered behavior.
