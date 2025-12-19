## Tasks for `update-arrays-cli-to-parameter`

### 1) Spec delta

- [X] Create an OpenSpec delta under `changes/update-arrays-cli-to-parameter/specs/cli-configuration-flags/spec.md`
  describing the modified requirement and scenarios (this file).

### 2) Implementation (follow-up)

- [X] Update CLI parsing to expose `--arrays`/`-a` accepting `relation|inline`.
- [X] Replace `CliOptions` fields and pass the new option to `PreferencesBuilder`.
- [X] Update `PreferencesBuilder` to resolve arrays rendering from either CLI option or config `arrays` key.
- [X] Update unit and integration tests to use the new CLI option. (updated `ConfigFileCliTest`)
- [X] Remove legacy flags `--arrays-as-relation` and `--arrays-inline` (no backward compatibility required).
- [X] Add focused unit tests for `PreferencesBuilder` to validate CLI/config precedence and invalid values.
- [X] Run test suite and fix regressions. (All 46 tests passed: BUILD SUCCESSFUL)

### 3) QA and docs

- [X] Update README and any documentation examples.
- [X] Update `openspec/specs/cli-configuration-flags.md` (if desired), archive previous spec if necessary.

### 4) Archive

- [X] Once merged and released, move change folder to
  `openspec/changes/archive/YYYY-MM-DD-update-arrays-cli-to-parameter/` and update archived spec snapshot.
  **Archived on:** 2025-12-19

Notes:

- Implementation tasks have been applied to the codebase for CLI, `CliOptions`, and `PreferencesBuilder`.
- All tests pass successfully (46 tests, 0 failures). Clean build + test run completed with BUILD SUCCESSFUL.
- Test reports available at `build/reports/tests/test/index.html` and XML results in `build/test-results/test/`.
- Remaining follow-ups: update README + docs and archive the spec after merge.
