# Reflections on AI-assisted software engineering

## Context

BudgetBot is a local, single-user desktop application for recording income and expenses and comparing monthly category spending with fixed budgets. Its interface is built with JavaFX, and its data is stored locally in SQLite. The project is built with Gradle and includes automated unit, persistence, architecture, and user-interface tests; static-analysis and coverage checks; GitHub Actions workflows; native installer packaging; and a Docusaurus documentation site.

Some of the main project-specific terms are:

| Term       | Meaning                                                                                                                 |
|------------|-------------------------------------------------------------------------------------------------------------------------|
| JavaFX     | The Java user-interface framework used to build the desktop application                                                 |
| SQLite     | The embedded database that stores the user's data on their own computer                                                 |
| Gradle     | The build tool used to compile, test, analyse, document, and package the application                                    |
| TestFX     | The framework used to automate interactions with JavaFX controls                                                        |
| OpenSpec   | The repository's specification-driven workflow for recording proposals, designs, requirements, and implementation tasks |
| Docusaurus | The tool used to build the documentation website                                                                        |
| Mermaid    | A text-based notation from which the documentation site renders diagrams                                                |
| Gherkin    | The Given/When/Then language used to express testable acceptance scenarios                                              |

This reflection describes how I used large language models (LLMs) as an engineering assistant. The LLM helped me explore the codebase, generate suggestions and alternatives, draft tests and documentation, and form debugging hypotheses. Importantly, I did not treat it as an authority and I remained responsible for product decisions, implementation boundaries, interpretation of requirements, code review, test selection, and releases.

The prompts below are representative versions of prompts used, edited for clarity and consolidated where several shorter prompts addressed the same problem. I found that the biggest takeaway is not simply what answer the LLM produced, but how the prompt constrained the answer, what assumptions remained, and how I established whether the answer was trustworthy.

## How I evaluated an LLM response

I used a source-first loop:

1. State the current product scope and the files or commands that are relevant.
2. Ask the LLM to clearly separate facts, assumptions, proposed changes, and unresolved open questions.
3. Compare the response with the implementation, tests, build configuration, and specifications.
4. Make the smallest change that satisfies the accepted requirement.
5. Run a targeted check, then the appropriate full build, test, documentation, or packaging command.
6. Inspect the diff and generated reports, and record limitations rather than converting an unverified result into a success claim.

This process mattered because an LLM can produce a plausible explanation that is inconsistent with the latest product model. A prompt was therefore a way of producing hypotheses to investigate, not a substitute for requirements analysis or testing.

## The prompt examples at a glance

| Case study                                     | Concrete software-engineering concepts                                                         | Main lesson                                                                                         |
|------------------------------------------------|------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| Requirements analysis                          | Domain modelling, invariants, non-goals, acceptance criteria, and traceability                 | The LLM could enumerate interpretations, but a human still had to decide what the product meant     |
| Persistence design                             | Layering, information hiding, transactions, failure modes, and test isolation                  | Architectural patterns were useful only when tied to actual risks and regression tests              |
| TestFX debugging                               | Reproducibility, asynchronous UI state, test oracles, and build caching                        | A green command was not evidence until I confirmed that the relevant test had executed              |
| Documentation tooling                          | Docs-as-code, configuration management, parser correctness, and visual review                  | A successful build proved syntactic validity, not diagram readability                               |
| Quality engineering and continuous integration | Risk-based testing, automated quality gates, platform boundaries, and evidence discoverability | Adding tools was less valuable than making existing engineering decisions and evidence reproducible |

## Requirements analysis

### Prompt

```text
Act as a product analyst for the existing BudgetBot repository. Inspect the
README, OpenSpec specifications, model classes, BudgetService, repositories,
views, and tests before proposing implementation work.

Produce a table with:
 - each current user-visible requirement;
 - the domain invariant that makes it testable;
 - the source files that implement or verify it;
 - a Gherkin acceptance scenario; and
 - any contradiction or ambiguity that needs a product decision.

Use these decisions as constraints: the dashboard is scoped to the selected
calendar month; Net cash flow means income minus expenses for that month;
monthly category budgets are fixed snapshots with no rollover or carryover;
the dashboard has no lifetime balance or Recent activity section; and the
application is local and single-user. Do not add accounts, cloud sync, bank
integration, or other features that are not in the current scope.

Before suggesting code, list any older or generic budget-app assumptions that
would produce a different behaviour.
```

