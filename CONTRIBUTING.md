Contributing

Thanks for wanting to contribute to JSONSchema-to-Mermaid! These guidelines help keep the project maintainable and make
it easier for reviewers to accept your changes.

Before you start

- Search open issues and PRs to see if your idea is being discussed.
- Open an issue for large or breaking changes before investing significant work.

Development workflow

- Create a feature branch: `git checkout -b feature/<short-desc>`
- Keep commits small and focused. Use conventional commit messages where practical (e.g., `feat:`, `fix:`, `chore:`).
- Run tests locally before pushing: `./gradlew test`.

Documentation & README requirements

- Any change that introduces new features, CLI flags, configuration options, behaviour changes, or output format changes
  must update `README.md` with a concise description and usage examples.
- If your PR changes generated output (diagrams, formatting, examples), update the golden files under
  `src/test/resources/golden` and any README examples to match.
- Update `CHANGELOG.md` with a short user-facing note for the change.

Pull Request checklist

- [ ] Branch is up to date with `main`.
- [ ] Include tests for new behavior and update golden files when output changes.
- [ ] Tests pass locally and on CI.
- [ ] `README.md` updated for user-facing changes (or PR contains a short explanation why not needed).
- [ ] `CHANGELOG.md` updated for user-facing changes.

PR Process

- Open a PR against `main` with a descriptive title and link to any related issue.
- Reference the relevant README sections or examples in the release notes.
- At least one approving review and passing CI are required before merging.

If you are unsure whether a change requires README or changelog updates, add a short note in the PR description and ask
reviewers during review.
