# Change: Replace separate array CLI flags with single `--arrays`/`-a` parameter

This change is scoped to CLI and configuration semantics. Rendering logic remains unchanged; only how the option is
supplied and represented is affected.

## Notes

- Consumers must update invocations of the CLI:
    - replace `--arrays-inline` with `--arrays=inline` (or `-a=inline`) and
    - replace `--arrays-as-relation` with `--arrays=relation` (or `-a=relation`).

## Migration

- Breaking change: old CLI flags are removed. There is no backwards compatibility guarantee for pre-release users.
- Affected code (implementation tasks): `src/main/kotlin/jsonschema_to_mermaid/cli/App.kt`, `CliOptions.kt`,
  `PreferencesBuilder.kt`, and CLI-related tests.
- Affected spec: `cli-configuration-flags` (modified).

## Impact

- Update tests, README, and any documentation/examples referring to the old flags.
- Update config documentation and behavior to emphasize that the config key `arrays` accepts the same values (
  `relation`/`inline`). The config key name remains `arrays`.
- Add new CLI option: `--arrays` (long) and `-a` (short) which takes a value: `relation` or `inline`. Example:
  `--arrays=inline` or `-a=relation`.
- Remove CLI flags: `--arrays-as-relation` and `--arrays-inline`.

## What changes

This change is breaking: the two old flags will be removed and replaced with a single option.

JSON Schema array rendering was historically toggled with two separate boolean flags (`--arrays-as-relation` and
`--arrays-inline`). This is redundant and error-prone. A single parameter that accepts explicit values (`relation` or
`inline`) is clearer for users, aligns directly with the configuration file shape, and reduces option combinatorics.

## Why

- Clarity: A single parameter with explicit values reduces ambiguity about the intended rendering style, and make it
  possible to extend with additional styles in the future without adding more flags.
