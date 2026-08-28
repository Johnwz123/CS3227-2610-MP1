# Reflections on AI-assisted software engineering

## Context

This reflection describes how I used large language models (LLMs) as an engineering assistant during the project. The LLM was useful for exploring a large codebase, generating alternatives, drafting tests and documentation, and suggesting debugging hypotheses. Importantly, it was not treated as an authority and I remained responsible for the product decisions, implementation boundaries, interpretation of requirements, code review, test selection, and releases.

The prompts below are representative versions of prompts used during the project, edited for clarity and consolidated where several short prompts addressed the same problem. I found that the important point is not just what answer the LLM produced, but how the prompt constrained the answer and how I established whether the answer was trustworthy.

## How I evaluated an LLM response

I used a source-first loop:

1. State the current product scope and the files or commands that are relevant.
2. Ask the LLM to clearly separate facts, assumptions, proposed changes, and unresolved open questions.
3. Compare the response with the implementation, tests, build configuration, and specifications.
4. Make the smallest change that satisfies the accepted requirement.
5. Run a targeted check, then the appropriate full build, test, or packaging command.
6. Inspect the diff and generated reports, and record limitations rather than converting an unverified result into a success claim.

This process mattered because an LLM can produce a plausible explanation that is inconsistent with the latest product model. A prompt was therefore a way of producing hypotheses to investigate, not a substitute for requirements analysis or testing.

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

The prompt deliberately makes the LLM perform an analysis of requirements before implementation. It explicitly names the scope and relevant constraints because simply "build a budget tracker" is too broad: it led to unverified assumptions about accounts, recurring transactions, rollover budgets, lifetime balances, or cloud synchronization, etc. I feel that, importantly, the prompt also stated negative requirements. A non-goal is valuable because it prevents scope creep and also gives testers something concrete not to expect.

The next few key concepts I included were domain definitions. For example, "Net cash flow" alone is just an ambiguous label if we do not specify that it is the invariant defined by the selected-month income minus selected-month expenses. "(Fixed) monthly category budget" means that a prior month cannot silently change the current month's available amount. Turning those decisions into scenarios made it possible to compare the UI, service, persistence layer, and tests against a single defined contract.

### Assumptions and errors made by the LLM

The LLM initially tended to interpret the product as a conventional personal-finance application. For example, it sometimes treated net cash flow as an all-time account balance, which was not intended. It also suggested useful-sounding features that were outside the intended scope. It is important to note that these were not syntax errors, but semantic errors, which I feel are more dangerous because the resulting code can look clean while implementing the wrong product.

The prompt helped expose those assumptions by asking for them explicitly. The final product decisions still requires human judgment. I had to decide on the month-scoped model, and then make sure the decision was reflected consistently in the service calculations, snapshots, views, user guide, developer guide, and acceptance scenarios.

### Verification and prompt evolution

I verified the response by tracing calculations from the transaction repository through `BudgetService` to `DashboardView`, and by comparing it with the documented product specification and tests. I also checked empty-month behaviour, income that has no category, expense category totals, and the absence of rollover and Recent activity. I then rejected the LLM response and changes whenever it described a UI label without a matching source or test invariant.

Through the whole refinement process, the prompt eventually evolved from a broad request with simply "the requirements of a budget tracker app" into a constrained request with explicit product decisions, non-goals, and explicit requests to check for ambiguity and request for clarification. Next time, I would first create the glossary of terms for the product domain along with relevant invariants before asking for design suggestions. That would reduce the amount of correction required when domain terminology drifts during implementation.

### When prompting was less effective

