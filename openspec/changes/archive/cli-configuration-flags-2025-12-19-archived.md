# ARCHIVED: Open Spec: CLI Configuration Flags

Original path: `openspec/specs/cli-configuration-flags.md`
Archived on: 2025-12-19
Archived by: automated archive step

Note: This spec has been implemented and archived. Keep the archived copy for historical reference.

---

<!-- Original spec content follows -->

# Open Spec: CLI Configuration Flags

Status: Draft

Authors: (add owners)

Created: 2025-12-19

Summary
-------
This document specifies a small, backwards-compatible set of CLI flags (and equivalent config-file entries) to control output rendering preferences for JSONSchema-to-Mermaid. The goal is to make previously hardcoded presentation choices configurable while keeping the CLI ergonomics simple.

Motivation
----------
Several rendering preferences are currently hardcoded (how arrays are shown, how required/optional fields are marked, how enums are rendered). Tests, README examples, and automated pipelines need a stable, documented way to opt in to alternative visual styles.

Scope
-----
This spec covers:
- Flag names and short/long forms
- Allowed values and defaults
- Interaction rules (mutually exclusive options, precedence)
- Help text, examples, and suggested test cases
- Acceptance criteria for merging the implementation

Out of scope:
- Styling hooks for Mermaid themes
- Large refactoring of internal model representation (this spec only defines configuration surface)

Design goals
------------
- Explicit, discoverable flags with good help strings
- Stable defaults to preserve existing snapshots
- Short and long flag forms for ergonomics
- Config-file parity (JSON/YAML or properties) so CI and scripts can set defaults
- Minimal surface area: implement only flags that address the highest-priority hardcoded choices

Proposed flags
--------------
We propose three top-level flag groups, each with a short and a long form and a small, fixed set of allowed values.

1) Arrays presentation

- Flags:
  - `--arrays-as-relation` (long) / `--arrays-relation` (short)
  - `--arrays-inline` (long) / `--arrays-inline` (short)
  - Alternatively: one flag with explicit values `--arrays=relation|inline`

- Recommended canonical form (single flag with values):
  - `--arrays=relation|inline`
  - Short form alias: `-a relation|inline` (optional)

- Semantics:
  - `relation` (default for backwards compatibility): render arrays as a relation edge from the owner class to the element type (arrow or association). Array field may be omitted from the owner class body or rendered as a succinct label.
  - `inline`: render array fields inline inside the owning class with square brackets (e.g. `items: Type[]`) and no separate relation edge.

- Default: `relation` (preserves current behavior and existing golden snapshots)

- Example: `--arrays=inline`

2) Required/Optional field marker style

- Flag: `--required-style=plus|none|suffix-q`

- Allowed values and semantics:
  - `plus` (default): mark required fields with a leading `+` (or whatever currently used style). This preserves existing snapshots.
  - `none`: do not visually mark required/optional; leave fields unadorned.
  - `suffix-q`: append `?` to optional fields (e.g. `name?: string`) — a common familiar notation. Equivalent to marking optional fields with `?` suffix; required fields remain plain.

- Default: `plus`

- Examples:
  - `--required-style=suffix-q`
  - `--required-style=none`

3) Enum rendering mode

- Flag: `--enum-style=inline|note|class`

- Allowed values and semantics:
  - `inline` (default): enumerate enum values inline in the field type (e.g. `status: "new" | "open" | "closed"`) or as a comma-separated list in the field's type. Keeps diagrams compact.
  - `note`: render enum values in a field-attached note (Mermaid note) or a multiline text attached to the owning class field.
  - `class`: render enums as a separate class (stereotyped `<<enumeration>>`) and draw relations from referencing classes to the enum class. Useful when enums are shared and should be visible as first-class types.

- Default: `inline`

- Examples:
  - `--enum-style=class`

Common interactions and precedence
---------------------------------
- Flags are independent unless otherwise noted.
- If multiple flags that set the same semantic option are supplied (e.g. `--arrays=inline` and `--arrays-as-relation`), the last flag wins (command-line parsing order) but the recommended parser should treat this as an error and fail fast with a helpful message.
- Config-file values are applied before command-line flags; command-line flags override config-file.
- Environment variables are not introduced in this first iteration (can be added later if needed).

CLI grammar and help text
-------------------------
Suggested additions to `--help` output with short descriptions:

--arrays=relation|inline
    How to render JSON Schema arrays: as a relation (edge) to the element type (relation), or inline as `Type[]` (inline). Default: relation.

--required-style=plus|none|suffix-q
    How to mark required vs optional fields. `plus` preserves current +/marker style. `suffix-q` appends `?` to optional fields. `none` shows no marker. Default: plus.

