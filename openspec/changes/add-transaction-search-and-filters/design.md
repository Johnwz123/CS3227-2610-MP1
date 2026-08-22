## Context

`TransactionsView` currently renders `BudgetService.transactions(YearMonth)`, which delegates to a repository query restricted to the selected month. The transaction model already supplies every value needed for discovery: date, type, positive `BigDecimal` amount, optional description, and expense category ID. The change must preserve local SQLite storage, newest-first ordering, shared month navigation, and the existing create, edit, and delete workflows.

## Goals / Non-Goals

**Goals:**

- Make transaction history discoverable with description search and composable date, category, type, and amount criteria.
- Support a bounded historical date search while retaining the selected month as the useful unfiltered default.
- Keep monetary comparison exact with `BigDecimal`, validate ranges before querying, and retain the current table ordering and row actions.
- Keep filters visible while editing or deleting a result, then refresh the same filtered result set.

**Non-Goals:**

- Persisting filter choices, changing the SQLite transaction schema, or changing transaction/budget calculations.
- Full-text indexing, saved searches, transaction tagging, sorting controls, export, or filtering dashboard and budget summaries.
- Changing a transaction's data as a consequence of searching or filtering it.

## Decisions

### Use a dedicated immutable transaction-query value

Introduce a domain/service-facing query value that carries optional description text, inclusive start and end dates, category ID, transaction type, and minimum and maximum amounts. `BudgetService` validates it and passes it to `BudgetDatabase` and `TransactionRepository`; the repository builds the `WHERE` clauses and binds every value through a prepared statement.

This keeps UI control state out of persistence and gives service and repository tests one explicit contract. Building a filter predicate in `TransactionsView` after loading transactions was rejected because it cannot efficiently search outside the selected month and would make persistence-level behavior untested.

### Default to the selected month; explicit dates search history

With neither date bound entered, the query uses the active `YearMonth`'s first and last day as its implicit range, preserving today's view. Once the user supplies either date bound, that bound replaces the corresponding implicit limit; the other side is unbounded, so a user can search before or after a date as well as between two dates. All supplied bounds are inclusive.

This makes the existing month selector a predictable starting context without preventing a real date-range search. Restricting explicit dates to the selected month was rejected because it makes a date-range filter misleading and prevents cross-month discovery.

### Apply all active criteria with AND semantics

The repository returns a transaction only when it satisfies every populated criterion. Description matching is case-insensitive substring matching; blank or whitespace-only text is treated as absent. Category filtering applies to expense transactions through their category ID, and the UI prevents the inapplicable Income-plus-category combination by clearing and hiding the category selector when Income is selected. Amount comparisons use inclusive `BigDecimal` bounds.

AND semantics are predictable for a compact filter panel. OR-style filter groups were rejected because they require a more complex query builder and are not part of the requested workflow.

### Use an explicit filter action with recoverable validation

The Transactions view will provide a compact filter panel with search text, two date controls, category and type selectors, and minimum/maximum amount controls. Applying filters validates that the start date is not after the end date and that the minimum amount is not greater than the maximum; invalid input remains visible with inline feedback and leaves the displayed result set unchanged. Transaction-form validation labels are horizontally resizable and wrap when their text exceeds the dialog width, so the user can read the complete corrective action. A clear action resets every criterion and restores the selected-month view. The view shows the current result count and an empty-state message when no transactions match.

An explicit Apply action avoids querying while a user is entering incomplete values and aligns invalid-range behavior with the application's existing inline validation. Immediate queries on every keystroke were rejected as unnecessary churn for a local SQLite query and awkward for partially entered dates and amounts.

### Group responsive controls and coordinate type with category

The filter panel will use two grouped control rows in a wrapping layout: description search plus start/end dates first; Income/Expense, Expense category, and minimum/maximum amounts second. On a wide view the groups share a line; when the available width is insufficient, the second group wraps intact rather than compressing individual selectors. The Income/Expense control appears before the category selector and has a practical minimum width.

Selecting Income clears the selected category and makes its node unmanaged as well as invisible, removing it from the layout. Selecting a category sets the type to Expense. Selecting Expense reveals the category selector but does not invent a category selection. A free-form wrapping flow was rejected because it could split the type from its related category and amount controls unpredictably.

### Preserve query state during table mutations, not across sessions

The view retains the last successfully applied query while it is open. Successful add, edit, and delete actions re-run that query before repopulating the table; changing the shared month changes only the implicit default used after filters are cleared. Query state is not saved in the database or restored after an application restart.

This lets users continue working within a narrowed list without adding user-preference persistence. Persisting searches was rejected because it adds state-management and stale-context concerns without a stated need.

## Risks / Trade-offs

- [Dynamic SQL assembly could introduce unsafe or incorrectly ordered parameters] → Generate only fixed, whitelisted clauses and bind all user values with `PreparedStatement`; test every criterion and combined criteria.
- [Open-ended history queries may return more rows than the current monthly query] → Keep the selected month as the default, preserve newest-first ordering, and avoid loading unrelated rows when bounds or filters are supplied.
- [A category filter can be confusing for income] → Clear and hide it whenever Income is selected, and select Expense whenever a category is chosen.
- [Editing a transaction can make it cease to match] → Re-run the active query after the mutation and update the result count or empty state.
- [Incomplete filter input could replace a useful result set] → Validate on Apply and retain the previous results when validation fails.
- [A long validation message could be clipped in a narrow dialog] → Allow the label to shrink and wrap across lines, with UI coverage for the rendered height.

## Migration Plan

No database migration is required because transaction storage is unchanged. Release with repository and service coverage for query behavior plus JavaFX-facing tests for applying, clearing, and preserving filters during mutations. Rollback consists of reverting the new query path and filter controls; persisted transactions remain compatible.

## Open Questions

None. The proposal's optional usability additions are resolved here as an Apply action, Clear action, result count, empty state, responsive filter groups, and coordinated type/category controls.
