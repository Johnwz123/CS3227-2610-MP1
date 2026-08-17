---
id: developer-guide
slug: /DeveloperGuide
---

# Developer Guide

## Technology Stack

### Application Development

- **Language and API documentation**: Java 25 and Javadoc
- **Build and dependency management**: Gradle 9, using the Gradle Wrapper

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

For the documentation site, install Node.js 20 or later, run `npm install` in `website`, then run `npm start`.
