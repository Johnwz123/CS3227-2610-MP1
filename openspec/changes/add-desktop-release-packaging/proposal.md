## Why

BudgetBot currently runs from source through Gradle, which requires users to install a compatible JDK and understand development commands. Native installers and tagged GitHub Releases will give users a simple, trustworthy download path while making each published version repeatable.

## What Changes

- Package BudgetBot as native, self-contained desktop installers for Windows, macOS, and Linux rather than requiring end users to run a JAR or install Java themselves.
- Define the user download, installation, launch, and uninstall experience for every supported platform.
- Preserve the user's local BudgetBot database when the application is uninstalled, and document the separate optional deletion of all app data.
- Add tag-triggered GitHub Actions automation that verifies the project, creates the platform packages, and attaches them to a GitHub Release.
- Document how maintainers publish a version and how users download, install, update, and uninstall it.

## Capabilities

### New Capabilities

- `desktop-application-distribution`: Native install packages and documented user installation, launch, update, uninstall, and local-data lifecycle across supported desktop platforms.
- `automated-release-publishing`: Tag-triggered, verified creation of platform packages and GitHub Release assets.

### Modified Capabilities

- None.

## Impact

- Gradle application packaging configuration and any packaging plugin or JDK tooling.
- New GitHub Actions release workflow with permission to create releases and upload assets.
- README and user/developer documentation.
- Platform-specific installer assets and GitHub Releases for end users.
- The existing `~/.budgetbot/budgetbot.db` data location, which must remain separate from installed application files.
