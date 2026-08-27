# BudgetBot Development Conversation Summary

This is the current consolidated summary of the development conversation for the
CS3227-2610-MP1 repository, through 27 August 2026. It is a human-readable summary,
not a verbatim transcript. It records substantive prompts, decisions, implementation
outcomes, validation evidence, and known limitations.

Earlier phase logs remain available:

- [conversation-summary-2026-08-17.md](conversation-summary-2026-08-17.md)
- [conversation-summary-2026-08-20.md](conversation-summary-2026-08-20.md)
- [conversation-summary-2026-08-21.md](conversation-summary-2026-08-21.md)

Those logs contain additional detail for their phases. This file consolidates that
history and adds the most recent quality-tool discussion and this log-generation
request. Dates describe development phases and do not claim an exact timestamp for
every prompt.

## Executive summary

BudgetBot evolved from a sparse Java coursework starter into a local JavaFX desktop
budget tracker for one user. The current application uses Java 25, Gradle 9.7.0,
SQLite, layered model/service/persistence/UI code, JUnit 6, Mockito, TestFX, Spotless,
Checkstyle, PMD, JaCoCo, ArchUnit, SpotBugs with FindSecBugs, Docusaurus, GitHub
Actions, Dependabot, and native release packaging.

The product is intentionally local and single-user. It does not require an account,
network connection, cloud synchronization, bank integration, or an external financial
service. User data is stored separately from installed application files at
~/.budgetbot/budgetbot.db.

The final product model is month-focused:

~~~text
Selected-month net cash flow = income in that month - expenses in that month
~~~

Category budgets are fixed per month. Rollover and carryover were removed, and the
dashboard no longer includes lifetime Overall balance or a dashboard-wide Recent
activity section. Warning and over-budget states remain visible in budget tables.

The repository also contains repeatable database reset/seed tools, transaction search
and filtering, documentation, CI quality reports, and Windows/macOS/Linux packaging.
The latest local verification recorded 43 passing tests, zero SpotBugs/FindSecBugs
findings, all three ArchUnit tests passing, and a successful Gradle check.

## 1. Foundation and project-management practices

The repository initially contained coursework documentation but not the complete
Gradle project, application source tree, tests, wrapper, or CI configuration. The first
request was to organize the technology-stack documentation, create a Conventional
Commits contribution guide, build a Hello World Gradle project, configure GitHub
Actions, and publish developer and user guides with Docusaurus and GitHub Pages.

The foundation added or configured:

- Java 25 and a checked-in Gradle Wrapper; the current wrapper is Gradle 9.7.0.
- settings.gradle, gradle.properties, build.gradle, wrapper files, and quality config.
- Java entry-point and JUnit/Mockito tests.
- Explicit JUnit Platform launcher support after the first JUnit 6 run required it.
- An 80% JaCoCo instruction-coverage gate connected to check.
- Spotless with Google Java Format, Checkstyle, PMD, and JaCoCo.
- CONTRIBUTING.md with focused branches, review expectations, verification commands,
  and Conventional Commits.
- A Docusaurus site under website/, GitHub Pages deployment, and Dependabot.

SonarCloud and other optional tools were discussed. SonarCloud was removed after the
user said they did not want to pay for it. PIT and SLF4J/Logback were discussed but
were not added. ArchUnit and SpotBugs/FindSecBugs were later approved and implemented.

## 2. Documentation, CI, and report workflow

Docusaurus was refined so the root README renders at /, DeveloperGuide.md renders at
/DeveloperGuide, UserGuide.md renders at /UserGuide, and the blank reflections file
is not published. Sidebar IDs were corrected to the repository-docs IDs.

The Node requirement was upgraded from Node 20 to Node 24 in CI, Pages, package
metadata, and documentation. A Linux Actions run showed that the Windows-created
gradlew lacked the executable bit; CI now runs chmod +x gradlew before invoking it.
Windows continues to use gradlew.bat.

The regular CI workflow installs JDK 25 and Node 24, runs Java quality, coverage, and
Javadoc tasks, builds the Docusaurus site, and uploads reports. Ordinary CI is
read-only; release publication is kept in a separate workflow with release-write
permission.

### JaCoCo summary and pull-request reporting

