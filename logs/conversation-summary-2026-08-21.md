# BudgetBot Conversation Summary

This is a consolidated, human-readable summary of the development conversation for the
`CS3227-2610-MP1` repository through 27 August 2026. It is not a verbatim transcript. It records
the important requests, decisions, implementation outcomes, validation evidence, and known limits
in chronological order. The earlier files
[`conversation-summary-2026-08-17.md`](conversation-summary-2026-08-17.md) and
[`conversation-summary-2026-08-20.md`](conversation-summary-2026-08-20.md) remain as historical
phase summaries; this file consolidates them with the later dashboard, database-tooling, release,
quality, filtering, and CI discussions.

## Executive summary

BudgetBot evolved from a sparse Java coursework repository into a local JavaFX desktop budget
tracker. It uses Java 25, Gradle, SQLite, layered model/service/persistence/UI code, JUnit,
Mockito, TestFX, Spotless, Checkstyle, PMD, JaCoCo, ArchUnit, SpotBugs/FindSecBugs, Docusaurus,
GitHub Actions, Dependabot, and tag-triggered native release packaging.

The product is intentionally local and single-user. It does not require an account, network
connection, cloud synchronization, bank integration, or an external financial service. User data
is stored at `~/.budgetbot/budgetbot.db` and is separate from installed application files.

The most important product decision was to make the dashboard month-focused:

```text
Net cash flow for the selected month = income in that month - expenses in that month
```

The former lifetime “Overall balance” presentation, global “Recent activity” section, rollover
setting, and carryover behavior were removed. Category budgets are fixed per month, with warning
thresholds and warning/over-budget visual states retained.

The application has repeatable database reset and demo-seed tools, a refactored persistence layer,
transaction search/filtering, documentation, and native installers. Windows and Ubuntu release
jobs run the complete test and JaCoCo gates. The macOS hosted ARM runner exposed an unreliable
TestFX/CGL modal-dialog interaction; the macOS release job therefore performs static checks, test
compilation, Javadoc, and native packaging while the full test gate remains required on Windows and
Ubuntu. macOS DMG packaging also normalizes internal `0.x.y` app versions because `jpackage`
rejects a zero first version component; public tags and asset filenames are unchanged.

## 1. Starting point and initial planning

The repository initially contained coursework documentation and supporting files but did not yet
have the complete Gradle project, application source tree, tests, wrapper, or release workflows.

The first planning request asked for ideas for additional useful features. Feature exploration was
kept separate from committed scope. The concrete work that followed prioritized a usable JavaFX
budget tracker, visible software-engineering practices, reliable tests, and a reviewable release
workflow rather than adding every possible enhancement.

The technology-stack documentation was reorganized into practical sections:

- application development: Java/Javadoc and Gradle/Gradle Wrapper;
- verification and code quality: JUnit/Mockito, Spotless/Google Java Format, Checkstyle, PMD, and
  JaCoCo;
- documentation: Docusaurus and Markdown guides; and
- collaboration and delivery: Git/GitHub, GitHub Actions, and Dependabot.

Optional tools such as SonarCloud, SpotBugs, ArchUnit, PIT, logging frameworks, and dependency
scanning were discussed at different points. SonarCloud was explicitly removed when the user said
they did not want to pay for it. ArchUnit and SpotBugs/FindSecBugs were later approved and
implemented as local/CI quality practices.

## 2. Project foundation, documentation, and CI

The initial Java project added/configured:

- `settings.gradle`, `gradle.properties`, and `build.gradle`;
- a checked-in Gradle Wrapper;
- Java 25 toolchain configuration;
- JUnit Jupiter and Mockito tests;
- Spotless with Google Java Format;
- Checkstyle and PMD rules;
- JaCoCo coverage verification;
- `src/main/java/edu/nus/cs3227/mp1/HelloWorldApp.java` and its tests; and
- `config/checkstyle/checkstyle.xml` and `config/pmd/ruleset.xml`.

