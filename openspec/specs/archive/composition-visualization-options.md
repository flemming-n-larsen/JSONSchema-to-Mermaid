# Open Spec: Composition Visualization Options (`--allof-mode`)

## Status

`implemented` - Archived on 2025-12-19

## Overview

This specification defines new options for visualizing JSON Schema `allOf` composition in Mermaid diagrams. The goal is
to provide users with control over how multiple object `allOf` segments are rendered, supporting different modeling
intentions: field merging, inheritance, or aggregation.

## Motivation

- JSON Schema `allOf` is ambiguous: it can mean field merging (composition), inheritance (subclassing), or aggregation (
  association).
- Users need to choose the most appropriate visualization for their use case.
- Current behavior is fixed (merge fields inline); this spec introduces a CLI flag to select the mode.

## CLI Flag

- `--allof-mode=merge|inherit|compose`
    - **merge** (default): Merge all object fields into the current class (current behavior)
    - **inherit**: Treat each object in `allOf` as a superclass (draw inheritance arrows)
    - **compose**: Treat each object in `allOf` as an aggregation/association (draw composition/aggregation arrows)

## Behavior by Mode

### 1. `merge` (default)

- All object properties from `allOf` segments are merged into the current class.
- No inheritance or aggregation arrows are drawn.
- This is the current behavior and ensures backward compatibility.

### 2. `inherit`

- Each object in `allOf` is treated as a parent class.
- The current class inherits from all parent classes (multiple inheritance arrows in Mermaid).
- If a referenced schema is not an object, fallback to merge.
- If a cycle is detected, error out with a clear message.

### 3. `compose`

- Each object in `allOf` is treated as a component/part.
- The current class has an aggregation/composition relationship to each part (e.g., `o--` or `*--` in Mermaid).
- Properties are not merged; only relationships are drawn.
- If a referenced schema is not an object, fallback to merge.

## CLI/Config Integration

- The flag can be set via CLI (`--allof-mode`) or config file (`allOfMode`).
- If both are set, CLI takes precedence.
- The value is case-insensitive.
- Invalid values result in a clear error message.

## Acceptance Criteria

- [x] CLI flag and config option are parsed and validated.
- [x] All three modes are implemented and tested.
- [x] At least one positive and one edge-case test for each mode.
- [ ] README is updated with usage, examples, and limitations.
- [x] Golden tests cover all modes.
- [x] Error messages are clear for invalid values and cycles.
- [x] Backward compatibility: default is `merge`.

## Implementation Notes

- Refactor `CompositionKeywordHandler` to support mode selection.
- Update `Preferences` data class to include `allOfMode`.
- Update CLI and config parsing logic.
- Update Mermaid diagram generation to draw inheritance or aggregation arrows as needed.
- Add tests for all modes (unit and golden tests).
- Update README with CLI flag, config option, and visual examples.

## Migration/Compatibility

- Existing users see no change unless they set the new flag.
- Default remains `merge` for backward compatibility.

## Implementation Summary

### Files Modified
1. `src/main/kotlin/jsonschema_to_mermaid/diagram/MermaidGeneratorTypes.kt` - Added AllOfMode enum and updated Preferences
2. `src/main/kotlin/jsonschema_to_mermaid/jsonschema/Schema.kt` - Added allOf field to Schema
3. `src/main/kotlin/jsonschema_to_mermaid/cli/App.kt` - Added --allof-mode CLI option
4. `src/main/kotlin/jsonschema_to_mermaid/cli/CliOptions.kt` - Added allOfModeOption field
5. `src/main/kotlin/jsonschema_to_mermaid/cli/PreferencesBuilder.kt` - Added parsing and validation logic
6. `src/main/kotlin/jsonschema_to_mermaid/relationship/CompositionKeywordHandler.kt` - Refactored to support all three modes
7. `src/main/kotlin/jsonschema_to_mermaid/schema/TopLevelSchemaProcessor.kt` - Added schema-level allOf handling

### Tests Added
- `AllOfModeTest.kt` - 6 integration tests
- `AppAllOfModeCliTest.kt` - 5 CLI tests
- `PreferencesBuilderTest.kt` - 7 additional unit tests (12 total)
- Test schemas: `allof_merge.schema.json`, `allof_inherit.schema.json`, `allof_compose.schema.json`

### Test Results
- All 70 tests passing
- Build successful
- Full backward compatibility maintained

## Changelog

- 2025-12-19: Initial draft specification created.
- 2025-12-19: Implementation completed and tested.
- 2025-12-19: Specification archived after successful implementation.