I found that prompting was less effective when the main task was deciding what the product should mean. The LLM could enumerate interpretations, but it could not choose the interpretation on behalf of the project. Manual documentation of the scope and project specification were faster and safer than asking for increasingly elaborate generated requirements.

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
regression. Do not introduce an ORM, a server, a network dependency, or an
abstraction whose only purpose is to make the diagram look more elaborate.
```

### Why I formulated it this way

This prompt was designed around architectural responsibilities rather than classes in isolation. A repository can satisfy individual unit tests while still reseeding data on reopen, leaking a connection, or leaving a transaction half-reassigned. Asking for failure modes and regression tests connected design advice to software engineering concepts: separation of concerns, information hiding, transaction atomicity, test isolation, and persistence invariants.

The prompt also constrained the amount of abstraction. LLMs are good at producing a familiar repository or service pattern, but a small desktop application does not automatically benefit from additional interfaces, dependency-injection frameworks, or an ORM. The relevant design question was whether each boundary made the current system easier to reason about and test.

### Assumptions and errors made by the LLM

The generic answer to an initial prompt assumed that a "clean" persistence design should introduce more layers or a general-purpose data-access framework. It also risked treating database initialization as a harmless constructor detail. In this project, initialization has observable behavior: default data should be created on first start, but custom categories, settings, and saved transactions must not be duplicated or lost after reopening.

Another easy-to-miss detail that the LLM failed to consider was category removal. Removing a category is not a simple delete when historical expenses refer to it. The operation must either reject an invalid removal or reassign the affected transactions within a safe transaction. After further prompting attempts, the LLM could describe this in prose, but I still had to confirm the actual foreign-key and repository behavior and decide which guard belonged in persistence versus the service layer.

### Verification and prompt evolution

I verified the design by reading the function call paths and then using isolated temporary SQLite databases. The important evidence included database reopen tests, category reassignment/removal tests, transaction CRUD tests, snapshot tests, and DatabaseTool reset/seed tests. The final design is documented as a BudgetDatabase facade over focused repositories, with schema setup in SchemaInitializer and JDBC error conversion in PersistenceSupport.

After multiple rounds of refinement, the prompt evolved from simply "suggest a database architecture" to more like "trace this existing call graph and preserve these invariants." That change reduced hallucinated APIs and made the LLM's output easier to challenge. A useful improvement for future work would be to ask for a failure-injection matrix as well as a happy-path design. Explicitly asking how the LLM would handle issues such as connection failure, partial reassignment, duplicate seeding, invalid monetary precision, and reopen-after-close would be more informative than getting it to produce another class diagram.

### Engineering judgment that remained necessary

The LLM could propose boundaries, but ultimately I still had to decide whether they were proportionate and appropriate to BudgetBot. I also had to decide which behavior required a transaction, where validation should occur, and which tests were useful integration evidence rather than duplicated unit tests. These are trade-offs involving risk, scope, maintainability, and the expected lifetime of the product and cannot be settled by the LLM alone.

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

This prompt deliberately included the stack trace, method name, source files, and steps to reproduce. Without those details, an LLM can produce a long list of generic JavaFX suggestions. The prompt also separated five hypotheses that are often conflated: product behavior, UI synchronization, shared state, runtime warnings, and build caching.

### What the LLM got wrong or could easily get wrong

The JavaFX unnamed-module warning looked suspicious, and the LLM's initial line of reasoning focused on module-path configuration. However, that warning is not what caused TestFX to report no matching node. The actual failure occurs when the helper calls `clickOn`: the helper searches for a visible node before it waits. `BudgetsView` does create the "Set budget" button, but the button is the graphic of a lazily laid-out TableView cell. Immediately after navigation, the cell may not yet have been materialized by a JavaFX layout pulse.

There was also a second trap the LLM fell into: a normal unfiltered Gradle run could appear green without executing the tests when Gradle build caching is enabled, and the task reported FROM-CACHE. A filtered test invocation has different task inputs, so it bypassed the unfiltered cached result and exposed the failure. The fact that a previous test report listed all UI tests as passing was not proof that the current source was reliable.

### Verification

The targeted method reproduced the FxRobotException. The normal unfiltered test task reported the test task FROM-CACHE. A forced run with the cache and up-to-date shortcuts disabled then reported the failure. Inspecting the source showed that the navigation test already waits for a budget-view control, while the failing test goes straight from clicking "Budgets" to clicking "Set budget".

The most direct test improvement is to wait for the actual control before clicking it:

```java
click(robot, "Budgets");
WaitForAsyncUtils.waitFor(
    5, TimeUnit.SECONDS,
    () -> robot.lookup("Set budget").tryQuery().isPresent());
