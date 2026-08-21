# Releasing

Maintainer notes for publishing `com.praxicraft:assess`.

## How publish works

[`.github/workflows/publish.yml`](.github/workflows/publish.yml) runs on pushes to **`main`** when:

- `src/**`
- `pom.xml`
- `CHANGELOG.md`
- `.github/workflows/publish.yml`

Flow:

1. Run tests on JDK 17 / 21.
2. Require `pom.xml` `<version>` to match `Version.STRING`.
3. If git tag `v{version}` already exists → skip.
4. Otherwise create + push `v{version}` and `mvn deploy` to **GitHub Packages**.

Maven Central is not wired yet (needs Central Portal / signing). Use GitHub Packages until then.

## Cut a release

1. Bump `<version>` in `pom.xml` and `Version.STRING` in `src/main/java/com/praxicraft/assess/Version.java` (keep them equal).
2. Update `CHANGELOG.md`.
3. Merge to `main`.

## One-time setup

1. Create GitHub Environment **`maven`** on `praxicraft-platform/praxicraft-java` (optional reviewers).
2. Ensure Actions has permission to write packages (workflow sets `packages: write`).
3. Consumers can depend on GitHub Packages with a `settings.xml` that authenticates to `https://maven.pkg.github.com/praxicraft-platform/praxicraft-java`.

## GitHub Release

The Publish workflow also creates a **GitHub Release** for tag `v{version}` (with generated notes and package assets where applicable).

You can run **Actions → Publish → Run workflow** manually (`workflow_dispatch`) after bumping the version on `main`.

## Auto-bump

Pushes to `main` that change package source auto-bump the patch version, update `CHANGELOG.md`, commit `chore(release): vX.Y.Z`, tag, create a **GitHub Release**, and publish to the language registry when credentials are configured.

Skip with `[skip release]` in the commit message.