The starter application printed `Hello, World!`. The first JUnit 6 run showed that the JUnit
Platform launcher had to be declared explicitly as a test runtime dependency. The starter
constructor and `main` method were then covered so the configured 80% instruction threshold could
pass. PMD findings about the short starter class name and intentional standard output were handled
with a clearer class name and a narrowly scoped suppression. PMD test-source analysis was disabled
because the selected production ruleset produced unsuitable findings for the output-capture
starter test; test compilation, JUnit, Checkstyle, Spotless, and JaCoCo still apply.

`CONTRIBUTING.md` was created with short-lived branches, focused changes, pull-request
expectations, testing/formatting/Javadoc requirements, and Conventional Commits. The allowed
commit types documented there include `feat`, `fix`, `docs`, `test`, `refactor`, `build`, `ci`,
`chore`, and `perf`.

The repository README was expanded with prerequisites, common commands, installation guidance,
database-tool instructions, and release-packaging commands. `.gitignore` was extended for Node,
Docusaurus, and quality-tool output.

### Docusaurus and GitHub Pages

The `website/` Docusaurus site was added for the developer and user guides. The root repository
`README.md` was made the Docusaurus root page with `id: overview` and `slug: /`. The guides have
stable routes `/DeveloperGuide` and `/UserGuide`; the unrelated blank reflections document is not
published. Sidebar IDs were corrected to the repository-docs IDs `docs/developer-guide` and
`docs/user-guide` after the first build reported the available namespaced IDs.

The Pages workflow builds from `website/`, uploads `website/build`, and deploys through the
`github-pages` environment. A repository administrator still needs to choose GitHub Actions as the
GitHub Pages publishing source. The local website command is run from `website/`, not the
repository root.

Node.js was upgraded to 24 in the CI workflow, Pages workflow, package metadata, lockfile root
metadata, README, contributing guide, and developer guide. Transitive lockfile package engines
were left as their own package requirements.

The regular CI workflow runs Gradle quality checks, JaCoCo/Javadoc generation, npm installation,
and the Docusaurus build. It uploads Java quality reports. A Linux failure showed that `gradlew`
was tracked with mode `100644`; CI now runs `chmod +x gradlew` before invoking it. The Windows
workflow continues to use `gradlew.bat`.

### Coverage summary reporting

The user wanted CI coverage output to resemble the generated JaCoCo HTML report instead of a
single percentage. The reusable parser `scripts/summarize_jacoco.py` now reads
`build/reports/jacoco/test/jacocoTestReport.xml` and emits a Markdown table with package rows and a
total row. It includes missed/total and percentage values for instructions and branches, plus
missed/total values for complexity, lines, methods, and classes. CI can write the same summary to
`GITHUB_STEP_SUMMARY` and `GITHUB_OUTPUT`.

The transformation logic was extracted because it is locally testable and reusable. The
pull-request sticky-comment operation stayed inline in `actions/github-script` because it is
tightly coupled to GitHub event context, permissions, and the marker
`<!-- budgetbot-jacoco-coverage -->`. Same-repository pull requests may receive a write; fork pull
requests retain summaries/artifacts without an unsafe write.

### Report-opening shortcuts

Gradle shortcuts were added for the JaCoCo HTML report and Javadoc. They use proper `Exec` tasks
with deferred commands, check that the generated index exists, and select the platform command:

- Windows: `cmd /c start`;
- macOS: `open`; and
- Linux: `xdg-open`.

An early `openJacocoReport` implementation called `exec()` from a plain task and failed with
“Could not find method exec()”. It was corrected to an `Exec` task. The same approach was used for
`openJavadoc`. Stale JaCoCo execution-data warnings were identified as requiring a clean
recompile/test/report run after source changes.

## 3. BudgetBot MVP and product behavior

The starter Hello World app was replaced with BudgetBot, a local JavaFX budget tracker for one
user. The agreed behavior includes:

- default expense categories such as Housing & Utilities, Groceries, Dining, Transport, Health,
  Entertainment, Shopping, Education, and Miscellaneous;
