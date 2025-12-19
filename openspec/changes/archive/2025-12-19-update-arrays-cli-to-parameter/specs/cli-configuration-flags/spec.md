# Open Spec: CLI configuration flags — arrays option

This delta describes a backward-compatible change to the CLI configuration flags related to how arrays are rendered. It replaces the current pair of boolean flags (`--arrays-as-relation` and `--arrays-inline`) with a single parameterized option `--arrays` (short `-a`) that accepts one of two values: `relation` or `inline`.

Rationale

- The existing pair of boolean flags is redundant and can be confusing: `--arrays-as-relation` (default true) and `--arrays-inline` (overrides) create an awkward precedence model.
- A single explicit parameter with an enumerated set of values is clearer for users and easier to validate/parse programmatically.

Proposal

- Add a new CLI option:
  - Long form: `--arrays`
  - Short form: `-a`
  - Accepted values: `relation`, `inline`
  - Semantics: controls how array-typed properties are rendered in generated diagrams.

- Deprecate (but keep temporarily for backward compatibility) the existing boolean flags:
  - `--arrays-as-relation` (boolean)
  - `--arrays-inline` (boolean)

Behavior and precedence

- CLI explicitly provided `--arrays`/`-a` takes highest precedence over the configuration file's `arrays` key.
- If `--arrays` is not provided, the config file `arrays` key (if present) is used. Valid values in config are `relation` and `inline`.
- If neither CLI nor config provides an `arrays` value, the existing default behavior is preserved (arrays rendered as relations).
- If the legacy boolean flags are present alongside `--arrays`, the new parameter wins. If both legacy flags are provided and conflict, apply existing precedence (explicit inline flag overrides relation flag) but emit a deprecation warning in future implementation.

Input validation

- CLI parser must validate the `--arrays` value and reject unknown values with a helpful error message, e.g. "Invalid value for --arrays: must be one of 'relation' or 'inline'".
- Config file parsing must validate the `arrays` key when present and surface a descriptive error on invalid values.

Examples

- Explicit relation (default):
  - `jsonschema-to-mermaid --arrays relation`
  - `jsonschema-to-mermaid -a relation`

- Explicit inline:
  - `jsonschema-to-mermaid --arrays inline`
  - `jsonschema-to-mermaid -a inline`

Migration notes for implementers

- Replace CLI boolean flags in `CliOptions` with a single property (preferably an enum or string) representing the `arrays` choice.
- Update `App.kt` (the Clikt command) to expose `--arrays`/`-a` accepting `relation|inline` and to continue accepting legacy flags for a transitional period if desired.
- Update `PreferencesBuilder` to resolve arrays preference in this order:
  1. CLI `--arrays` parameter (if supplied)
  2. Config file `arrays` key (if present)
  3. Built-in default (`relation`)
- Ensure `PreferencesBuilder` throws a clear `InvalidOptionException` (or similar) for unknown values.

Acceptance criteria

- New spec file added to `openspec/changes/update-arrays-cli-to-parameter/specs/cli-configuration-flags/spec.md`. (this file)
- CLI accepts `--arrays`/`-a` with `relation` or `inline` values.
- `PreferencesBuilder` resolves arrays rendering using the precedence described above.
- Config file continues to support `arrays` key with the same valid values.
- Unit tests and integration tests added/updated to cover:
  - CLI `--arrays` values accepted and result in corresponding preferences
  - Config file `arrays` value used when CLI not provided
  - Invalid values in CLI and config produce descriptive errors
  - Backward compatibility with previous boolean flags (if kept temporarily) or migration notes updated if they are removed

Files that will need changes (implementation tasks, not part of this spec-only change):
- `src/main/kotlin/jsonschema_to_mermaid/cli/App.kt`
- `src/main/kotlin/jsonschema_to_mermaid/cli/CliOptions.kt`
- `src/main/kotlin/jsonschema_to_mermaid/cli/PreferencesBuilder.kt`
- Unit tests under `src/test/kotlin/jsonschema_to_mermaid/cli/` and integration tests that exercise CLI/config precedence
- README.md and any docs/examples that mention the old flags

Testing notes

- Add focused unit tests for `PreferencesBuilder` that pass a mocked config and CliOptions with all three cases (CLI present, config present, neither) and assert the resolved boolean/enum.
- Add integration CLI tests using the real `App` runner to assert exit codes and output when invalid values are provided.

Compatibility and changelog

- This change is a breaking change for users who relied on the boolean flags programmatically; document the change in CHANGELOG and the README and provide migration guidance (map `--arrays-as-relation true` -> `--arrays relation`, `--arrays-inline` -> `--arrays inline`).

Archive plan

- After implementation, move this change to `openspec/changes/archive/YYYY-MM-DD-update-arrays-cli-to-parameter/` and include an archived snapshot of this spec.

---

(End of spec delta)

