# Public Release Checklist (Outstanding Items)

This file lists remaining tasks identified during the review that have NOT yet been addressed. Ordered by priority (highest first). Completed fixes such as inheritance arrow direction, hiding inherited fields, transitive inheritance handling, cycle detection, and improved error messages are intentionally excluded.

## ✅ Completed Since Last Review

15. [X] Additional optional followups (implementation options)
    - Add support for a user-level AND repository-level precedence policy that includes searching parent directories (e.g., walk up to the repo root).
    - Add more tests for edge cases (empty config file, unknown keys, mixed-case keys).
    - Add integration tests that run the `App().main` invocation to exercise Clikt parsing together with config-file discovery.
    - Add a README example that shows `--config-file` usage on the command line (short CLI examples).

16. [X] Composition Visualization Options
    - Offer `--allof-mode=merge|inherit|compose` and treat multiple object `allOf` segments as inheritance or aggregation.

## 🟡 Priority: Nice to Have / Future Enhancements

17. [ ] PatternProperties Rendering Modes
    - Choose representation: field note vs separate synthetic class vs Map<Regex,Type>.

18. [ ] Performance / Large Schema Sets
    - Add lazy processing, streaming, or ability to split large diagrams into modules.

19. [ ] External Plugin or API Mode
    - Provide library API returning structured model (classes & relations) for embedding in other tools.

20. [ ] Diagram Layout Guidance
    - Add optional grouping by source file or namespace cluster (Mermaid hints / subgraphs).

21. [ ] Warning System / Diagnostics Flags
    - `--warn-collisions`, `--warn-unsupported-keywords` display findings on stderr.

22. [ ] Contributing / Governance Docs
    - Add `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, issue & PR templates.

23. [ ] CHANGELOG.md & Versioning Policy
    - Introduce semantic versioning; changelog entries for added/changed/removed features.

24. [X] Add CLI config-file discovery and tests (6a)
    - Action: Add `--config-file` flag and discovery for `js2m.json` / `.js2mrc` in project and user home.
    - Acceptance: Tests cover: project-level config, CLI override, invalid JSON handling (stderr).

## ⚪ Research / Deferred Decisions

25. [ ] Support Advanced JSON Schema Keywords
    - `if/then/else`, `dependentSchemas`, `$defs` (2020-12), `unevaluatedProperties`, `additionalItems`.
    - Requires careful visual mapping; defer until core set stable.

26. [ ] Remote Fetch Security
    - HTTP `$ref` introduces security considerations (SSRF, large downloads); design safe fetch policy (size limit, whitelist domains).

27. [ ] Configurable Type Mapping
    - Allow user-supplied mapping (e.g., map `string` with `format: date-time` to `Instant` instead of `String`).

28. [ ] Multi-Diagram Output
    - Split generation if number of classes exceeds threshold; generate index file linking diagrams.

29. [ ] Mermaid Theme / Styling Hooks
    - Provide placeholders for customizing colors or stereotypes (requires Mermaid syntax extensions or comments).

30. [ ] Make sure we have 100% test coverage


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