- add, rename, and remove categories;
- reassignment before removing a category referenced by existing data;
- income and expense transactions, with income contributing to cash flow but not requiring an
  expense category;
- fixed monthly category budgets;
- configurable warning thresholds; and
- dashboard, transaction, budget, and settings views.

`BigDecimal` is used for monetary values. The service validates positive transaction amounts,
non-negative budgets, at most two decimal places, valid dates/types/categories, warning thresholds,
and filter bounds before delegating to persistence. SQLite totals are converted to Java monetary
values rather than relying on binary floating-point arithmetic.

The original MVP included an opt-in global rollover setting. The user then asked whether Overall
balance should instead show the selected month’s net balance, followed by the explicit decision:

1. rename/reframe it as month-focused “Net cash flow”;
2. remove rollover; and
3. remove “Recent activity” from the month-oriented dashboard.

The final wording uses `Net cash flow`, not `balance`. The synchronized contract uses selected-month
income minus expenses, fixed monthly category availability, no rollover/carryover, and no
dashboard-wide recent-activity section. Because the app had not been released, the user explicitly
said backward-compatible schema migration was unnecessary. Obsolete fields were removed directly
and a recreated local database is acceptable.

## 4. Application architecture and refactoring

The application follows this flow:

```text
JavaFX UI -> BudgetService -> BudgetDatabase/repositories -> SQLite
```

The user identified `BudgetBotWindow.java` as a god file. It was split into:

- `BudgetBotWindow` for the stage, shell, and navigation;
- `ViewCoordinator` for the selected month, active view, and rerendering;
- `views/` for dashboard, transactions, budgets, settings, and month controls;
- `dialogs/` for transaction, category, and budget dialogs;
- `tables/` for transaction and budget-summary table factories;
- `UiAlerts` for confirmation/information/error dialogs; and
- `MoneyInput` for money parsing and formatting.

The UI package is organized below `budgetbot.ui`. Selected-month navigation refreshes the current
view rather than redirecting to Dashboard. Budget-table status is represented with compact semantic
color classes instead of a redundant Status column: normal, warning, and over-budget remaining
values have distinct styles, while income type/amount are green and expense type/amount are red.

The next god-file concern was `BudgetDatabase`. It became a connection-owning facade over focused
components:

- `CategoryRepository` for category operations and transactional reassignment/removal;
- `SettingsRepository` for global settings;
- `TransactionRepository` for transaction CRUD, filtering, totals, and net cash flow;
- `MonthlyBudgetRepository` for monthly snapshots and base amounts;
- `SchemaInitializer` for repeatable tables/default categories; and
- `PersistenceSupport` for common JDBC-to-`BudgetPersistenceException` conversion.

The facade’s public API remains convenient for the service/UI. A nullable-category mapping defect
was fixed by checking `ResultSet.wasNull()` immediately after reading the category column. A
reopen test verifies custom categories and settings survive close/reopen without duplicate default
seeding.

Public APIs, including `BudgetDatabase` and the extracted persistence classes, were documented with
`@param`, `@return`, `@throws`, resource ownership, validation, persistence, and failure semantics
where relevant. Javadoc generation succeeds; occasional default-constructor comments are
non-fatal warnings.

## 5. Tests and quality practices

Testing expanded from the service layer to model, persistence, service, application bootstrap,
database tools, and JavaFX UI behavior. TestFX uses `ApplicationExtension`, `FxRobot`, and isolated
temporary SQLite files. The UI tests cover navigation, selected-month changes, transaction CRUD,
category creation, budget editing, settings, and inline validation that leaves invalid dialogs open.

The conversation considered whether Hamcrest was needed, whether `FxRobot` was necessary, whether
AssertJ should replace Hamcrest, and whether TestFX includes Hamcrest transitively. The final build
keeps TestFX for JavaFX interactions and an explicit Hamcrest test dependency; AssertJ was not
introduced because existing JUnit assertions and TestFX covered the required checks.