The user reported that CI printed only a one-line coverage percentage and asked for a
table similar to the JaCoCo HTML report. scripts/summarize_jacoco.py now parses
build/reports/jacoco/test/jacocoTestReport.xml and emits package rows and a total row.
It reports instruction/branch missed, total, and percentages, plus complexity, line,
method, and class totals.

The parser can write to GITHUB_STEP_SUMMARY and GITHUB_OUTPUT. The sticky pull-request
comment uses the same summary and the marker
<!-- budgetbot-jacoco-coverage -->. The parser was extracted for reuse and local
testing; the GitHub API operation remains inline because it depends on Actions
context, permissions, and the pull-request event. Same-repository pull requests may
receive a write; fork pull requests retain summaries and artifacts without one.

The upload-artifact action was updated to a Node 24-compatible release. Live GitHub
comment behavior was kept separate from local validation because it requires a real
same-repository pull request.

### Report-opening shortcuts

openJacocoReport and openJavadoc are proper Gradle Exec tasks. They depend on report
generation, verify that the HTML index exists, and use cmd /c start on Windows, open
on macOS, or xdg-open on Linux.

An early implementation called Gradle exec() inside a plain task and failed with
Could not find method exec() on task :openJacocoReport. It was corrected to an Exec
task. Stale JaCoCo execution-data warnings were identified as requiring recompilation
and a fresh test/report run after source changes.

## 3. BudgetBot behavior and product decisions

The Hello World starter was replaced with a JavaFX desktop budget tracker. Agreed
behavior includes:

- seeded categories: Housing & Utilities, Groceries, Dining, Transport, Health,
  Entertainment, Shopping, Education, and Miscellaneous;
- adding, renaming, and removing categories;
- reassignment before removing a category referenced by existing data;
- income and expense transactions, with income affecting cash flow but not requiring
  an expense category;
- fixed monthly category budgets;
- configurable spending-warning thresholds; and
- dashboard, transactions, budgets, and settings views.

BigDecimal is used for money. BudgetService validates positive transaction amounts,
non-negative budgets, two-decimal precision, dates, transaction types, categories,
warning thresholds, and filter bounds before delegating to persistence. SQLite totals
are converted to Java monetary values instead of using binary floating point.

The original MVP included an opt-in global rollover setting. The user then questioned
whether the dashboard should show selected-month net balance instead of lifetime
Overall balance and whether rollover was necessary. The final decisions were to use
month-focused Net cash flow, compute selected-month income minus expenses, remove
rollover/carryover, and remove Recent activity. Because the application was unreleased,
the user accepted direct removal of obsolete behavior instead of a compatibility
migration.

## 4. Architecture and refactoring

The application flow is:

~~~text
JavaFX UI -> BudgetService -> BudgetDatabase/repositories -> SQLite
~~~

BudgetBotWindow was identified as a god file and split into:

- BudgetBotWindow for the stage, shell, and navigation;
- ViewCoordinator for active view, selected month, and rerendering;
- ui/views/ for dashboard, transactions, budgets, settings, and month controls;
- ui/dialogs/ for transaction, category, and budget dialogs;
- ui/tables/ for transaction and budget-summary table factories;
- UiAlerts for confirmation/information/error dialogs; and
- MoneyInput for money parsing and formatting.

Selected-month navigation refreshes the current tab instead of redirecting to
Dashboard. Budget status uses semantic normal, warning, and over-budget CSS classes
instead of a redundant Status column; income is green and expense is red.

BudgetDatabase was then refactored into a connection-owning facade over:

- CategoryRepository for category operations and reassignment/removal;
- SettingsRepository for global settings;
- TransactionRepository for CRUD, filtering, totals, and net cash flow;
- MonthlyBudgetRepository for monthly snapshots and base amounts;
- SchemaInitializer for repeatable schema/default data; and
- PersistenceSupport for common JDBC-to-BudgetPersistenceException conversion.

The nullable-category mapping defect was fixed by checking ResultSet.wasNull()
immediately after reading the category column. A reopen test verifies that custom
categories and settings survive close/reopen without duplicate default seeding.

Public APIs received @param, @return, and @throws documentation plus resource,
validation, persistence, and failure semantics where relevant. Javadoc succeeds;
occasional default-constructor comments were recorded as non-fatal warnings.

