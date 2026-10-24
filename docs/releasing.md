# Release checklist

Use this checklist for each versioned GitHub Release. Releases are created by pushing a version tag such as `v0.2.0`; pushing to `main` only creates a GitHub Actions build artifact.

## Before tagging

- [ ] Confirm the changes are focused, reviewed, and documented.
- [ ] Update `CHANGELOG.md`, moving shipped items from `Unreleased` into the new version section.
- [ ] Add `docs/releases/v<version>.md` with highlights, quick start, compatibility notes, and known limitations.
- [ ] Update the README and roadmap if behavior or priorities changed.
- [ ] Build the JAR locally with the intended version: `./gradlew clean bootJar -PreleaseVersion=<version>`.
- [ ] Verify the JAR name is `build/libs/dev-data-api-<version>.jar`.
- [ ] Run the agreed verification for the release and clearly record any skipped checks.
- [ ] Confirm the Gradle wrapper files, including `gradle/wrapper/gradle-wrapper.jar`, are committed.

## Publish

Create and push a new tag (never reuse a tag for a different artifact):

```bash
git tag v<version>
git push origin v<version>
```

For example:

```bash
git tag v0.2.0
git push origin v0.2.0
```

The GitHub Actions workflow builds the tagged commit and attaches the JAR to the GitHub Release. It also uploads a workflow artifact for inspection. A `main`-branch build does not publish a GitHub Release.

## After publishing

- [ ] Confirm the tagged workflow completed successfully.
- [ ] Confirm the release exists and its JAR asset downloads successfully.
- [ ] Confirm the artifact filename and version match the tag.
- [ ] Verify the published quick-start steps and known-issues section.
- [ ] Do not replace a published asset silently; if a release is broken, document and publish a corrected version.