An earlier UI race was addressed with `WaitForAsyncUtils.waitForFxEvents()` after view changes. A
later macOS-specific failure showed that event waits did not make native pointer/modal interaction
reliable on the hosted ARM runner; that incident is documented in the release section below.

ArchUnit was later added with three architecture rules: no top-level package cycles, dependencies
must follow declared layers, and model isolation. The initial use of
`consideringAllDependencies()` reported 1,128 external JavaFX/dependency violations; it was
corrected to `consideringOnlyDependenciesInLayers()`.

SpotBugs with FindSecBugs was added with maximal effort, medium confidence, HTML/XML reports, and
a narrowly scoped `EI_EXPOSE_REP2` suppression for intentional service/UI constructor injection.
The `check` lifecycle runs production SpotBugs. A complete offline gate later passed with zero
SpotBugs findings and ArchitectureTest 3/3.

At different stages, stored reports recorded 29 passing tests with 84.9% line coverage, and later
43 passing tests with approximately 87.9% instruction coverage, 84.5% line coverage, 76.4% branch
coverage, 85.9% method coverage, and 92.9% class coverage. The configured bundle instruction
threshold remains 80%. These figures are historical validation snapshots, not a promise that every
future change has the same result.

## 6. Database reset and demo-data scripts

The user requested two cross-platform test/demo scripts:

1. reset the database; and
2. seed it with deterministic mock data.

The reset requirement was refined so reset leaves the database in the same usable state as first
startup: all tables exist, default settings are present, and default categories are present. It
does not leave a blank schema.

The implementation includes:

- `DatabasePaths` for the default `~/.budgetbot/budgetbot.db` path and normalized explicit paths;
- `DatabaseTool` for reset and deterministic seed operations;
- PowerShell launchers for Windows; and
- POSIX-shell launchers that work on macOS/Linux when invoked with `sh`.

Reset requires literal `RESET` confirmation unless a force option is supplied, removes only the
selected database and matching SQLite sidecars, and recreates the normal schema/default data. Seed
adds fixed current-month categories, budgets, income, and expenses. It refuses to duplicate a
database that already contains transactions. Tests cover path resolution, confirmation safety,
sidecar cleanup, deterministic data, duplicate-seed refusal, and dashboard cash flow.

The README documents both PowerShell and POSIX usage, optional disposable database paths, and the
need to close BudgetBot before operating on SQLite.

## 7. OpenSpec workflow and archived changes

The repo-local OpenSpec root is `openspec/`. The initial MVP was developed through the
spec-driven artifact sequence:

```text
proposal.md -> design.md -> specs/.../spec.md -> tasks.md -> implementation -> verification
```

The user repeatedly sent `continue` and invoked `openspec-continue-change` to advance one artifact
at a time, then invoked `openspec-apply-change` to implement the approved change. The final MVP
verification included the complete Gradle gate and the user’s confirmation that the main JavaFX
workflows worked. The change reached 19/19 tasks, its delta was synchronized into the main
budget-tracking specification, and it was archived.

The following later changes were also completed and archived:

- `2026-08-21-simplify-monthly-dashboard` — selected-month Net cash flow, no rollover/carryover,
  no dashboard recent activity, and direct unreleased schema changes;
- `2026-08-21-add-database-test-scripts` — safe cross-platform reset and deterministic seed tools;
- `2026-08-21-add-desktop-release-packaging` — native installers and GitHub Release automation; and
- `2026-08-23-add-transaction-search-and-filters` — composable transaction query/filtering and
  validation-dialog layout improvements.

Strict OpenSpec validation passed for the relevant changes. The final OpenSpec status after the
archived work reported no active changes. The user specifically requested bulk archival of the
database-script, desktop-packaging, and simplified-dashboard changes, and confirmed that all three
should be archived.

## 8. Transaction search and filters

Transaction history was later extended with an immutable `TransactionQuery` and validated,
parameterized repository SQL rather than UI-only filtering. Supported criteria are:

- case-insensitive description substring;
- inclusive start/end dates, including one-sided and cross-month ranges;
- category;
- income/expense type;
- minimum amount; and
- maximum amount.

