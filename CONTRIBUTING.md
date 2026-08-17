# Contributing

## Getting Started

1. Install JDK 25 and clone the repository.
2. Run `gradlew.bat check` on Windows, or `./gradlew check` on macOS/Linux.
3. For documentation changes, install Node.js 24 or later and run `npm install` in `website`.

Always use the Gradle Wrapper supplied by this repository. Do not rely on a locally installed Gradle version.

## Development Workflow

1. Create a short-lived branch from the default branch.
2. Make one focused change.
3. Add or update tests for changed behaviour.
4. Run `gradlew.bat spotlessApply` and `gradlew.bat check`.
5. Update relevant Javadoc and user or developer documentation.
6. Open a pull request describing the problem, solution, tests run, and any follow-up work.

## Commit Messages

Use Conventional Commits:

```text
<type>(optional-scope): short imperative summary
```

Allowed types are `feat`, `fix`, `docs`, `test`, `refactor`, `build`, `ci`, `chore`, and `perf`.

Examples:

```text
feat(greeting): add configurable greeting message
fix(cli): return a non-zero exit code for invalid arguments
docs: explain local documentation preview
test(greeting): cover empty recipient names
```

Keep the summary concise, use the imperative mood, and make unrelated changes in separate commits.

## Quality Expectations

Pull requests must pass the same checks as CI:

```text
gradlew.bat check
gradlew.bat jacocoTestReport
gradlew.bat javadoc
```

`check` runs tests, Spotless format validation, Checkstyle, PMD, and JaCoCo coverage verification.

## Documentation

The published site reads from `docs/DeveloperGuide.md` and `docs/UserGuide.md`. Preview it locally with `npm start` from `website`. Keep documentation changes focused, accurate, and understandable to a new project member.
