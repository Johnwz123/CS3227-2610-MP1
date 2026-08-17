---
id: overview
slug: /
---

# CS3227-2610-MP1

Coursework project for CS3227.

## Prerequisites

- JDK 25
- Node.js 24 or later (only for the documentation site)

## Common commands

On Windows, use `gradlew.bat`; on macOS/Linux, use `./gradlew`.

```text
gradlew.bat run                 # Run the application
gradlew.bat check               # Test and run all Java quality checks
gradlew.bat spotlessApply       # Format Java sources
gradlew.bat jacocoTestReport    # Generate the coverage report
gradlew.bat javadoc             # Generate API documentation
```

To run the documentation site locally:

```text
cd website
npm install
npm start
```

The published guides are built from [DeveloperGuide.md](docs/DeveloperGuide.md) and
[UserGuide.md](docs/UserGuide.md).