## 5. Testing and coverage

Testing expanded from the service layer to model, persistence, service, application
bootstrap, database tools, and JavaFX UI behavior. TestFX uses
ApplicationExtension, FxRobot, and isolated temporary SQLite files. UI tests cover
navigation, selected-month changes, transaction CRUD, category creation, budget
editing, settings, and invalid-dialog behavior.

The conversation considered Hamcrest, FxRobot, AssertJ, and TestFX transitive
dependencies. The final build keeps TestFX for JavaFX interaction and an explicit
Hamcrest dependency. AssertJ was not introduced because existing JUnit assertions
and TestFX were sufficient.

WaitForAsyncUtils.waitForFxEvents() made one navigation assertion reliable. A later
hosted macOS ARM run showed native TestFX pointer/modal interaction can still fail;
that limitation is kept separate from the Windows/Ubuntu full test gate.

The latest local test XML contains 43 passing tests:

| Test class | Tests | Failures | Errors |
| --- | ---: | ---: | ---: |
| ArchitectureTest | 3 | 0 | 0 |
| BudgetBotAppTest | 1 | 0 | 0 |
| ModelTest | 4 | 0 | 0 |
| BudgetDatabaseTest | 6 | 0 | 0 |
| BudgetServiceTest | 14 | 0 | 0 |
| DatabaseToolTest | 4 | 0 | 0 |
| BudgetBotWindowUiTest | 8 | 0 | 0 |
| UiSupportTest | 3 | 0 | 0 |
| Total | 43 | 0 | 0 |

The latest JaCoCo XML records 4,224/4,807 instructions (87.9%), 877/1,038 lines
(84.5%), 172/225 branches (76.4%), 201/234 methods (85.9%), and 39/42 classes
(92.9%) covered. The configured bundle instruction threshold remains 80%.

## 6. Database tooling and transaction search

The user requested repeatable reset and deterministic demo-seed tools. Windows
PowerShell and POSIX-shell launchers were added. Reset requires confirmation unless
-Force is used, removes SQLite sidecars, recreates tables/default settings/categories,
and accepts a disposable database path. Seed adds fixed current-month budgets, income,
and expenses. The README says to close BudgetBot before either operation.

Transaction filtering was implemented through immutable TransactionQuery and validated,
parameterized repository SQL rather than UI-only filtering. Filters use AND semantics,
case-insensitive description substring matching, inclusive date/amount bounds, exact
BigDecimal comparisons, newest-first ordering, and selected-month defaults when dates
are absent. Income clears or hides category; choosing a category selects Expense.

The UI gained Clear/reset behavior, result counts, an empty state, and responsive
grouped controls. TextFlow and post-update Platform.runLater/sizeToScene() handling
keep transaction and 320px budget-dialog validation text and Save/Cancel controls
visible. The completed transaction-search OpenSpec change was synchronized, strictly
validated, and archived.

## 7. Native packaging and releases

The conversation compared JAR distribution with native installers and selected
automated native packaging. The release workflow validates a v<major>.<minor>.<patch>
tag, packages Windows, macOS, and Ubuntu in a matrix, uploads one installer per
platform, and creates or updates a GitHub Release after required jobs succeed.

The README explains Windows MSI installation, macOS DMG installation, and Debian/
Ubuntu DEB installation. Installed program files and user data are separate, so
upgrades retain ~/.budgetbot/budgetbot.db and normal uninstallation does not remove
that data. Packages are unsigned and trust warnings are expected.

The first release workflow had a missing Bash fi; it was corrected. A hosted macOS
ARM run exposed CGLChoosePixelFormat warnings and TestFX modal/pointer failures.
Windows and Ubuntu retain full check/JaCoCo; macOS packaging uses static checks,
test compilation, Javadoc, and packaging. Additional event waits and synchronous
robot.interact(button::fire) did not resolve the modal interaction.

macOS jpackage rejected -PreleaseVersion=0.0.4 because its internal app version
cannot start with zero. Only the internal DMG app version maps 0.x.y to 1.x.y;
public tags, release names, asset names, and Windows/Linux versions stay unchanged.