Criteria combine with AND semantics. Blank descriptions mean no search criterion. Reversed dates
and invalid amounts are rejected without mutating stored data. When explicit date bounds are absent,
the selected month is the default date range. Income clears/hides the expense-category filter;
choosing a category selects Expense.

The UI gained Apply and Clear actions, result counts, an empty-state message, responsive grouped
controls, and mutation refreshes that preserve the last successful query. Validation text uses
finite-width `TextFlow`; dialogs resize after validation so Save/Cancel remain visible. Tests cover
independent and combined criteria, inclusive bounds, invalid ranges, clearing, no results, and
mutation refreshes.

## 9. Native packaging and release workflow

The user asked whether to ship a JAR and whether builds/uploads to GitHub Releases could be
automated. The chosen approach is native, self-contained installers generated by JDK 25
`jpackage`/`jlink`, with a bundled runtime so end users do not need Java, Gradle, or a terminal.
The supported artifacts are:

- Windows: `BudgetBot-<version>-windows.msi`;
- macOS: `BudgetBot-<version>-macos.dmg`; and
- Debian/Ubuntu Linux: `BudgetBot-<version>-linux.deb`.

`packageAppImage` creates an application image and `packageNative` creates the host-native
installer under `build/packages/`. Windows MSI creation requires WiX Toolset; the release workflow
installs WiX automatically. A full JDK with `jpackage` is required, not only a minimal runtime.

The release workflow in `.github/workflows/release.yml`:

1. triggers on tags matching `v<major>.<minor>.<patch>`;
2. validates and normalizes the tag in a Bash preparation job;
3. packages Windows, macOS, and Ubuntu in a matrix;
4. uploads one native installer artifact per platform; and
5. creates or updates a GitHub Release only after all required package jobs succeed.

The publication job has contents-write permission; package and ordinary CI jobs use read-only
permissions. Packages are currently unsigned, so Windows/macOS trust warnings are expected and the
README tells users to verify that downloads came from the repository’s Releases page.

The README explains installation:

- Windows users open the MSI and launch from the Start menu;
- macOS users open the DMG and drag BudgetBot to Applications; and
- Debian/Ubuntu users open the DEB in the software installer or run
  `sudo apt install ./BudgetBot-<version>-linux.deb`.

Normal upgrades install a newer package over the existing one. Uninstalling the application removes
installed program files but retains `~/.budgetbot/budgetbot.db`, because the data directory is
separate from the package. The conversation did not perform a commit, push, pull request, GitHub
Pages setting change, or external service-account setup automatically.

### Release tag validation correction

The first release workflow snippet had a missing closing `fi` in the Bash tag-validation step. It
produced `syntax error: unexpected end of file`. The conditional was closed and the intended
normalization was verified conceptually: for example, `v1.2.3` produces
`is-release=true`, `tag=v1.2.3`, and `version=1.2.3`.

The user asked where installers appear after completion. They are attached as assets to the GitHub
Release created by the `publish` job, and are also available temporarily as workflow artifacts on
the run.

### macOS TestFX failure

The macOS package run used a hosted ARM JDK and reported repeated
`CGLChoosePixelFormat` warnings. Three TestFX tests failed before packaging:

- `addsEditsAndDeletesTransactionsThroughTheUi`;
- `managesCategoriesBudgetsAndSettingsThroughTheUi`; and
- `validatesTransactionFormWithoutClosingTheDialog`.

The initial failure was an `EmptyNodeQueryException`; adding JavaFX event waits changed the symptom
to a timeout while looking up `.dialog-pane`. The local Windows/JBR experiments also showed that
synchronous `robot.interact(button::fire)` can block when the application handler calls modal
`showAndWait()`. Those attempted approaches were not retained as the release fix.

The final decision was to preserve the full TestFX/JaCoCo gate on Windows and Ubuntu, where the
release jobs were already successful, and make the macOS package step run:

```text
spotlessCheck checkstyleMain checkstyleTest pmdMain compileTestJava javadoc packageNative
```