--enum-style=inline|note|class
    How to render enumerated values. `inline` keeps values inside the field type. `note` shows a note attached to the field/class. `class` makes enums first-class classes. Default: inline.

Suggested short aliases (optional):
- `-A` for arrays (takes `relation` or `inline`)
- `-R` for required style
- `-E` for enum style

Config file
-----------
Support for an optional config file allows users and CI to pin rendering preferences without changing CLI flags.

- Location and precedence (from lowest to highest):
  1. System-level config (not implemented in v1)
  2. Project-level config (file named `.js2mrc` or `js2m.json` in project root) — recommended
  3. User-level config (`~/.js2mrc`) — optional
  4. Command-line flags (highest precedence)

- File format: JSON or simple properties. Example JSON:

{
  "arrays": "inline",
  "requiredStyle": "suffix-q",
  "enumStyle": "class"
}

- The parser should be forgiving: ignore unknown keys with a warning, validate values and return a helpful error listing allowed values.

Acceptance criteria
-------------------
Each implemented flag must meet these criteria:
- Feature guarded behind the flags with sensible defaults that preserve current output.
- `--help` includes short descriptions and default values for each flag.
- Unit tests:
  - At least one positive unit/integration test per flag value demonstrating the rendering effect (golden snapshot or string match).
  - At least one edge case per flag: invalid value returns non-zero exit and helpful message.
- README updated with a short usage paragraph and examples.
- No regression of existing tests or golden snapshots when flags are not set (defaults preserved).
- CLI parsing behavior documented: precedence of config file vs flags, error behavior on invalid values, examples.

Suggested tests
---------------
1) Arrays
  - Input: schema with a property `items: { type: "array", items: { $ref: "#/definitions/Thing" } }`
  - Assert: default output contains a relation edge from the owner to `Thing`.
  - Assert: `--arrays=inline` results in an inline `Thing[]` field and no relation edge.

2) Required style
  - Input: schema with required `id` and optional `name`.
  - Assert: default output uses the existing `plus`/marker behavior.
  - Assert: `--required-style=suffix-q` shows `name?` (optional) and `id` plain.
  - Assert: `--required-style=none` shows neither marker.

3) Enum style
  - Input: schema with an enum property `status: { type: "string", enum: ["new","open"] }`.
  - Assert: `--enum-style=inline` shows values in the type.
  - Assert: `--enum-style=note` shows a note attached to the class/field with enum values.
  - Assert: `--enum-style=class` produces a separate `<<enumeration>>` class and relation.

4) Invalid values
  - Passing `--arrays=borked` should exit non-zero with an error listing allowed values: relation, inline.

Backward compatibility and migration
-----------------------------------
- Defaults preserve existing behavior (arrays=relation, required-style=plus, enum-style=inline).
- Config file is optional; missing file keeps defaults.
- Snapshots in CI should not change unless a flag is intentionally added in test invocation.

Implementation notes
--------------------
- Parser: extend the existing CLI argument parser to add three new options. Use a small enum to represent each option internally.
- Validation: centralize allowed values in enums so tests and help output can be driven from the same source of truth.
- Rendering: renderer should consult configuration via an injected config object; avoid scattering env/flag checks across code.
- Tests: prefer small unit tests that set config values and call rendering methods rather than full CLI integration for every case; keep a couple of golden integration tests.

Open questions
--------------
- Short flag letters: do we want `-A`, `-R`, `-E`? They help typing but potentially conflict with existing short flags. If existing short-letter scope is tight, omit short aliases and keep long forms only.
- Config file format and name: `.js2mrc` vs `js2m.json`. Recommendation: `js2m.json` for clarity and ease of editing, with support for `.js2mrc` as alternative.

Next steps
----------
1. Review and agree on the canonical flag names and defaults.
2. Implement CLI parsing changes and an injected `RenderConfig` data class.
3. Wire renderer to consult `RenderConfig` instead of hardcoded behavior.
4. Add unit and a couple of integration/golden tests covering the three flag groups.
5. Update README and release notes.

Appendix A: Example help output snippet
--------------------------------------
Usage: jsonschema-to-mermaid [options] <schema-files>
Options:
  --arrays=relation|inline       How to render JSON Schema arrays. Default: relation.
  --required-style=plus|none|suffix-q  How to mark required fields. Default: plus.
  --enum-style=inline|note|class       How to render enums. Default: inline.

Appendix B: Example config file (project-level `js2m.json`)
----------------------------------------------------------
{
  "arrays": "inline",
  "requiredStyle": "suffix-q",
  "enumStyle": "class"
}