## 8. Grader-oriented engineering assessment

The user asked what could improve the project for a university grade focused on
Software Engineering Practices. The assessment found visible evidence in OpenSpec
artifacts, layered architecture, tests, static checks, coverage, CI, release
packaging, documentation, database tooling, and dependency automation.

The highest-impact remaining evidence was discoverability and reflection:

- complete docs/Reflections.md with decisions, setbacks, trade-offs, retrospective
  actions, and individual contributions;
- add a README-linked requirements-to-evidence page;
- include an architecture diagram and rationale; and
- explain risk-based testing, coverage, tool choices, and known limits.

The recommendation was to make engineering reasoning easy for a grader to find,
rather than turning assessment work into uncontrolled feature implementation.

## 9. Static-analysis and architecture-tool implementation

The user proposed SpotBugs with FindSecBugs, ArchUnit, PIT, and SLF4J/Logback.
The assessment recommended ArchUnit and SpotBugs/FindSecBugs first, selective
logging if operational diagnostics were needed, and PIT later or as a scheduled
report because mutation testing is slower and needs a meaningful baseline.
There were no Java application/test println calls at the time.

The user approved ArchUnit and SpotBugs with FindSecBugs. The implementation added:

- SpotBugs Gradle plugin 6.5.6 and engine 4.10.3;
- FindSecBugs plugin 1.14.0;
- ArchUnit JUnit 5 dependency 1.4.2;
- maximal SpotBugs effort and medium confidence reporting;
- required HTML/XML reports under build/reports/spotbugs/;
- production spotbugsMain in the check lifecycle; and
- a disabled spotbugsTest task to keep the production gate focused.

ArchitectureTest verifies no top-level package cycles, declared layer dependencies,
and model isolation. Persistence may use model, service may use model/persistence,
UI may use model/service/persistence, and tools may use model/service/persistence.
BudgetBotApp remains the composition root outside the layer graph.

The first ArchUnit implementation used consideringAllDependencies() and reported
1,128 false violations from Java, JavaFX, and external classes. It was corrected to
consideringOnlyDependenciesInLayers().

The first SpotBugs scan found two actionable database issues: an unclosed PRAGMA
Statement and SQL category seeding assembled through string concatenation. The
statement is now try-with-resources, and category seeding uses a parameterized
PreparedStatement.

SpotBugs also reported EI_EXPOSE_REP2 for intentionally retained service/UI
constructor collaborators. config/spotbugs/exclude-filter.xml narrowly suppresses
only those reviewed constructor-injection findings; persistence and security
findings remain in scope.

The first corrected build also exposed a Groovy enum-resolution detail: the
configuration must use Effort.valueOf('MAX') and Confidence.valueOf('MEDIUM').

## 10. Prompt and interaction register

Repeated continuation prompts are grouped when they advanced the same artifact.

### Foundation, quality, and documentation

1. Organize the existing Technology Stack into suitable subsections.
2. Create CONTRIBUTING.md with Conventional Commits.
3. Create a Hello World Java project and configure GitHub Actions CI/CD.
4. Set up Docusaurus for DeveloperGuide.md and UserGuide.md on GitHub Pages.
5. Expand one-line JaCoCo output into a table similar to the HTML report.
6. Decide whether Python belongs directly in ci.yml, then extract the formatter.
7. Decide whether the pull-request coverage-comment script should also be extracted;
   retain the GitHub API operation inline.
8. Add cross-platform Gradle shortcuts for JaCoCo and Javadoc reports.
9. Diagnose the Gradle exec() failure and stale JaCoCo execution-data warning.
10. Improve BudgetDatabase Javadocs, then add API documentation across Java files.
11. Remove SonarCloud because the user did not want to pay for it.
12. Make README render as the Docusaurus root page.
13. Upgrade Docusaurus/CI from Node 20 to Node 24.
14. Fix Linux Actions gradlew permission failure.
15. Assess IntelliJ PMD XML warnings versus Gradle PMD validity.
16. Add model, persistence, service, application, and UI tests with at least 80%
    coverage.
17. Explain Hamcrest, FxRobot, AssertJ, and TestFX transitive dependencies.
18. Assess and implement the BudgetBotWindow god-file refactor.
19. Move UI classes into the budgetbot.ui package hierarchy.
20. Refactor BudgetDatabase into focused repositories.

