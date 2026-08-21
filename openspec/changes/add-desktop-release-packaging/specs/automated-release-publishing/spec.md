## ADDED Requirements

### Requirement: Version-tag-triggered release workflow
The repository SHALL provide a dedicated GitHub Actions workflow that triggers only for documented version tags in the `v<major>.<minor>.<patch>` format. The workflow SHALL derive a normalized package version from the tag and SHALL not publish end-user releases for pull requests or ordinary branch pushes.

#### Scenario: Publish from a version tag
- **WHEN** a maintainer pushes a tag such as `v1.2.3`
- **THEN** the release workflow starts and uses `1.2.3` as the package version

#### Scenario: Push a normal branch commit
- **WHEN** a maintainer pushes a non-tag commit to a branch
- **THEN** the release workflow does not create a GitHub Release

### Requirement: Native package build matrix
The release workflow SHALL build the Windows installer on a Windows runner, the macOS image on a macOS runner, and the Debian/Ubuntu package on an Ubuntu runner. Every package build SHALL use Java 25 and the repository Gradle Wrapper.

#### Scenario: Build all release packages
- **WHEN** the release workflow runs for a valid version tag
- **THEN** it produces one versioned Windows `.msi`, macOS `.dmg`, and Linux `.deb` package

### Requirement: Verified release publication
The release workflow SHALL complete the Gradle quality gate before publishing a GitHub Release. It SHALL publish a release with all required platform assets only after every platform package job succeeds.

#### Scenario: Publish a complete verified release
- **WHEN** the quality gate and every native package build succeed
- **THEN** the workflow creates or updates the matching GitHub Release with generated release notes and all required installer assets

#### Scenario: Stop a failed release
- **WHEN** the quality gate or any required platform package job fails
- **THEN** the workflow does not publish a GitHub Release or partial set of release assets

### Requirement: Least-privilege release permissions
The dedicated release workflow SHALL use `contents: write` only for the jobs that create the GitHub Release and upload its assets. Existing pull-request and branch CI workflows SHALL retain their current non-release permissions.

#### Scenario: Review ordinary CI permissions
- **WHEN** the normal CI workflow runs for a pull request or branch push
- **THEN** it cannot create or modify GitHub Releases
