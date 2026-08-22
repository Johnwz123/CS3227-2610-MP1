## 1. Transaction query contract

- [x] 1.1 Add an immutable transaction-query value that represents optional description, date, category, type, and amount criteria without coupling it to JavaFX controls.
- [x] 1.2 Extend `BudgetService` with filtered transaction retrieval, normalize blank description input, and reject reversed date or amount bounds while retaining exact `BigDecimal` comparisons.

## 2. SQLite retrieval

- [x] 2.1 Extend `BudgetDatabase` and `TransactionRepository` to accept the transaction query while preserving the existing selected-month retrieval path.
- [x] 2.2 Build the repository query from fixed, parameterized clauses for case-insensitive description matching, inclusive date and amount bounds, category, and type; retain newest-first ordering.
- [x] 2.3 Apply the selected month as the default date range only when both explicit date bounds are absent, and support one-sided or cross-month explicit date searches.

## 3. Transactions view filtering

- [x] 3.1 Add a compact filter panel to `TransactionsView` with description search, start/end date controls, expense-category and type selectors, and minimum/maximum amount controls.
- [x] 3.2 Implement Apply and Clear actions, inline invalid-range feedback, current-result count, and a no-results empty state.
- [x] 3.3 Keep the last successfully applied query while the view is open; re-run it after add, edit, or delete, and restore the selected-month view when filters are cleared.
- [x] 3.4 Preserve existing month navigation, table ordering, type styling, and edit/delete row actions for filtered results.

## 4. Automated coverage

- [x] 4.1 Add repository tests for every independent criterion, inclusive and one-sided date/amount bounds, case-insensitive description matching, combined AND semantics, category-plus-income no-match behavior, and newest-first ordering.
- [x] 4.2 Add service tests for blank-search normalization and invalid date or amount ranges without data changes.
- [x] 4.3 Add JavaFX-facing tests for applying and clearing filters, validation retaining prior results, result counts and empty states, and mutation refreshes under an active query.

## 5. Verification

- [x] 5.1 Run `gradlew.bat spotlessApply` and review the scoped diff for unintended changes.
- [x] 5.2 Run `gradlew.bat check --no-daemon` and resolve all test, coverage, and Java quality failures.
- [x] 5.3 Run `git diff --check` and verify the final implementation and OpenSpec artifacts match the agreed requirements.

## 6. Responsive and coordinated filter controls

- [x] 6.1 Update the filtering artifacts to define grouped responsive layout and Income/Expense-to-category coordination.
- [x] 6.2 Reorder and resize the selector controls, and use grouped wrapping so related controls move to a second row together.
- [x] 6.3 Clear and hide the Expense category selector for Income, and set Expense when the user selects a category.
- [x] 6.4 Add JavaFX-facing coverage for responsive grouping and coordinated selector state, then rerun the full quality gate.

## 7. Fully visible transaction validation feedback

- [x] 7.1 Update the filtering artifacts to require transaction-form validation feedback to wrap without truncation.
- [x] 7.2 Make transaction-form validation feedback shrinkable and wrapping within its dialog.
- [x] 7.3 Add JavaFX-facing coverage for a multi-line validation message, then rerun the full quality gate.

## 8. Enforced validation-message wrapping

- [x] 8.1 Diagnose the ineffective wrapping constraint and strengthen the layout requirement.
- [x] 8.2 Replace the validation label with a finite-width `TextFlow` that forces long messages to wrap.
- [x] 8.3 Require at least two rendered text lines in JavaFX coverage, then rerun the full quality gate.

## 9. Responsive transaction-dialog height

- [x] 9.1 Update the validation-feedback artifacts to keep the button bar visible after the message wraps.
- [x] 9.2 Resize the dialog after validation feedback changes, using the next JavaFX layout pulse.
- [x] 9.3 Add JavaFX coverage for visible Save and Cancel controls after a multi-line validation error, then rerun the full quality gate.

## 10. Complete Set budget validation feedback

- [x] 10.1 Update the artifacts for readable, wrapped validation feedback in the slightly wider Set budget dialog.
- [x] 10.2 Use a finite-width `TextFlow` and post-validation resize in `BudgetDialog`.
- [x] 10.3 Add JavaFX coverage for a wrapped Set budget validation message and visible actions, then rerun the full quality gate.
