## ADDED Requirements

### Requirement: Transaction history search and filtering

The system SHALL let the user apply optional transaction-description search, date-range, expense-category, transaction-type, minimum-amount, and maximum-amount criteria in the Transactions view. The system SHALL treat a blank description search as absent, match a non-blank description as a case-insensitive substring, and return only transactions that satisfy every active criterion. Date and amount bounds SHALL be inclusive. The system SHALL preserve newest-first transaction ordering and existing transaction actions for filtered results. The Income/Expense selector SHALL precede the Expense category selector and have a usable minimum width. When the available view width cannot accommodate every control in one row, the system SHALL keep description and date controls together and move the type, category, and amount controls as a group to the following row.

#### Scenario: User applies combined search and filters

- **WHEN** the user applies an expense type, a Groceries category, a description search of `market`, an inclusive date range, and inclusive minimum and maximum amounts
- **THEN** the system displays only Groceries expense transactions whose descriptions contain `market` regardless of case and whose date and amount fall within every supplied bound
- **AND** the table retains newest-first ordering and its edit and delete actions

#### Scenario: User searches with an explicit date bound outside the selected month

- **WHEN** the user applies a start date or end date without the other bound
- **THEN** the system searches transaction history on the supplied inclusive side of that date without restricting results to the selected month

#### Scenario: User applies no date criterion

- **WHEN** the user applies search or non-date filters with both date bounds empty
- **THEN** the system restricts results to the currently selected calendar month

#### Scenario: User selects Income

- **WHEN** the user selects Income in the Income/Expense selector
- **THEN** the system clears any selected expense category and removes the Expense category selector from the filter layout

#### Scenario: User selects an expense category

- **WHEN** the user selects an Expense category
- **THEN** the system sets the Income/Expense selector to Expense
- **AND** the Expense category selector remains visible

#### Scenario: Filter controls wrap at a constrained width

- **WHEN** the Transactions view is too narrow to display every filter control on one row
- **THEN** the description search and date controls remain together on the first row
- **AND** the Income/Expense selector, Expense category selector, and amount controls move together to the next row

### Requirement: Filter feedback and recovery

The system SHALL validate filter bounds before replacing the displayed transaction result set. The system SHALL reject a start date after the end date and a minimum amount greater than the maximum amount, retain the previous result set, and present inline feedback. Any inline transaction-form validation feedback SHALL wrap within its dialog so its complete message remains visible. The system SHALL provide an action that clears every search and filter criterion, restores the selected-month transaction view, and reports the resulting count. The system SHALL report the count of the currently displayed results and an empty state when no transaction matches.

#### Scenario: User applies an invalid filter range

- **WHEN** the user applies a start date after the end date or a minimum amount greater than the maximum amount
- **THEN** the system presents inline validation feedback
- **AND** the previously displayed transaction result set remains unchanged

#### Scenario: Transaction-form validation message exceeds one line

- **WHEN** transaction-form validation produces a message wider than the dialog
- **THEN** the dialog wraps the message across additional lines
- **AND** the complete validation message remains visible
- **AND** the dialog grows to keep its Save and Cancel controls visible

#### Scenario: Budget-form validation message exceeds one line

- **WHEN** Set budget validation produces a message wider than the dialog
- **THEN** the dialog uses a wider readable content area and wraps the message across additional lines
- **AND** the complete message and its Save and Cancel controls remain visible

#### Scenario: User clears applied filters

- **WHEN** the user clears active search or filter criteria
- **THEN** the system removes every criterion and displays all transactions in the currently selected calendar month
- **AND** the displayed result count reflects the restored result set

#### Scenario: Search returns no transactions

- **WHEN** the user applies valid criteria that match no transaction
- **THEN** the system displays an empty state and reports a result count of zero