This still formats/checks the macOS build, compiles test sources, generates Javadoc, and exercises
native packaging, while avoiding the hosted ARM TestFX pointer/modal surface. It does not silently
skip the entire test suite across the release: Windows and Ubuntu must still pass `check` and
JaCoCo before publication.

### macOS `jpackage` version failure

After the TestFX issue was isolated, the macOS package reached `jpackage` but failed for
`-PreleaseVersion=0.0.4` with:

```text
The first number in an app-version cannot be zero or negative.
```

The build now has a `jpackageVersion` helper. Only for DMG packaging, a public `0.x.y` release is
mapped to an internal `1.x.y` app version. Public tags, GitHub Release names, asset filenames, and
Windows/Linux app versions remain unchanged. Thus a `v0.0.4` release still produces
`BudgetBot-0.0.4-macos.dmg`, while its internal macOS bundle version is `1.0.4`.

The local `gradlew.bat help --offline --no-daemon` run successfully loaded the updated Gradle
configuration. Actual DMG creation must be verified on the macOS runner because the development
machine is Windows and its IntelliJ-bundled runtime is not a substitute for the macOS packaging
environment.

## 10. Grader-oriented engineering assessment

The user later asked what could improve the project for a university assessment focused on Software
Engineering Practices. The assessment concluded that the repository already had substantial
visible evidence: OpenSpec artifacts, layered architecture, tests, quality gates, CI, release
packaging, documentation, database tooling, and dependency automation.

The highest-impact remaining evidence was not another feature. It was discoverability and reflection:

- complete `docs/Reflections.md` with decisions, setbacks, trade-offs, retrospective actions, and
  individual contributions;
- add a README-linked requirements-to-evidence page;
- include an architecture diagram and rationale; and
- explain risk-based testing, coverage, quality-tool choices, and known limits.

The recommendation was to make engineering reasoning easy for a grader to find rather than turning
the assessment into uncontrolled feature implementation.

## 11. Prompt and interaction register

The following register preserves the important explicit requests and decisions from the
conversation. Repeated `continue` messages are grouped where they advanced the same OpenSpec
artifact sequence.

### Foundation and quality work

1. Organize the existing Technology Stack into suitable subsections.
2. Create `CONTRIBUTING.md` with Conventional Commits.
3. Create a Hello World Java project using the listed tools and configure GitHub Actions CI/CD.
4. Set up Docusaurus for `DeveloperGuide.md` and `UserGuide.md` on GitHub Pages.
5. Expand the CI’s one-line JaCoCo output into a table similar to the HTML report.
6. Decide whether Python belongs directly in `ci.yml`, then extract the formatter into a repository
   script.
7. Decide whether the pull-request coverage-comment script should also be extracted; retain the
   GitHub API operation inline because of its Actions coupling.
8. Add Gradle shortcuts for opening JaCoCo and Javadoc reports, including Windows and
   macOS/Linux behavior.
9. Diagnose the Gradle `exec()` task failure and the stale JaCoCo execution-data warning.
10. Improve `BudgetDatabase.java` Javadocs, then apply `@param`, `@return`, and `@throws` quality
    across Java files.
11. Remove SonarCloud because the user did not want to pay for it.
12. Make the repository README render as the Docusaurus `/` page.
13. Upgrade the Docusaurus/CI requirement from Node 20 to Node 24.
14. Fix the Linux Actions `./gradlew: Permission denied` failure.
15. Assess the IntelliJ PMD XML schema warnings; distinguish IDE resolution warnings from Gradle
    PMD validity.
16. Add model, persistence, service, application, and UI tests; add TestFX and reach at least 80%
    coverage.
17. Explain Hamcrest, FxRobot, AssertJ, and TestFX transitive dependencies.
18. Assess and implement the `BudgetBotWindow` god-file refactor.
19. Move UI classes into the `budgetbot.ui` package hierarchy.
20. Identify another god file and refactor `BudgetDatabase` into focused repositories.

### Product and OpenSpec work

