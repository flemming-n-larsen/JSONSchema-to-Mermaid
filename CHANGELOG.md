# Changelog

All notable changes to this project will be documented in this file.

Format

- Use "Unreleased" heading for the current development cycle.
- Follow common Keep a Changelog style: group entries by `Added`, `Changed`, `Fixed`, `Removed`.

Example

## Unreleased

### Added

- Short user-facing note describing the feature.

### Changed

- Notes about behavior or formatting changes users should know about.

### Fixed

- Bug fixes with brief descriptions and references if relevant.

When to update

- Update `CHANGELOG.md` for any user-visible changes (features, CLI flags, behavior changes, output changes). Prefer a
  one-line entry describing the change and why it matters to users.

Release process

- Before creating a release, move `Unreleased` entries into a versioned section (e.g., `## 0.2.0 - YYYY-MM-DD`) and
  update `gradle.properties` version accordingly.