### Why I formulated it this way

The prompt deliberately required the LLM to perform requirements analysis before implementation. Naming the relevant repository artifacts and constraints was important because a request such as "build a budget tracker" leaves too much unstated. Earlier, broader requests led to assumptions about accounts, recurring transactions, rollover budgets, lifetime balances, and cloud synchronization. The updated prompt also states negative requirements. These non-goals constrained scope and told the LLM which behaviors should not appear, preventing scope creep.

The other key concept was a precise domain vocabulary. "Net cash flow" is ambiguous until it is defined as selected-month income minus selected-month expenses. A "fixed monthly category budget" means that a previous month cannot silently alter the current month's available amount. Expressing these definitions as invariants and acceptance scenarios gave the interface, service, persistence layer, documentation, and tests one shared contract.

### Assumptions and errors made by the LLM

The LLM initially interpreted the product as a conventional personal-finance application. For example, it sometimes treated net cash flow as an all-time account balance and proposed useful-sounding features outside the intended scope. These were semantic rather than syntactic errors. They were especially risky because the resulting code could be internally clean and tidy while the LLM implements the wrong product.

Asking the LLM to list its assumptions made the disagreement visible, but the final product decisions still required human judgement. I had to decide on the month-scoped model and then check that the decision was reflected consistently in calculations, monthly snapshots, views, user guide, developer guide, and tests.

### Verification and prompt evolution

I verified the response by tracing the calculation from the transaction repository through `BudgetService` to `DashboardView`, then comparing it with the product specification and tests. I checked empty-month behaviour, income with no category, expense category totals, and the absence of rollover and Recent activity. I revised or rejected suggestions from the LLM whenever a claimed behaviour had no matching source path, requirement, or test invariant.

The prompt evolved from asking for "the requirements of a budget tracker" to explicitly naming current product decisions, non-goals, evidence sources, and unresolved ambiguities. Next time, I would first establish the domain glossary and invariants before asking for design suggestions. That would make semantic drift easier to detect and reduce later corrections.

### Engineering judgement and lessons

I found that prompting was least effective when the task was deciding what the product should mean. The LLM could enumerate interpretations and consequences, but it could not choose an interpretation on behalf of the project. Writing the scope decision into the specification was faster and safer than asking for increasingly elaborate generated requirements. The LLM became useful again after that decision existed, because it could help trace the decision across code, tests, and documentation.

## Persistence design and testability

### Prompt

```text
Trace the persistence path of the current BudgetBot implementation from
BudgetService to SQLite. Inspect BudgetDatabase, SchemaInitializer,
CategoryRepository, SettingsRepository, TransactionRepository, and
MonthlyBudgetRepository, together with their tests.

Propose a minimal layered design that:
 - keeps BudgetDatabase as a clear facade over focused repositories;
 - initializes the schema and default categories only when appropriate;
 - preserves user-created categories and settings across close and reopen;
 - uses a transaction for category reassignment and removal;
 - validates money at the service boundary and preserves exact decimal values;
 - gives tests isolated temporary databases; and
 - exposes enough seams for unit and persistence integration tests.

For every recommendation, name the current source file, the invariant it
protects, the failure mode it prevents, and the test that would detect a
regression. Do not introduce an object-relational mapper (ORM), a server,
a network dependency, or an abstraction whose only purpose is to make the
diagram look more elaborate.
```

### Why I formulated it this way

This prompt was designed around architectural responsibilities rather than classes in isolation. A repository can satisfy individual unit tests while still reseeding data on reopen, leaking a connection, or leaving a transaction half-reassigned. Asking for failure modes and regression tests connected design advice to software engineering concepts: separation of concerns, information hiding, transaction atomicity, test isolation, and persistence invariants.

