# CLAUDE.md — Coding Guidelines

## Response Protocol
- **Prioritize clarity, correctness, and directness** over length.
- **Keep responses meaningful and focused** — avoid verbose explanations.
- **Show, don't tell** — deliver working, tested code instead of describing it.
- **Skip preambles/postambles** — go straight to the solution.
- **Assume developer-level competence** — omit basic explanations unless requested.
- **Show only what's necessary** — partial diffs or relevant snippets preferred over full files.

## Code Generation Rules

### Naming & Style
- No explanatory comments — code must be self-documenting.
- No single-letter variables — use meaningful names.
- Use English for all identifiers.
- Use `var` for local variables when possible.
- Follow IntelliJ’s default Java formatting.
- Use `@NonNull` / `@Nullable` (JSR-305) consistently.

### Architecture & Design
- Prefer **functional style** (lambdas, streams, immutability).
- Prefer **pure functions** and small, cohesive methods.
- Always create **specific custom exceptions** for distinct business cases.
- Map exceptions to **Problem+JSON** HTTP responses.
- No unreachable or dead code.

### Quality & Maintenance
- Format files and remove unused imports after generation/refactor.
- Keep logs minimal, meaningful, and context-rich.
- Always create **unit tests** for new or modified code.
- Always document endpoints with **Swagger** (`@Operation`, `@ApiResponse`, with examples).
- Use **concise, expressive method names** to clarify behavior.
- **Fix all compilation errors immediately**, especially in tests.

## Mandatory Checklist
For every code change:
- ✓ Unit tests created or updated.
- ✓ Swagger documentation added (if endpoint).
- ✓ File formatted, imports cleaned.
- ✓ Custom exceptions defined if applicable.
- ✓ No comments; code is self-explanatory.

---

## Authorization
Claude may autonomously execute all allowed commands **except** `git commit`, `git add`, and `git push`.