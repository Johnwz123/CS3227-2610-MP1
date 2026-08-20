# Budget Tracking

## Purpose

Provide a local JavaFX workspace for recording cash flow, managing monthly expense budgets, and receiving clear budget-status feedback.

## Requirements

### Requirement: Local desktop budget workspace

The system SHALL provide BudgetBot as a JavaFX desktop application for one local user and SHALL persist its budget data locally without requiring an account or an external service.

#### Scenario: User opens a previously created budget

- **WHEN** the user launches BudgetBot after entering transactions and budgets
- **THEN** the application displays the persisted budget data
- **AND** no authentication or network connection is required

### Requirement: Monthly category budgets and global rollover

The system SHALL let the user set a fixed monthly base amount for each expense category. The system SHALL provide one global rollover setting that is disabled by default.

#### Scenario: Rollover is disabled

- **WHEN** a new month begins while rollover is disabled
- **THEN** each category's available amount equals its fixed monthly base amount

#### Scenario: Rollover is enabled

- **WHEN** a new month begins while rollover is enabled
- **THEN** each category's available amount includes its prior month's remaining amount
- **AND** a negative prior remaining amount reduces the new month's available amount

#### Scenario: User changes rollover preference

- **WHEN** the user changes the global rollover setting during a started month
- **THEN** the new setting applies from the next unstarted month
- **AND** calculations for started months remain unchanged

### Requirement: Configurable expense categories

The system SHALL initialize a new budget with sensible default expense categories and SHALL let the user add, rename, and remove categories.

#### Scenario: New budget is initialized

- **WHEN** the user opens BudgetBot with no existing budget data
- **THEN** the category list includes Housing & Utilities, Groceries, Dining, Transport, Health, Entertainment, Shopping, Education, and Miscellaneous

#### Scenario: User removes a category that has expenses

- **WHEN** the user requests removal of a category referenced by expense transactions
- **THEN** the system requires a replacement category
- **AND** reassigns those expenses before completing removal

### Requirement: Income and expense transactions

The system SHALL let the user create, edit, and delete income and expense transactions. Income SHALL affect only overall balance; expenses SHALL require a category and count toward that category's monthly spending.

#### Scenario: User records income

- **WHEN** the user saves a valid income transaction
- **THEN** the overall balance increases by its amount
- **AND** no category budget spending changes

#### Scenario: User records an expense

- **WHEN** the user saves a valid expense transaction with a category and date
- **THEN** the overall balance decreases by its amount
- **AND** the expense increases the matching category's spending for that calendar month

### Requirement: Dashboard budget feedback

The system SHALL show overall balance, recent transactions, and per-category budget progress for the selected month. It SHALL warn at a globally configurable threshold that defaults to 80 percent and SHALL alert at 100 percent or more of a category's available amount.

#### Scenario: Category reaches warning threshold

- **WHEN** category spending reaches or exceeds the configured threshold but is below its available amount
- **THEN** the dashboard marks the category with a warning state

#### Scenario: Category exceeds its available amount

- **WHEN** category spending reaches or exceeds its available amount
- **THEN** the dashboard marks the category as over budget

#### Scenario: User changes warning threshold

- **WHEN** the user saves a valid global warning threshold
- **THEN** dashboard warning states use that threshold for future applicable monthly calculations