The prompt also constrained the amount of abstraction. LLMs are good at producing a familiar repository or service pattern, but a small desktop application does not automatically benefit from additional interfaces, dependency-injection frameworks, or an ORM. The relevant design question was whether each boundary made the current system easier to reason about and test.

### Assumptions and errors made by the LLM

An initial generic LLM response assumed that a "clean" persistence design should introduce more layers or a general-purpose data-access framework. It also treated database initialization as an implementation detail. In BudgetBot, initialization has observable consequences: default data should be created on first start, while custom categories, settings, budgets, and transactions must neither be duplicated nor lost after reopening.

The LLM also initially overlooked category removal. Removing a category is not a simple deletion when historical expenses refer to it. The operation must reject an invalid removal or reassign affected transactions atomically. Further prompting produced a sensible description, but I still had to inspect the foreign-key and repository behavior and decide which guards belonged in persistence and which belonged in the service layer.

### Verification and prompt evolution

I verified the design by tracing function call paths and running tests against isolated temporary SQLite databases. The evidence included database-reopen tests, category reassignment and removal tests, transaction create/read/update/delete tests, monthly-snapshot tests, and database reset/seed tests. The resulting design uses `BudgetDatabase` as a facade over focused repositories, with schema setup in `SchemaInitializer` and JDBC error conversion in `PersistenceSupport`.

The prompt evolved from "suggest a database architecture" to "trace this existing call graph and preserve these invariants". That change reduced hallucinated APIs and made the LLM output easier to challenge. Next time, I would request a failure-injection matrix alongside the happy path. Connection failure, partial reassignment, duplicate seeding, invalid monetary precision, and reopen-after-close are more revealing design probes to the LLM than another class diagram.

### Engineering judgement and lessons

The LLM could propose boundaries, but I still had to decide whether they were proportionate to BudgetBot. I also decided which operations required a transaction, where validation belonged, and which tests supplied meaningful integration evidence rather than duplicating unit tests. These choices balance risk, scope, maintainability, and the expected lifetime of the product.

## Diagnosing TestFX budget-form failure

### Prompt

```text
Diagnose this exact failure in the current repository. Do not assume that the
JavaFX warning is the cause:

  the query "Set budget" returned no nodes
  at BudgetBotWindowUiTest.java:303
  from validatesBudgetFormWithVisibleFeedbackAndActions

First inspect the test, BudgetBotWindow, ViewCoordinator, BudgetsView,
BudgetSummaryTableFactory, and the Gradle test configuration. Then run the
targeted method using the exact Gradle wrapper command. Also run the
unfiltered test task once normally and once with --rerun-tasks. Compare whether
the task actually executes or reports UP-TO-DATE/FROM-CACHE.

Explain whether this is:
 - a missing product control;
 - a TestFX lookup/synchronization race;
 - a test-order or shared-state problem;
 - a JavaFX module warning; or
 - a Gradle caching/reporting problem.

Do not claim the test passes unless the test method actually executed.
Recommend the smallest deterministic test improvement and identify what still
requires visual or platform-specific verification.
```

### Why I formulated it this way

This prompt deliberately included the stack trace, method name, source files, and reproduction steps. Without those details, an LLM can produce a long list of generic JavaFX suggestions. The prompt also separated five hypotheses that are often conflated: product behavior, UI synchronization, shared state, runtime warnings, and build caching. This is an application of falsification: each possible cause should predict different observable evidence.

### Assumptions and errors made by the LLM

The JavaFX unnamed-module warning looked suspicious, and the LLM's initial reasoning focused on module-path configuration. However, that warning did not explain why TestFX found no matching node. The failure occurs when the helper calls `clickOn`: it searches for a visible node before waiting. `BudgetsView` does create the "Set budget" button, but JavaFX creates visible table cells on demand. Immediately after navigation, the row containing the button may not yet have been laid out and made available to TestFX.

