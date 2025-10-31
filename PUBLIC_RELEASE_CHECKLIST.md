# Public Release Checklist (Outstanding Items)

This file lists remaining tasks identified during the review that have NOT yet been addressed. Ordered by priority (highest first). Completed fixes such as inheritance arrow direction, hiding inherited fields, transitive inheritance handling, cycle detection, and improved error messages are intentionally excluded.

## 🔴 Priority: Must Fix Before Public Release

4. Enum Rendering Absent
   - Problem: Enums appear only as primitive fields; no differentiation.
   - Action: Provide configuration: inline `{A|B|C}`, note, or separate `<<enumeration>>` class.
   - Acceptance: Test ensures enum values surface in diagram consistently.

5. Remove / Justify Unused Dependency `net.pwall.json:json-kotlin-schema`
   - Problem: Present in `build.gradle.kts` but not referenced in code.
   - Action: Remove or start using explicitly (e.g., schema validation). Prefer removal for minimal surface.
   - Acceptance: Build passes after removal; dependency no longer in `build.gradle.kts`.

6. Required Feature Documentation & Limitations Section
   - Problem: README lists capabilities that are partially or not implemented (patternProperties, enums, deeper composition semantics).
   - Action: Add a "Limitations" section enumerating unsupported JSON Schema keywords (e.g., `not`, `if/then/else`, `dependentSchemas`, external URL `$ref`).
   - Acceptance: README updated; confusion risk reduced.

7. Name Collision Handling
   - Problem: Different schemas can sanitize to same class name (e.g., `product.schema.json` vs nested definition `product`).
   - Action: Introduce disambiguation (namespace prefix, numeric suffix, or path-based hashing) and a warning when collisions occur.
   - Acceptance: Test: two colliding inputs produce distinct class names.

## 🟠 Priority: Should Fix Soon (Post-0.1.0 if Needed)

8. CLI Configuration Flags
   - Problem: Preferences (arrays as relations, required marker style, enum rendering mode) hardcoded.
   - Action: Add flags: `--arrays-as-relation/--arrays-inline`, `--required-style=plus|none|suffix-q`, `--enum-style=inline|note|class`.
   - Acceptance: Help text (`--help`) lists flags; tests exercise at least one flag changing output.

9. Snapshot / Golden Tests
   - Problem: Tests assert presence of substrings only; risk of unnoticed regressions.
   - Action: Add golden Mermaid output files for README examples and compare entire string.
   - Acceptance: Test suite includes snapshot tests; update workflow for intentional changes.

10. Continuous Integration (GitHub Actions)
   - Problem: No automated build/test on pushes or PRs.
   - Action: Add workflow: setup JDK + Gradle cache, run `./gradlew build`, optionally publish artifacts on tag.
   - Acceptance: Badge added to README; workflow green.

11. External `$ref` Support (File and HTTP URLs)
   - Problem: Only local same-directory refs supported.
   - Action: Add resolver for relative paths outside initial set and (optionally) HTTP with caching & timeout.
   - Acceptance: Test referencing another directory (and optionally remote) passes.

12. Improved Array Item Naming
   - Problem: Singularization by dropping trailing `s` is naive.
   - Action: Integrate a lightweight inflection library or custom rules (handle `companies` → `Company`).
   - Acceptance: Tests for plural edge cases.

13. Inheritance Visualization Preference
   - Problem: Behavior fixed to hide inherited fields; some users may want them.
   - Action: Add flag `--show-inherited-fields`.
   - Acceptance: Tests verify toggled display.

14. Refactor Vestigial Classes (`MermaidClassDiagramGenerator`, model classes)
   - Problem: Unused alternative generation path increases maintenance footprint.
   - Action: Remove or unify into a single internal representation with adapter to string output.
   - Acceptance: Codebase free of unused generator OR documented API usage.

15. Cycle Detection Test Coverage Enhancement
   - Problem: Only simple 2-node cycle tested.
   - Action: Add 3+ node cycle case, and ensure message chain ordering consistent.
   - Acceptance: Additional cycle test passes.

## 🟡 Priority: Nice to Have / Future Enhancements

16. Enum Stereotype Rendering (If separate class chosen)
   - Add `<<enumeration>>` stereotype for dedicated enum classes.

17. Optional Field Nullability Marker
   - Use `?` suffix or separate styling when not required.

18. Composition Visualization Options
   - Offer `--allof-mode=merge|inherit|compose` and treat multiple object `allOf` segments as inheritance or aggregation.

19. PatternProperties Rendering Modes
   - Choose representation: field note vs separate synthetic class vs Map<Regex,Type>.

20. Performance / Large Schema Sets
   - Add lazy processing, streaming, or ability to split large diagrams into modules.

21. External Plugin or API Mode
   - Provide library API returning structured model (classes & relations) for embedding in other tools.

22. Diagram Layout Guidance
   - Add optional grouping by source file or namespace cluster (Mermaid hints / subgraphs).

23. Warning System / Diagnostics Flags
   - `--warn-collisions`, `--warn-unsupported-keywords` display findings on stderr.

24. Contributing / Governance Docs
   - Add `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, issue & PR templates.

25. CHANGELOG.md & Versioning Policy
   - Introduce semantic versioning; changelog entries for added/changed/removed features.

## ⚪ Research / Deferred Decisions

26. Support Advanced JSON Schema Keywords
   - `if/then/else`, `dependentSchemas`, `$defs` (2020-12), `unevaluatedProperties`, `additionalItems`.
   - Requires careful visual mapping; defer until core set stable.

27. Remote Fetch Security
   - HTTP `$ref` introduces security considerations (SSRF, large downloads); design safe fetch policy (size limit, whitelist domains).

28. Configurable Type Mapping
   - Allow user-supplied mapping (e.g., map `string` with `format: date-time` to `Instant` instead of `String`).

29. Multi-Diagram Output
   - Split generation if number of classes exceeds threshold; generate index file linking diagrams.

30. Mermaid Theme / Styling Hooks
   - Provide placeholders for customizing colors or stereotypes (requires Mermaid syntax extensions or comments).

## Cross-Cutting Acceptance Criteria Template (for each implemented item)
- Functionality implemented behind a flag or default behavior clarified.
- Tests: At least one positive + one edge case.
- README updated (Usage, Limitations, or Examples section).
- No duplication of logic; code passes existing test suite.
- Error paths produce clear messages (checked via dedicated tests for failures where applicable).

## Suggested Implementation Order (Fast Win Path)
1. Required semantics correction.
2. allOf inline merge.
3. patternProperties decision & README limitations.
4. enum rendering (inline variant minimal).
5. Remove unused dependency.
6. Add CLI flags for arrays/required/enum styles.
7. Golden snapshot tests.
8. CI workflow.
9. Name collision handling.
10. External refs expansion (optional for initial release).

---
Generated: 2025-10-31