click(robot, "Set budget");
```

A stronger long-term design would give important controls stable IDs and use a readiness condition rather than relying on visible text.

### Prompt evolution and lessons

The prompt evolved from an initial "why does this error occur?" to a source-backed experiment that compared targeted execution, cached execution, and forced execution. It was important as it changed the task from explanation generation to evidence collection.

This was also a case where prompting was less effective than manual work. The model could identify the likely race, but only an actual run showed that the unfiltered green build was cached. UI timing, JavaFX pulses, screen dimensions, and platform windowing are difficult to infer from source alone. The engineering judgment was to avoid claiming that the quality gate passed merely because Gradle printed BUILD SUCCESSFUL, and to distinguish a flaky test-harness issue from a missing application feature.

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

This prompt distinguished documentation content from documentation tooling. Mermaid rendering requires Docusaurus configuration and the Mermaid theme, while Gherkin highlighting is a Prism language configuration concern. It also stated the current repository layout because the docs plugin reads the root README and `docs` directory through a path outside website. That avoids a common LLM response that silently moves or duplicates the canonical Markdown files.

The explicit request for running a production build is an example of prompting the LLM to perform verification. I believe that when working with LLMs, a documentation change should also be treated like a software change: dependencies must be locked, configuration must compile, links must resolve as far as configured, and generated output should be inspected. The explicit visual caveat acknowledges the difference between build correctness and presentation correctness.

### Assumptions and errors

The first assumption the LLM made was that enabling a Mermaid flag alone would solve every rendering problem. The working configuration also needs the Docusaurus Mermaid theme, and the package lock must agree with `package.json`. Gherkin is different: it can remain a fenced code block while Prism provides keyword highlighting.

The Mermaid parser also rejected a decision-node label containing problematic quoting and punctuation. After that syntax issue was corrected, a separate UI-state diagram still rendered with overlapping text. A parser cannot detect that a human cannot comfortably read a diagram. The correction required shorter labels, a more suitable left-to-right direction, and simplified edge labels. This demonstrated that syntactic validity and visual usability are separate quality attributes.

### Verification and prompt evolution

I verified the configuration by inspecting `website/docusaurus.config.js`, `website/package.json`, and the lockfile, then running the Docusaurus production build. The build proved that Mermaid and Gherkin configuration was accepted and that the documentation compiled. It did not prove that a diagram was readable at every viewport, so a visual browser review remained necessary for visual claims.

The prompt evolved through separate iterations: enable Mermaid, enable Gherkin, repair a Mermaid parse error, repair text overlap, standardize headings and user-story tables, and investigate frontmatter. Explicitly splitting those concerns made each change easier to review and reduced the temptation to rewrite documentation wholesale.

### When prompting was less effective

LLMs were effective at drafting Mermaid syntax and documentation structure, but less effective at judging layout without seeing the rendered page. They also tended to optimize for a generic Docusaurus site rather than the repository's deliberate root-README layout. Manual inspection of the generated site and direct comparison with the current links were more reliable than trusting a plausible configuration snippet.

Next time I would also add a small documentation acceptance checklist: every diagram must build, every diagram must be visually inspected, every public guide URL must remain stable, and every fenced language used by the guides must either render or highlight as intended. A CI build and a human visual review would then cover complementary risks.

## Quality engineering and CI

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

The prompt treats software engineering practice as observable evidence. A tool name is not evidence by itself. I explicitly get the LLM to find the configuration, the command, the report, and the decision that explains the result. This mapping requirement for the LLM encouraged traceability from requirements to implementation and tests. The platform constraint prevented a convenient but weak solution such as dropping a failing operating-system job.

It also explicitly prohibited paid or unnecessary infrastructure. I found that  a generic LLM prompt often results in the LLM recommending adding a familiar hosted quality service, but this project has specific constraints, in particular having to be a Java desktop app. Local Gradle tasks and inspectable reports were a better fit for this repository.

### Assumptions and errors

The LLM initially treated a missing or hard-to-find document as if the engineering practice itself did not exist. The better conclusion was that the repository already had substantial infrastructure, including Gradle Wrapper, formatting, Checkstyle, PMD, JaCoCo, GitHub Actions, packaging, and tests, but that those was not sufficiently or easily discoverable without explicit documentation pointing the LLM to where it may find them. That changed the priority from adding unrelated features to improving design rationale, testing strategy, and traceability.

It was also unsafe to infer cross-platform behavior from a Windows run. Hosted macOS ARM TestFX failures involving CGLChoosePixelFormat and dialog/window interaction occur before jpackage. Treating the failure as a packaging defect, or claiming macOS UI compatibility because Windows passed, would be incorrect. The release process therefore needs a documented platform boundary: keep the full TestFX and quality gate where it is reliable, while retaining macOS static, compilation, and packaging verification.

Another assumption was regarding quality tools. Static analysis and coverage are useful gates, but they do not prove that every user journey works. Conversely, a passing test task does not prove it actually executed if Gradle restored a cached result. The prompt forced those categories of evidence to remain separate.

### Verification and prompt evolution

I verified recommendations against `build.gradle`, the configuration directories, CI workflow definitions, generated reports, and the documentation site. The resulting process included architecture tests, production SpotBugs/FindSecBugs analysis, Checkstyle, PMD, Javadoc, Spotless, JaCoCo coverage verification, repeatable database tooling, and documentation builds. The Developer Guide records commands and limitations so that another person can reproduce the claims.

The prompt evolved from a broad "improve the project for grading" request to an evidence matrix with explicit commands and platform boundaries. That prevented the LLM assessment from becoming feature implementation. It also led the LLM to perform requirements-to-evidence traceability, which are valuable because they show why practices were selected rather than merely listing tools.

### Engineering judgement that remained necessary

I had to decide which risks justified a gate, whether a static-analysis finding represented a real defect or an intentional dependency-injection pattern, how much coverage was meaningful, and which macOS limitation could be documented without hiding a failure. I also had to choose not to remove macOS release coverage just because one hosted UI interaction was unreliable. Those decisions balance defect risk, false positives, platform constraints, grading evidence, and project scope.

Prompting was less effective for deciding what counts as proportionate engineering. A grader persona can produce a long checklist, but manual inspection was necessary to remove duplicated recommendations, distinguish existing evidence from missing discoverability, and keep the project deliverable coherent. Next time I would start with a manually verified risk register and ask the LLM to challenge it, rather than asking the LLM to generate the register from nothing.

## Cross-cutting lessons

| Prompting practice                                   | What it improved                  | What could still fail                                      | Engineering control                                           |
|------------------------------------------------------|-----------------------------------|------------------------------------------------------------|---------------------------------------------------------------|
| Give the model repository paths and current scope    | Source navigation and relevance   | The model may still infer behaviour from names             | Trace the actual call path and cite the source                |
| State negative requirements and non-goals            | Scope control and test design     | The model may resurrect an older product decision          | Maintain a current glossary and acceptance specification      |
| Request assumptions and contradictions               | Early discovery of semantic drift | The model may present a guess as a fact                    | Require facts, hypotheses, and decisions in separate sections |
| Ask for a test and expected evidence for each change | Reproducibility and traceability  | A command may pass from cache or skip a path               | Inspect task outcomes and use forced execution when needed    |
| Ask for diagrams, tables, or code drafts             | Communication speed and breadth   | Generated output may be syntactically valid but unreadable | Build, render, inspect, and review the result                 |
| Preserve platform constraints in the prompt          | Less destructive CI advice        | A model may generalize from one operating system           | Validate each platform separately and document boundaries     |

Overall, I found that the most useful prompt constraints were therefore not stylistic instructions such as "be detailed." They were operational constraints: inspect these files, use these product invariants, do not invent these features, separate assumptions from facts, provide a command, and state what the command does not prove.

## What I would do differently next time

I would improve my process in five ways:

- Maintain a short decision log before implementation, including the current domain glossary, non-goals, and superseded decisions.
- Write the acceptance scenarios and evidence links before asking for implementation code. This would reduce semantic drift and make review more objective.
- Include the exact environment and cache policy in debugging prompts. For Gradle, I would distinguish normal incremental execution from forced execution at the start.
- Treat documentation and diagrams as two-stage artifacts: compile-time verification followed by a human readability review. I would also record the review status explicitly instead of treating a successful build as visual proof.

I would continue using LLMs for repository search, alternatives generation, test-case enumeration, code explanation, and first drafts. I would use them less for choosing product semantics, judging visual quality, or deciding on architecture design.

## Overall reflection

AI assistance greatly increased the project's breadth and iteration speed, particularly when the task involved connecting many files or producing a first version of a test strategy, diagram, guide, or quality checklist. It also made hidden assumptions easier to expose when I instructed the LLM to list contradictions and failure modes.

From my experience, I found that the main risk was false confidence. A coherent explanation can still be based on an old requirement; a passing Gradle command can still restore a cached result; a valid Mermaid graph can still be unreadable; and a Windows test run cannot establish macOS TestFX behavior. The developer's role was therefore not reduced to accepting generated code. It shifted toward directing the investigation, deciding what evidence is sufficient, and rejecting plausible but unsupported conclusions.

The lasting lesson is that prompting is most valuable when it is embedded in a disciplined software-engineering process. A good prompt narrows the search space and asks useful questions, but source tracing, requirements decisions, tests, reports, human review, and honest limitation reporting are what turn an LLM suggestion into reliable engineering evidence.
