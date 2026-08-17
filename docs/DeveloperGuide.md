---
id: developer-guide
slug: /DeveloperGuide
---

# Developer Guide

## Technology Stack

### Application Development

- **Language and API documentation**: Java 25 and Javadoc
- **Build and dependency management**: Gradle 9, using the Gradle Wrapper
- **Desktop UI**: JavaFX Controls
- **Local persistence**: SQLite through the Xerial JDBC driver

### Verification and Code Quality

- **Unit testing and mocking**: JUnit 6 and Mockito
- **Code formatting**: Spotless with Google Java Format
- **Style linting**: Checkstyle
- **Static code analysis**: PMD
- **Code coverage**: JaCoCo

### Documentation

- **Documentation site generator**: Docusaurus
- **Source guides**: Markdown files in the repository `docs` directory

### Collaboration and Delivery

- **Version control and hosting**: Git and GitHub
- **Continuous integration and deployment**: GitHub Actions
- **Automated dependency updates**: Dependabot

## Local Development

Install JDK 25 and use the Gradle Wrapper; no system Gradle installation is required.
Run `gradlew.bat check` on Windows (or `./gradlew check` on macOS/Linux) before opening a pull request.

For the documentation site, install Node.js 24 or later, run `npm install` in `website`, then run `npm start`.

## System Overview

BudgetBot uses a layered JavaFX design: views call `BudgetService`, which owns validation and monthly
calculations, and `BudgetDatabase`, which owns the SQLite schema and queries. The default application
database is `~/.budgetbot/budgetbot.db`; tests create an isolated temporary database. Monetary values
are represented with `BigDecimal`, never floating point.

The database initializer is repeatable and seeds default expense categories only for a new database.
Monthly budget snapshots retain the base amount, rollover policy, and warning threshold used for that
month, so later setting changes do not rewrite historical dashboard calculations.
