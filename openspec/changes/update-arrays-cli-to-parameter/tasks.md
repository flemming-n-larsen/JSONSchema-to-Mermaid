## Tasks for `update-arrays-cli-to-parameter`

### 1) Spec delta

- [ ] Create an OpenSpec delta under `changes/update-arrays-cli-to-parameter/specs/cli-configuration-flags/spec.md`
  describing the modified requirement and scenarios (this file).

### 2) Implementation (follow-up)

- [ ] Update CLI parsing to expose `--arrays`/`-a` accepting `relation|inline`.
- [ ] Replace `CliOptions` fields and pass the new option to `PreferencesBuilder`.
- [ ] Update `PreferencesBuilder` to resolve arrays rendering from either CLI option or config `arrays` key.
- [ ] Update unit and integration tests to use the new CLI option.
- [ ] Update README and any documentation examples.
- [ ] Run test suite and fix regressions.

### 3) QA and docs

- [ ] Update CHANGELOG and mention breaking change.
- [ ] Update `openspec/specs/cli-configuration-flags.md` (if desired), archive previous spec if necessary.

### 4) Archive

- [ ] Once merged and released, move change folder to
  `openspec/changes/archive/YYYY-MM-DD-update-arrays-cli-to-parameter/` and update archived spec snapshot.

Notes:

- Implementation tasks are intentionally listed for completeness but are not part of this spec-only request.