### Product and OpenSpec

21. Explore useful additional BudgetBot features.
22. Discuss selected-month net balance versus Overall balance and rollover.
23. Decide on month-focused Net cash flow, no rollover, and no Recent activity.
24. Advance the simplified-dashboard OpenSpec artifacts.
25. Confirm that an unreleased app did not need a backward-compatible migration.
26. Apply the simplified-dashboard change.
27. Create reset and demo-seed database scripts.
28. Make the scripts work on Windows and macOS/Linux.
29. Ensure reset recreates tables, default settings, and default categories.
30. Advance and apply the database-test-scripts OpenSpec change.

### Packaging and release

31. Compare JAR distribution with native installers and automate GitHub Releases.
32. Explain end-user installation and uninstallation.
33. Advance the desktop-release-packaging OpenSpec change.
34. Confirm the full JDK 25 and approve Java/Gradle command execution.
35. Bulk-archive the database-script, release-packaging, and dashboard changes.
36. Fix the release workflow's missing Bash fi.
37. Explain where completed installers appear on GitHub.
38. Diagnose macOS hosted ARM TestFX failures while preserving release coverage.
39. Diagnose macOS jpackage rejection of 0.0.4 and normalize only internal DMG
    versioning.

### Application, assessment, and current tooling

40. Add ArchUnit and SpotBugs/FindSecBugs as approved CI quality practices.
41. Add transaction search and composable date/category/type/amount/description
    filters, with Clear, result count, empty state, and responsive controls.
42. Fix validation-message wrapping and dialog sizing so Save/Cancel remain visible.
43. Synchronize, validate, and archive transaction-search-and-filters OpenSpec.
44. Assess the project from a grader's Software Engineering Practices perspective.
45. Assess SpotBugs/FindSecBugs, ArchUnit, PIT, and SLF4J/Logback as additions.
46. Approve implementation of ArchUnit and SpotBugs with FindSecBugs.
47. Implement dependencies, architecture rules, reports, documentation, and fixes.
48. Correct the initial ArchUnit external-dependency false-positive configuration.
49. Correct JDBC resource, SQL-construction, and constructor-injection findings.
50. Record the successful quality gate and zero-finding report.
51. Request an accurate Markdown summary in a new logs file.

## 11. Final state and validation boundaries

The repository contains Java 25/Gradle Wrapper configuration, JavaFX UI, SQLite
persistence, layered model/service/repository code, fixed monthly budgets,
selected-month net cash flow, no rollover/carryover, transaction search/filtering,
database reset/seed tools, JUnit/Mockito/TestFX tests, Spotless, Checkstyle, PMD,
JaCoCo, ArchUnit, SpotBugs/FindSecBugs, Docusaurus guides, GitHub Actions,
Dependabot, native installers, and tag-triggered releases.

Important commands include:

~~~text
gradlew.bat run
gradlew.bat check
gradlew.bat spotlessApply
gradlew.bat spotbugsMain
gradlew.bat jacocoTestReport
gradlew.bat openJacocoReport
gradlew.bat javadoc
gradlew.bat openJavadoc
gradlew.bat packageAppImage "-PreleaseVersion=1.2.3"
gradlew.bat packageNative "-PreleaseVersion=1.2.3"
~~~

On macOS/Linux, use ./gradlew. Native packaging requires a full JDK 25 with
jpackage; Windows MSI also requires WiX. The normal Windows gate is
gradlew.bat check --no-daemon.

The final quality-tool validation was:

~~~text
.\gradlew.bat check --offline --no-daemon --console=plain
~~~

It completed with BUILD SUCCESSFUL. The latest test XML contains 43 tests with no
failures or errors; ArchitectureTest is 3/3; and
build/reports/spotbugs/main.xml contains zero findings. git diff --check completed
without whitespace errors, with only normal Git LF/CRLF conversion advisories.

No automatic commit, push, pull request, GitHub Pages setting change, external
service-account setup, or unexercised live GitHub API claim was made. The pre-existing
untracked .idea/openspec.xml was left untouched. Generated reports and local IDE
metadata should be reviewed separately before committing.
