# Open Spec: Additional Optional Followups (Implementation Options)

## Overview

This specification covers a set of optional, non-breaking enhancements and implementation options for
JSONSchema-to-Mermaid. These are not required for core functionality, but are recommended for improved usability,
robustness, and maintainability. Each item is independent and may be implemented separately.

## Scope

This spec addresses the following enhancements:

1. **Config Precedence Policy**
    - Support for user-level and repository-level config file precedence.
    - Search parent directories (walk up to repo root) for config files.
    - Precedence order: CLI > project config > repo config > user config > defaults.

2. **Edge Case Tests for Config Files**
    - Add tests for:
        - Empty config file
        - Unknown keys in config
        - Mixed-case keys in config

3. **Integration Tests for CLI + Config Discovery**
    - Add integration tests that invoke `App().main` to exercise Clikt parsing and config-file discovery end-to-end.
    - Cover scenarios: CLI override, project config, user config, invalid config, missing config.

4. **README Example for --config-file**
    - Add a short CLI example in the README showing use of the `--config-file` flag.

## Acceptance Criteria

For each implemented item:

- Functionality is behind a flag or default behavior is clarified.
- At least one positive and one edge case test is present.
- README is updated (Usage, Limitations, or Examples section).
- No duplication of logic; code passes the full test suite.
- Error paths produce clear messages (checked via dedicated tests for failures where applicable).

## Details

### 1. Config Precedence Policy

- When resolving config, the tool should:
    1. Use CLI `--config-file` if provided.
    2. Search for `js2m.json` or `.js2mrc` in the working directory.
    3. Walk up parent directories to the repo root, using the first config found.
    4. Fall back to user home directory config (`~/.js2m.json` or `~/.js2mrc`).
    5. Use built-in defaults if no config is found.
- Document the search order in the README.

### 2. Edge Case Tests for Config Files

- Add unit tests for:
    - Empty config file (should not crash; should use defaults)
    - Config file with unknown keys (should ignore unknown keys, warn if desired)
    - Config file with mixed-case keys (should be case-insensitive)

### 3. Integration Tests for CLI + Config Discovery

- Add tests that run `App().main` with various config scenarios:
    - CLI override
    - Project config
    - User config
    - Invalid config (should print error)
    - Missing config (should use defaults)
- Tests should verify correct precedence and error handling.

### 4. README Example for --config-file

- Add a short example to the README:
  ```sh
  jsonschema-to-mermaid --config-file myconfig.json schema.json
  ```
- Briefly explain the effect and precedence.

## Out of Scope

- Breaking changes to config file format.
- Changes to core diagram generation logic.

## References

- See `PUBLIC_RELEASE_CHECKLIST.md` item 15 for original motivation.

---
*Spec generated 2025-12-19 by GitHub Copilot*
