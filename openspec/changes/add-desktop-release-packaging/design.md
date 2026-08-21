## Context

BudgetBot is currently a Java 25 JavaFX application launched through the Gradle
Application plugin. That workflow is appropriate for developers but requires end
users to install a compatible JDK and JavaFX runtime. The existing CI workflow
verifies source code on Ubuntu and has read-only repository contents permission;
it does not create a distributable application or GitHub Release.

The application deliberately stores user data outside its installation directory
at `~/.budgetbot/budgetbot.db`. Native package removal must therefore remove the
application files without deleting this independent data location.

## Goals / Non-Goals

**Goals:**

- Produce self-contained native installers that bundle the Java runtime and
  JavaFX dependencies for Windows, macOS, and Linux.
- Give end users a clear GitHub Releases download, install, launch, update, and
  uninstall path without requiring Gradle, Java, or a terminal.
- Publish installers only from an explicitly versioned Git tag after the project
  quality checks succeed.
- Retain local budget data by default during upgrades and uninstalls, while
  documenting how the user can explicitly remove all data.

**Non-Goals:**

- Code signing, Apple notarization, or managing signing certificates in this
  change. Unsigned packages may display platform trust warnings.
- Automatic in-app update checks or silent updater services.
- Supporting every Linux distribution or package format in the initial release.
- Publishing a raw JAR as the primary end-user download.

## Decisions

### Use `jlink` and `jpackage` through Gradle packaging tasks

The build will add Gradle packaging support that creates a minimized runtime image
from BudgetBot's Java, JavaFX, and SQLite dependencies, then invokes JDK
`jpackage` to create native installers. The packaging tasks will use the existing
application main class and version, and write clearly named artifacts beneath the
build directory.

This produces an application users can install and run independently of a system
JDK. A Gradle integration is preferred over ad-hoc shell commands because it
keeps package creation reproducible locally and in CI.

Alternatives considered:

- A plain or fat JAR was rejected as the primary distribution because users
  would still need a compatible Java/JavaFX runtime and platform-native
  dependencies.
- Committing prebuilt runtime images was rejected because they are large,
  platform-specific generated outputs that should be produced by the build.

### Build one native artifact per operating system

Each installer will be generated on its matching operating-system runner:

- Windows: an `.msi` installer.
- macOS: a `.dmg` containing the application bundle.
- Ubuntu Linux: a `.deb` package.

The initial Linux package targets Debian/Ubuntu-derived systems. Artifact names
will contain the normalized Git tag version and platform to make the correct
download immediately identifiable in a GitHub Release.

Native runner builds are required because `jpackage` installers are
platform-specific. The package contents must include only the application and its
runtime; the user data directory must never be treated as install content.

### Publish immutable release assets from version tags

A dedicated release workflow will trigger on tags matching the documented version
pattern (for example, `v1.2.3`). It will derive the package version from the tag,
run the Gradle quality gate, package each platform in a matrix, upload the
platform artifacts between jobs, and create or update the matching GitHub Release
with generated release notes and all three assets.

Only the release workflow will receive `contents: write`; the existing pull
request and branch CI remains read-only. A publish job will run only after all
matrix jobs succeed, so a partial set of installers is not released.

Alternatives considered:

- Publishing on every `main` branch push was rejected because unversioned or
  unreviewed commits are not stable user releases.
- Giving the general CI workflow release-write permission was rejected under the
  principle of least privilege.

### Make uninstallation remove the application but retain budget data

The installers will use normal platform installation locations and standard
uninstall mechanisms: Windows Installed Apps, macOS deletion of the app bundle,
and Linux package-manager removal. Documentation will state that these operations
leave `~/.budgetbot/budgetbot.db` untouched so upgrades and reinstallations retain
the user's history. It will also give an explicit, separately labelled command or
file-manager path for users who intentionally want to remove all BudgetBot data.

This separation protects data from accidental loss and matches the existing
application storage design.

## Risks / Trade-offs

- Unsigned Windows and macOS packages can show security warnings -> Document
  the limitation clearly and keep signing/notarization as a later hardening
  change.
- Native installers cannot be built on a single runner -> Use an OS matrix and
  publish only after every required package succeeds.
- `jpackage` inputs and package formats can vary with the installed JDK and
  runner image -> Pin Java 25, verify generated artifact names, and keep
  packaging configuration in version control.
- The `.deb` package is not directly installable on every Linux distribution ->
  Label its Debian/Ubuntu scope and defer other formats until they are needed.
- Users may expect uninstall to delete data -> State the retained-data behavior
  in the release notes and user guide, and make full data deletion an explicit
  separate action.

## Migration Plan

1. Add and test the Gradle packaging tasks locally for the host platform.
2. Add the tag-triggered release workflow and verify that it uploads package
   artifacts without publishing from ordinary CI events.
3. Publish a test/pre-release tag, verify installation and uninstallation on
   each platform, then publish the first stable tag.
4. Roll back a bad release by marking the GitHub Release as a draft or deleting
   its assets; existing installed copies and their local data remain unaffected.

## Open Questions

- None for the initial unsigned release. Code signing and notarization are
  explicitly deferred to a follow-up change.
