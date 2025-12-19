# Project Context

## Purpose
JSONSchema-to-Mermaid is a command-line tool that converts JSON Schema (JSON or YAML) files into Mermaid class diagrams. Its goals are:

- Help developers and architects visualize data models and API schemas quickly.
- Support multiple JSON Schema features (objects, arrays, $ref, allOf/oneOf/anyOf, patternProperties, additionalProperties, inheritance via `extends`).
- Produce readable Mermaid classDiagram output that can be embedded in docs or rendered by Mermaid toolchains.

## Tech Stack
- Language: Kotlin (JVM)
  - Kotlin plugin version: 2.2.20 (see build.gradle.kts)
  - JVM toolchain: Java 11 (configured in build.gradle.kts)
- Build: Gradle Kotlin DSL (build.gradle.kts)
  - Shadow (fat JAR) plugin used to produce a single executable JAR
  - Ben Manes versions plugin available for dependency updates
- CLI parsing: Clikt
- JSON parsing: Gson
- YAML parsing: SnakeYAML
- Testing: Kotest with JUnit Platform
- Packaging: Shadow JAR (fat JAR) and standard JAR
- License: Apache License 2.0 (see LICENSE)

## Project Conventions
This section documents the current, observed conventions and recommended best-practices used by contributors.

### Code Style
- Codebase is Kotlin-first and follows typical Kotlin idioms (packages under `jsonschema_to_mermaid`).
- **Modern Kotlin Style Required**: All Kotlin code MUST follow modern, functional, idiomatic Kotlin 2.x style as defined in `openspec/specs/kotlin-modern-style.md`. Key principles:
  - **Prefer functional transformations** over imperative loops (`filter`, `map`, `fold` instead of `for` loops)
  - **Use expression bodies** for short functions
  - **Use `when` as an expression** instead of if-else chains
  - **Use scope functions** (`let`, `run`, `apply`, `also`, `with`) for cleaner null handling and initialization
  - **Prefer immutability** (`val` over `var`, immutable collections)
  - **Use `buildList`, `buildString`, `buildMap`** for collection/string construction
  - **Use destructuring declarations** for pairs and data classes
  - **Use `takeIf`/`takeUnless`** for conditional returns
  - **Use `require`/`check`/`error`** for preconditions
  - **Avoid `!!` (non-null assertion)** — use safe calls or proper null handling
- Use expressive names, PascalCase for types, camelCase for values/functions
- Keep short functions where sensible, prefer immutability and data classes for simple DTOs
- Use meaningful package structure: `cli`, `schema`, `diagram`, `relationship`, `schema_files`, etc.
- Recommendation: Add a formatter (ktlint or spotless) to enforce consistent style in CI if desired.

### Architecture Patterns
- CLI logic is thin and delegates to a service layer. For example:
  - `jsonschema_to_mermaid.cli.App` handles parsing CLI args (Clikt) and builds a `CliOptions` object.
  - `CliService` orchestrates the application workflow by coordinating specialized components.
- The CLI package follows Single Responsibility Principle with focused classes:
  - `CliOptions` — Immutable data class encapsulating all CLI options.
  - `SourceResolver` — Resolves source files and directories from CLI options.
  - `ConfigFileResolver` — Discovers and parses configuration files (js2m.json, .js2mrc).
  - `PreferencesBuilder` — Builds `Preferences` from CLI options and config files.
  - `OutputWriter` — Handles writing output to files or stdout.
  - `DiagnosticLogger` — Handles diagnostic logging for CLI operations.
- Dependency Injection is used in `CliService` to allow testing with mock collaborators.
- Modular package layout by responsibility (diagram generation, schema parsing/resolution, relationship handling).
- Single-responsibility classes and small functions are used throughout (see `MermaidGenerator`, `SchemaFilesReader`, `RelationshipBuilder`, etc.).
- Immutable data structures for inputs/outputs where applicable (data classes like `CliOptions`).

### Clean Code Principles
Clean Code principles MUST be applied to all code in this project. The goal is readable, maintainable, and well-tested code. Apply the following rules on every change:

- Meaningful names: Choose descriptive names for packages, classes, functions, variables, and tests. Avoid abbreviations and single-letter names except for well-known idioms (e.g., i for loop index).
- Small functions: Functions should do one thing and be short (prefer < 20–25 lines where practical). If a function grows, extract helpers with clear names.
- Single Responsibility & SOLID: Each class/module should have a single responsibility. Favor composition and small interfaces. Use dependency inversion to make code testable.
- DRY (Don't Repeat Yourself): Avoid duplication. Extract shared logic into functions/util classes or test helpers.
- Readability over cleverness: Prefer explicit, straightforward code rather than clever or terse constructions.
- Test-first thinking: Add unit tests for new behaviors and regression tests for bugs. Use golden tests for output formatting where appropriate.
- Prefer immutability: Favor val over var, and immutable data structures when possible.
- Fail fast and handle errors explicitly: Validate inputs and throw meaningful exceptions; provide useful user-facing error messages in CLI flows.
- Minimize side-effects: Keep functions pure where possible to make reasoning and testing easier.
- Keep comments useful and up to date: Comments should explain intent or rationale, not restate what the code does. Remove or update comments that become stale.
- Continuous refactoring: When adding features, refactor legacy code rather than piling on technical debt.

Enforcement & Automation
- Formatting and linting: Add a formatter/linter (recommended: ktlint or spotless plus detekt) and enable it locally and in CI. The formatter should be applied automatically (pre-commit hook or CI) and CI should fail on violations.
- Static analysis: Use detekt (or similar) to catch common issues and code smells.
- Pre-commit checks: Optionally use a pre-commit hook (e.g., via `pre-commit` or a simple git hook) to run formatter and fast tests locally before committing.
- CI gates: CI should run `./gradlew build`, tests, and static analysis. Merges to `main` should require passing CI and at least one approving review.

Code Review Checklist (minimum)
- Scope: Is the PR small and focused? If not, can it be split?
- Naming: Are names clear for intent and usage?
- Tests: Are there tests for happy paths, edge cases, and regression scenarios? Do golden tests cover formatting changes where relevant? Also confirm that all tests pass locally using `./gradlew test`.
- Readability: Can a reviewer understand the change quickly? Are helper functions introduced when needed?
- Error handling: Are errors surfaced with clear messages and proper exception types?
- Duplication: Is functionality duplicated elsewhere? Can it be extracted?
- Performance: Any obvious performance regression or unnecessary allocations?
- Security: Any user input or file handling concerns accounted for?
- Formatting/linting: Is the code formatted and passing static analysis checks?

### Documentation & README
- Keep `README.md` up to date: For every change that introduces new features, CLI flags, configuration options, behaviour changes, or example/output format changes, update `README.md` with a concise description and usage examples showing the new/changed behavior.
- PR requirement: Any pull request that changes user-facing behaviour must include README updates (or a short explanation in the PR why documentation changes are not required). Reviewers should verify README changes as part of the Code Review Checklist.
- Release notes & changelog: Update release notes and the changelog for user-facing changes and reference the relevant README sections or examples.
- Golden examples and tests: If a change affects generated output (diagrams, examples, or formatting), update the golden files and README examples to match; add or update tests where appropriate.

### Examples & Conventions
- Use `data class` for DTOs and immutable value containers.
- Use `sealed` classes for closed hierarchies and exhaustive when expressions.
- Mark module-internal types/functions `internal` when they shouldn't be part of the public API.
- Prefer small expression bodies for short functions; prefer block bodies for more complex logic.
- Use Kotlin standard library helpers thoughtfully; avoid chaining many scoping functions that reduce clarity.

### Testing Strategy
- Unit & integration tests run with Gradle using Kotest and JUnit Platform.
- Golden-file tests exist to compare generated Mermaid output against expected output in `src/test/resources/golden` (see tests under `src/test/kotlin` and `test_util` helpers).
- Test command: `./gradlew test` (or `./gradlew build` to run tests as part of the build).
- Tests are expected to be deterministic; golden files act as regression checks for output format.

### Git Workflow
- CI: GitHub Actions (badge in README indicates a workflow at `.github/workflows/ci.yml`).
- Recommended branching model:
  - `main` is the stable branch (use whichever the repository uses — PRs will land there).
  - Work on feature branches named `feature/<short-desc>` or `fix/<ticket>`.
  - Open PRs against `main` with a descriptive title and link to an issue if applicable.
- Commit messages: use a concise style (e.g. Conventional Commits or short imperative messages). Example: `feat: add arrays-inline option` or `fix(cli): handle missing files gracefully`.
- Releases: use semantic versioning. The project currently stores the version in `gradle.properties` (example: `version=0.1.0`).
  - Release flow suggestion: update `gradle.properties` → run `./gradlew build` → tag `vX.Y.Z` → create release artifacts (shadow JAR) → push tag.

## Build / Run / Test
- When implementing code or bugfixing, ensure all tests pass locally with `./gradlew test` before opening a PR.

- Build (compile, tests, assemble jars):

```bash
./gradlew build
```

- Produce an executable fat JAR (shadow plugin):

```bash
./gradlew shadowJar
# or the shadow jar created as part of build if configured
```

- Run tests only:

```bash
./gradlew test
```

- Run the CLI from Gradle (application plugin):

```bash
# Run with arguments (example):
./gradlew run --args='-d examples -o out.mmd'
```

- Run the packaged jar (example; adjust version if changed):

```bash
java -jar build/libs/jsonschema-to-mermaid-0.1.0.jar
```

Notes:
- The build config generates `src/main/resources/version.properties` and a generated `app.properties` from `gradle.properties` and `gradle.properties`'s `appName` value.
- The code defines a main entry in `src/main/kotlin/jsonschema_to_mermaid/cli/App.kt` (top-level `main(args)` calls `App().main(args)`). The observed main class is `jsonschema_to_mermaid.cli.AppKt`.
  - The `build.gradle.kts` file currently sets `application.mainClass` to `jsonschema_to_mermaid.AppKt`; consider aligning it with `jsonschema_to_mermaid.cli.AppKt` (if needed) to ensure the produced jar's manifest references the correct main class.

<!-- End of generated project specification -->

