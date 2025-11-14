# Prioritized TODO List for JSONSchema-to-Mermaid

Each item is an actionable task, prioritized by expected user benefit and impact. Address the highest priority items
first. For each, provide robust, well-tested solutions and update documentation as needed.

1. Implement class name disambiguation to avoid name collisions when schemas sanitize to the same class name.
2. Improve patternProperties support: visualize multiple patterns distinctly in Mermaid diagrams.
3. Enhance plural-to-singular conversion for array item naming (handle irregular plurals and edge cases).
4. Support merging multiple object segments in allOf for multiple inheritance scenarios.
5. Map the `format` keyword to special types (e.g., convert `date-time` to `Date` in Mermaid diagrams).
6. Support external $ref to files in other directories or via HTTP URLs, not just local or same-directory references.
7. Gradually add support for more advanced/unsupported JSON Schema keywords (e.g., `not`, `if`/`then`/`else`,
   `dependentSchemas`, `$defs`, `unevaluatedProperties`, `const`, `contains`, `propertyNames`).
8. Add unit tests that assert presence of certain Mermaid nodes/edges when given specific schema snippets.
9. Add an integration smoke test that runs the CLI on `src/test/resources/bookstore` and verifies non-empty output.
10. Move more presentation decisions to a small Config/Preferences DTO to let the CLI toggle array handling or required
    notation.
11. Add stricter snapshot tests (exact equality) for the README examples.
12. Tweak the class naming heuristics further (e.g., use a singularization library for array item naming).

For each task, ensure code quality, maintainability, and user documentation are updated. Prioritize tasks from the top
of the list.
