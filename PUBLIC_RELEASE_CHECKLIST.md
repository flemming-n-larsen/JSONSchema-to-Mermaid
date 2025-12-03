# Public Release Checklist (Outstanding Items)

This file lists remaining tasks identified during the review that have NOT yet been addressed. Ordered by priority (highest first). Completed fixes such as inheritance arrow direction, hiding inherited fields, transitive inheritance handling, cycle detection, and improved error messages are intentionally excluded.

## ✅ Completed Since Last Review

6. CLI Configuration Flags
    - Problem: Preferences (arrays as relations, required marker style, enum rendering mode) hardcoded.
    - Action: Figure out which options to expose and there names.
    - Action: Add flags: `--arrays-as-relation/--arrays-inline`, `--required-style=plus|none|suffix-q`, `--enum-style=inline|note|class`.
    - Acceptance: Help text (`--help`) lists flags; tests exercise at least one flag changing output.

## 🟡 Priority: Nice to Have / Future Enhancements

14. Enum Stereotype Rendering (If separate class chosen)
    - Add `<<enumeration>>` stereotype for dedicated enum classes.

15. Optional Field Nullability Marker
    - Use `?` suffix or separate styling when not required.

16. Composition Visualization Options
    - Offer `--allof-mode=merge|inherit|compose` and treat multiple object `allOf` segments as inheritance or aggregation.

17. PatternProperties Rendering Modes
    - Choose representation: field note vs separate synthetic class vs Map<Regex,Type>.

18. Performance / Large Schema Sets
    - Add lazy processing, streaming, or ability to split large diagrams into modules.

19. External Plugin or API Mode
    - Provide library API returning structured model (classes & relations) for embedding in other tools.

20. Diagram Layout Guidance
    - Add optional grouping by source file or namespace cluster (Mermaid hints / subgraphs).

21. Warning System / Diagnostics Flags
    - `--warn-collisions`, `--warn-unsupported-keywords` display findings on stderr.

22. Contributing / Governance Docs
    - Add `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, issue & PR templates.

23. CHANGELOG.md & Versioning Policy
    - Introduce semantic versioning; changelog entries for added/changed/removed features.

## ⚪ Research / Deferred Decisions

24. Support Advanced JSON Schema Keywords
    - `if/then/else`, `dependentSchemas`, `$defs` (2020-12), `unevaluatedProperties`, `additionalItems`.
    - Requires careful visual mapping; defer until core set stable.

25. Remote Fetch Security
    - HTTP `$ref` introduces security considerations (SSRF, large downloads); design safe fetch policy (size limit, whitelist domains).

26. Configurable Type Mapping
    - Allow user-supplied mapping (e.g., map `string` with `format: date-time` to `Instant` instead of `String`).

27. Multi-Diagram Output
    - Split generation if number of classes exceeds threshold; generate index file linking diagrams.

28. Mermaid Theme / Styling Hooks
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
4. enum rendering (inline variant minimal). — DONE
5. Remove unused dependency.
6. Add CLI flags for arrays/required/enum styles.
7. Golden snapshot tests.
8. CI workflow.
9. Name collision handling.
10. External refs expansion (optional for initial release).

---
Generated: 2025-10-31 (updated enum rendering status)
