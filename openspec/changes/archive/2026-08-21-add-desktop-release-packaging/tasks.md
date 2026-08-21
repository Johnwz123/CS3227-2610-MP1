## 1. Native packaging foundation

- [x] 1.1 Add Gradle `jlink`/`jpackage` integration that produces a self-contained BudgetBot runtime from the existing JavaFX application, including stable application metadata and an externally supplied release version.
- [x] 1.2 Configure host-native packaging tasks and artifact names for Windows `.msi`, macOS `.dmg`, and Debian/Ubuntu `.deb` releases under the build directory.
- [x] 1.3 Add local packaging documentation and verify that the host platform package can be generated, contains the application/runtime, and does not include the user data directory.

## 2. Automated GitHub Release publishing

- [x] 2.1 Add a dedicated release workflow triggered only by `v<major>.<minor>.<patch>` tags, with normalized version extraction and release-specific `contents: write` permission.
- [x] 2.2 Implement the Windows, macOS, and Ubuntu packaging matrix using Java 25, the Gradle Wrapper, platform-native package tasks, and the complete quality gate before each artifact is uploaded.
- [x] 2.3 Add a dependent publish job that downloads every platform artifact and creates or updates one GitHub Release with generated notes only when the full matrix succeeds.
- [x] 2.4 Preserve the existing CI workflow's read-only release permission boundary and Linux Gradle-wrapper executable handling.

## 3. User and maintainer guidance

- [x] 3.1 Document GitHub Release downloads, install, launch, and manual upgrade steps for Windows, macOS, and Debian/Ubuntu Linux users.
- [x] 3.2 Document standard uninstall steps that preserve `~/.budgetbot/budgetbot.db`, plus separately labelled, explicit instructions for removing all local BudgetBot data.
- [x] 3.3 Document the maintainer tag-release process, expected release assets, and unsigned-package trust-warning limitation.

## 4. Verification

- [x] 4.1 Run Gradle formatting and the complete quality gate, then generate and inspect the host-native installer artifact.
- [x] 4.2 Review the release workflow for tag-only triggering, normalized version propagation, full-matrix dependency, required assets, and least-privilege permissions.
- [x] 4.3 Validate the OpenSpec change strictly and review the final diff for whitespace and unintended files.