There was a second trap: a normal unfiltered Gradle run could appear green without executing the tests because Gradle reported `FROM-CACHE`. This means Gradle restored an earlier successful test output instead of rerunning the current tests. A filtered invocation has different task inputs, so it bypassed that cached result and exposed the failure. A previous report listing the UI tests as passing was therefore not proof that the current source was reliable.

### Verification and prompt evolution

The targeted method reproduced the `FxRobotException`. The normal unfiltered test task reported `FROM-CACHE`. A forced run with the cache and up-to-date shortcuts disabled executed 43 tests and reported one failure in the targeted UI scenario. Source inspection then showed that the navigation test already waits for a budget-view control, while the failing test goes directly from clicking "Budgets" to clicking "Set budget".

The prompt evolved from "why does this error occur?" into a source-backed experiment comparing targeted, cached, and forced execution. That changed the task from generating an explanation to collecting evidence that could distinguish the hypotheses.

The most direct test improvement is to wait for the actual control before clicking it:

```java
click(robot, "Budgets");
WaitForAsyncUtils.waitFor(
    5, TimeUnit.SECONDS,
    () -> robot.lookup("Set budget").tryQuery().isPresent());
click(robot, "Set budget");
```

A stronger long-term design would give important controls stable semantic IDs and wait for a view-specific readiness condition rather than relying on visible text.

### Engineering judgement and lessons

This was also a case where prompting was less effective than manual execution. The LLM could identify a likely race, but only the Gradle task outcome proved that the unfiltered green build was cached. UI timing, layout pulses, screen dimensions, and platform windowing are difficult to infer from source alone. The required engineering judgement was to avoid treating `BUILD SUCCESSFUL` as proof that the tests ran and to distinguish a test-harness synchronization defect from a missing application feature.

## Docusaurus, Mermaid, and Gherkin documentation

### Prompt

```text
Configure the existing website directory for the current documentation set.
The site uses Docusaurus 3.10.2, serves the repository README and the two
guides from outside website, and must preserve the existing guide URLs.

Make Mermaid fenced blocks render as diagrams and make Gherkin code blocks
use Gherkin keyword highlighting. Show the exact package, configuration, and
lockfile changes and verify the production site with `npm run build`.

Then inspect every Mermaid diagram in DeveloperGuide.md. For each diagram,
check parser-valid syntax, readable node labels, direction, line wrapping,
and whether labels can overlap at the rendered size. Do not claim that a
successful JavaScript build proves visual legibility. If a diagram fails,
give the smallest source-level correction and explain how it was checked.
```

### Why I formulated it this way

This prompt distinguished documentation content from documentation tooling. Mermaid rendering requires Docusaurus configuration and its Mermaid theme, whereas Gherkin highlighting is configured through the Prism syntax highlighter. It also stated the repository layout because the documentation plugin reads the root README and `docs` directory from outside `website`. This prevented a generic solution from moving or duplicating the canonical Markdown files.

Requesting a production build treated documentation as code: dependencies had to be locked, configuration had to compile, and the generated site had to be inspectable. The visual caveat was equally important because build correctness and presentation quality are different forms of evidence.

### Assumptions and errors made by the LLM

The LLM first assumed that enabling a Mermaid flag alone would solve every rendering problem. However, the working configuration also required the Docusaurus Mermaid theme, and the package lock had to agree with `package.json`. Gherkin also had a different path: it remained a fenced code block while Prism supplied keyword highlighting.

The Mermaid parser also rejected a decision-node label containing problematic quoting and punctuation. After that syntax error was corrected, a separate UI-state diagram still rendered with overlapping text. A parser cannot determine whether a human can comfortably read a diagram. Shorter labels, a more suitable left-to-right direction, and simpler edge labels were needed. This demonstrated that syntactic validity and visual usability are separate quality attributes.

### Verification and prompt evolution

I verified the configuration by inspecting `website/docusaurus.config.js`, `website/package.json`, and the lockfile, then running the Docusaurus production build. The build showed that the Mermaid and Gherkin configuration was accepted and that the documentation compiled. However, it did not show that every diagram was readable at every viewport and that still required a browser-based visual review.

