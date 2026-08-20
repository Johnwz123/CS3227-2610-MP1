## MODIFIED Requirements

### Requirement: Monthly category budgets and global rollover

The system SHALL let the user set a fixed monthly base amount for each expense category. Each category's available amount for a calendar month SHALL equal its fixed monthly base amount; the system SHALL NOT provide a rollover preference or carry unspent or overspent amounts into another month.

#### Scenario: A new month starts after any prior-month spending

- **WHEN** the user views a category budget in a new calendar month after the category was under or over its budget in the preceding month
- **THEN** the category's available amount equals its fixed monthly base amount
- **AND** no amount from the preceding month changes that availability

### Requirement: Income and expense transactions

The system SHALL let the user create, edit, and delete income and expense transactions. Income SHALL contribute positively to net cash flow for its transaction month; expenses SHALL require a category, contribute negatively to net cash flow for their transaction month, and count toward that category's monthly spending.

#### Scenario: User records income

- **WHEN** the user saves a valid income transaction
- **THEN** net cash flow for the transaction's calendar month increases by its amount
- **AND** no category budget spending changes

#### Scenario: User records an expense

- **WHEN** the user saves a valid expense transaction with a category and date
- **THEN** net cash flow for the transaction's calendar month decreases by its amount
- **AND** the expense increases the matching category's spending for that calendar month

### Requirement: Dashboard budget feedback

The system SHALL show selected-month net cash flow and per-category budget progress for the selected month. It SHALL NOT show overall balance or recent transactions on the dashboard. It SHALL warn at a globally configurable threshold that defaults to 80 percent and SHALL alert at 100 percent or more of a category's available amount.

#### Scenario: Dashboard shows selected-month net cash flow

- **WHEN** the user views the dashboard for a selected calendar month
- **THEN** it displays that month's income minus that month's expenses as Net cash flow
- **AND** it does not display an all-time overall balance or a Recent activity section

#### Scenario: Category reaches warning threshold

- **WHEN** category spending reaches or exceeds the configured threshold but is below its available amount
- **THEN** the dashboard marks the category with a warning state

#### Scenario: Category exceeds its available amount

- **WHEN** category spending reaches or exceeds its available amount
- **THEN** the dashboard marks the category as over budget

#### Scenario: User changes warning threshold

- **WHEN** the user saves a valid global warning threshold
- **THEN** dashboard warning states use that threshold for future applicable monthly calculations
