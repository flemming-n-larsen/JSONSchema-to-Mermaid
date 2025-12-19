## MODIFIED Requirements

### Requirement: Arrays CLI option and config
The system SHALL provide a single CLI option and a matching configuration setting to control how JSON Schema arrays are rendered in generated Mermaid diagrams.

- The CLI SHALL expose a long option `--arrays` and a short option `-a`.
- The option SHALL accept exactly two case-insensitive values: `relation` and `inline`.
  - `relation` means arrays are rendered as relationship edges between nodes (default).
  - `inline` means arrays are rendered as inline fields (no relationship edges for arrays).
- The configuration file SHALL accept the same key `arrays` with allowed values `relation` or `inline`.
- CLI option SHALL take precedence over configuration file values.
- The old flags `--arrays-as-relation` and `--arrays-inline` SHALL be removed.

#### Scenario: CLI sets arrays to inline using long form
- **WHEN** the user runs the CLI with `--arrays=inline`
- **THEN** arrays SHALL be rendered as inline fields and no relationship edges shall be generated for array properties
- **AND** the program SHALL exit zero on success

#### Scenario: CLI sets arrays to relation using short form
- **WHEN** the user runs the CLI with `-a=relation` (or `-a relation`)
- **THEN** arrays SHALL be rendered as relationship edges (the default behavior)
- **AND** the program SHALL exit zero on success

#### Scenario: CLI option overrides config file
- **GIVEN** a configuration file containing `"arrays": "relation"`
- **WHEN** the user runs the CLI with `--arrays=inline`
- **THEN** the effective behavior SHALL be `inline` (CLI wins)

#### Scenario: Invalid value for CLI option
- **WHEN** the user runs the CLI with `--arrays=borked`
- **THEN** the program SHALL exit non-zero
- **AND** the program SHALL print a helpful error message listing allowed values: `relation, inline`

#### Scenario: Config file accepts same values
- **GIVEN** a configuration file containing `"arrays": "inline"`
- **WHEN** the user runs the CLI with no explicit arrays option
- **THEN** the effective behavior SHALL be `inline`

#### Scenario: Migration note for removed flags
- **GIVEN** a user previously relied on `--arrays-inline` or `--arrays-as-relation`
- **THEN** they SHALL update invocations to use `--arrays=inline` or `--arrays=relation` (or `-a`) respectively


## Rationale
Using a single parameter with explicit enumerated values avoids conflicting boolean flags and matches the configuration file semantics, making both documentation and usage simpler.