The prompt evolved through separate iterations: enable Mermaid, enable Gherkin, repair a Mermaid parse error, repair text overlap, standardize headings and user-story tables, and investigate frontmatter. Separating those concerns made each change easier to review and reduced the risk of an unnecessary wholesale rewrite.

### Engineering judgement and lessons

LLMs were effective at drafting Mermaid syntax and documentation structure, but less effective at judging layout without rendered-page evidence. They also tended to optimise for a generic Docusaurus site rather than the repository's deliberate root-README layout. Preserving the canonical README and stable public URLs required project-specific judgement.

Next time, I would use a documentation acceptance checklist: every diagram must build, every diagram must be visually inspected, every public guide URL must remain stable, and every fenced language used by the guides must render or highlight as intended. A continuous-integration build and a human visual review would then cover complementary risks.

## Quality engineering and continuous integration

### Prompt

```text
Review the current BudgetBot repository against a university assessment of
software-engineering practice. Inspect the implementation, tests, Gradle
configuration, reports, GitHub Actions workflows, OpenSpec artefacts, README,
User Guide, Developer Guide, and Reflections file.

Identify the highest-impact gaps, not just opportunities to add
features. For every recommendation, map it to:
 - a requirement or engineering risk;
 - a repository file or report;
 - an executable verification command;
 - the expected evidence; and
 - any limitation or platform boundary.

Preserve Windows, Ubuntu, and macOS release coverage. Do not introduce paid
services or external infrastructure unless the repository already requires
them. Distinguish a test that ran from a cached test result, and distinguish
static/package verification from TestFX execution on a different operating
system. Prefer risk-based tests, architecture rules, reproducible tooling,
requirements-to-evidence traceability, and reflective documentation over
unjustified feature work.
```

### Why I formulated it this way

The prompt treats software-engineering practice as observable evidence. A tool name is not evidence by itself, so I explicitly required the LLM to identify the configuration, command, report, and decision behind each claim. This mapping encouraged traceability from requirements to implementation and tests. The platform constraint also prevented a convenient but weak solution such as dropping a failing operating-system job.

The prompt also explicitly excludes paid or unnecessary infrastructure. Generic LLM prompts often results in recommendations of hosted quality services even though local Gradle tasks and inspectable reports were more reproducible, required no external credentials, and better matched a small desktop project.

### Assumptions and errors made by the LLM

The LLM initially treated missing or hard-to-find documentation as evidence that the underlying engineering practice did not exist. However,, substantial existing infrastructure was already present, including the Gradle Wrapper, formatting, Checkstyle, PMD, JaCoCo, GitHub Actions, packaging, and tests. The real gap was that this evidence was not sufficiently discoverable without explicit documentation pointing the LLM to where it may find them. That changed the priority from adding unrelated features to improving design rationale, testing strategy, and traceability.

It was also unsafe to infer cross-platform behavior from a Windows run. On the hosted ARM-based macOS runner, TestFX encountered `CGLChoosePixelFormat`, a graphics/window-initialization failure, before `jpackage`, the native-installer packaging tool, ran. Treating this as a packaging defect, or claiming macOS UI compatibility because Windows passed, would be incorrect. The release process therefore needed an explicit platform boundary: run the full TestFX and quality gate where they are reliable while retaining macOS static, compilation, and packaging verification.

The LLM also risked treating quality tools as interchangeable evidence. Static analysis and coverage are useful gates, but they do not prove that every user journey works. Conversely, a passing test task does not prove that it executed if Gradle restored a cached result. The prompt forced these evidence categories to remain separate.

### Verification and prompt evolution

I verified recommendations against `build.gradle`, the configuration directories, continuous-integration workflow definitions, generated reports, and the documentation site. The resulting process included architecture tests, production SpotBugs/FindSecBugs analysis, Checkstyle, PMD, Javadoc, Spotless, JaCoCo coverage verification, repeatable database tooling, and documentation builds. The Developer Guide records the commands and limitations so another person can reproduce the claims.