21. Explore useful additional BudgetBot features.
22. Discuss whether “Overall balance” should instead be a selected-month net balance and whether
    rollover could be removed.
23. Decide explicitly to use month-focused “Net cash flow”, remove rollover, and remove “Recent
    activity” from the month dashboard.
24. Advance the simplified-dashboard OpenSpec proposal/design/spec through repeated `continue`
    and `openspec-continue-change` prompts.
25. Confirm that no backward-compatible database migration was needed because the app was unreleased.
26. Apply the simplified-dashboard change.
27. Create reset and demo-seed database scripts.
28. Make those scripts work on Windows and macOS/Linux.
29. Ensure reset recreates tables, default settings, and default categories.
30. Advance and apply the database-test-scripts OpenSpec change.

### Packaging and release work

31. Discuss JAR distribution versus native installers and automate builds/uploads to GitHub Releases.
32. Explain end-user installation and uninstallation for the Java desktop app.
33. Start and advance the desktop-release-packaging OpenSpec change through proposal and design.
34. Confirm that the installed full JDK 25 was sufficient and approve Java/Gradle command execution
    when needed.
35. Bulk-archive `add-database-test-scripts`, `add-desktop-release-packaging`, and
    `simplify-monthly-dashboard`, then confirm all three should be archived.
36. Fix the release workflow’s missing Bash `fi`.
37. Explain where completed installers can be found on GitHub.
38. Diagnose the macOS hosted ARM TestFX failures and preserve cross-platform release coverage.
39. Diagnose the macOS `jpackage` rejection of `0.0.4` and normalize only the internal DMG version.

### Later application and assessment work

40. Add ArchUnit and SpotBugs/FindSecBugs as approved, CI-enforced quality practices.
41. Add transaction search and composable filters for date range, category, income/expense, amount,
    and description, with usability additions such as Clear, result count, empty state, and
    responsive controls.
42. Fix validation-message wrapping and dialog sizing so Save/Cancel stay visible.
43. Advance, synchronize, validate, and archive the transaction-search-and-filters OpenSpec change.
44. Assess the project from a grader’s Software Engineering Practices perspective and prioritize
    reflections, traceability, rationale, and testing evidence.
45. Create this consolidated Markdown summary in `logs/`.

## 12. Final implementation state and validation boundaries

At the time of this log, the repository contains:

- Java 25 and Gradle Wrapper build configuration;
- JavaFX desktop UI and SQLite persistence;
- layered model/service/repository architecture;
- fixed monthly category budgets and selected-month net cash flow;
- no rollover/carryover and no dashboard recent activity;
- transaction search/filtering;
- cross-platform database reset and deterministic seed tools;
- JUnit/Mockito/TestFX tests;
- Spotless, Checkstyle, PMD, JaCoCo, ArchUnit, and SpotBugs/FindSecBugs quality checks;
- Docusaurus developer/user documentation and GitHub Pages workflow;
- Dependabot configuration;
- GitHub Actions CI;
- Windows MSI, macOS DMG, and Debian/Ubuntu DEB packaging;
- tag-triggered GitHub Release publication; and
- installation/uninstallation documentation in `README.md`.

Important commands documented by the project include:

```text
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
```

On macOS/Linux the equivalent wrapper is `./gradlew`. Native packaging requires a full JDK 25
with `jpackage`; Windows MSI also requires WiX. The normal Windows quality gate is
`gradlew.bat check --no-daemon`.

Validation evidence includes successful local Gradle quality gates at several implementation
stages, strict OpenSpec validation, Docusaurus production builds, test/coverage reports, and
successful local loading of the final packaging configuration. The hosted macOS DMG workflow and
the exact ARM graphics behavior cannot be reproduced on the Windows development machine; the next
macOS tag run is the authoritative verification for those two platform-specific changes.

No automatic commit, push, pull request, GitHub Pages setting change, or external service-account
configuration was performed by this conversation. Existing logs, generated reports, and any local
IDE metadata should be reviewed separately before committing.