The prompt evolved from a broad "improve the project for grading" request to an evidence matrix with explicit commands and platform boundaries. This prevented the LLM assessment from turning into unrelated feature implementation. It also produced requirements-to-evidence traceability, which showed why each practice was selected instead of merely listing tools.

### Engineering judgement and lessons

I had to decide which risks justified a gate, whether a static-analysis finding represented a real defect or an intentional dependency-injection pattern, how much coverage was meaningful, and how to document the macOS limitation without hiding a failure. I also chose not to remove macOS release coverage simply because one hosted UI interaction was unreliable. Those decisions balanced defect risk, false positives, platform constraints, grading evidence, and project scope.

Prompting was less effective for deciding what counted as proportionate engineering. A grader persona could produce a long checklist, but manual inspection was necessary to remove duplicate recommendations, distinguish existing evidence from poor discoverability, and keep the deliverable coherent. Next time, I would start with a manually verified risk register and ask the LLM to challenge it rather than asking it to generate the register from nothing.

## Cross-cutting lessons

| Prompting practice                                   | What it improved                               | What could still fail                                      | Engineering control                                           |
|------------------------------------------------------|------------------------------------------------|------------------------------------------------------------|---------------------------------------------------------------|
| Give the model repository paths and current scope    | Source navigation and relevance                | The model may still infer behaviour from names             | Trace the actual call path and cite the source                |
| State negative requirements and non-goals            | Scope control and test design                  | The model may resurrect an older product decision          | Maintain a current glossary and acceptance specification      |
| Request assumptions and contradictions               | Early discovery of semantic drift              | The model may present a guess as a fact                    | Require facts, hypotheses, and decisions in separate sections |
| Ask for a test and expected evidence for each change | Reproducibility and traceability               | A command may pass from cache or skip a path               | Inspect task outcomes and use forced execution when needed    |
| Ask for diagrams, tables, or code drafts             | Communication speed and breadth                | Generated output may be syntactically valid but unreadable | Build, render, inspect, and review the result                 |
| Preserve platform constraints in the prompt          | Less destructive continuous-integration advice | The model may generalise from one operating system         | Validate each platform separately and document boundaries     |

Overall, I found that the most useful prompt constraints were operational rather than stylistic. "Be detailed" mainly increased the length of a response, while instructions such as "inspect these files", "preserve these invariants", and "state what this command does and does not do" made the response more verifiable.

## What I would do differently next time

I would improve my process in five ways:

- Maintain a short decision log before implementation, including the current domain glossary, non-goals, and superseded decisions.
- Write the acceptance scenarios and evidence links before asking for implementation code. This would reduce semantic drift and make review more objective.
- Include the exact environment and cache policy in debugging prompts. For Gradle, I would distinguish normal incremental execution from forced execution at the start.
- Treat documentation and diagrams as two-stage artifacts: compile-time verification followed by a human readability review. I would also record the review status explicitly instead of treating a successful build as visual proof.

I would continue using LLMs for repository search, generating alternatives, enumerating test cases, explaining code, and preparing first drafts. I would rely on them less for making final product, architecture, or visual-quality judgments and decisions.

## Overall reflection

AI assistance increased the project's breadth and iteration speed, particularly when the task involved connecting many files or producing a first version of a test strategy, diagram, guide, or quality checklist. It also made hidden assumptions easier to expose when I instructed the LLM to list contradictions and failure modes.

The main risk was false confidence. A coherent explanation can still be based on an old requirement; a passing Gradle command can restore a cached result; a valid Mermaid graph can remain unreadable; and a Windows test run cannot establish macOS TestFX behavior. The developer's role was therefore not reduced to accepting generated code. It shifted towards directing the investigation, deciding what evidence was sufficient, and rejecting plausible but unsupported conclusions.

The lasting lesson is that prompting is most valuable when it is embedded in a disciplined software-engineering process. A good prompt narrows the search space and asks useful questions, but source tracing, requirements decisions, tests, reports, human review, and honest limitation reporting are what turn an LLM suggestion into reliable engineering evidence.
