# CLAUDE.md

# 공통 기본 규칙입니다. 수정이 필요하다면 공유 후 수정 부탁드립니다.

## Response Style

- State the conclusion first.
- Keep explanations concise (≤5 lines when possible).
- Explain only the key reason behind code changes.
- Avoid repetition and filler.

## Workflow

1. Understand the requirement.
2. Inspect the existing implementation.
3. Implement with minimal changes.
4. Verify when possible.
5. Summarize the changes.

## Engineering Principles

### Correctness

- State assumptions explicitly.
- Use only verifiable information.
- Distinguish facts from assumptions.
- Ask questions instead of guessing.

### Code

- Respect the existing architecture.
- Keep changes minimal and focused.
- Avoid unnecessary abstractions (YAGNI).
- Do not refactor unrelated code.
- Modify existing code before creating new files or layers.

### Frontend

- Read `claude/design.md` before UI work.
- Follow the existing design system.
- Preserve behavior unless requested.
- Prefer CSS/Tailwind over JSX changes for visual updates.

### TypeScript

- Follow strict mode.
- Never use `any` unless explicitly approved.
- Prefer type inference and local types.
- Create new interfaces/types only when they improve the current implementation.
- Minimize type assertions.

### Debugging

- Fix root causes, not symptoms.
- Do not hide errors.
- Add temporary debug logs when useful.
- Mark temporary logs with `// TODO: remove debug logs`.

### Testing

- Update tests when behavior changes.
- Recommend tests if none exist.
- Never claim success without verification.

### Dependencies

- Reuse existing dependencies.
- Get approval before adding new ones.

### Git & Security

- Commit/push only when requested.
- Never run destructive Git commands.
- Never expose secrets.
- Confirm before modifying `.env`.

### Language

- Default response: Korean.
- Keep code, filenames, CLI, APIs, and errors in their original language.

## Engineering Mindset

- Correctness over cleverness.
- Simplicity over abstraction.
- Readability over clever design.
- Evidence over assumptions.

### MVP Development

This project is an MVP and requirements change frequently.

- Optimize for iteration speed over architectural perfection.
- Prefer code that is easy to modify tomorrow over code prepared for hypothetical future requirements.
- Avoid creating interfaces, hooks, utilities, wrappers, or extension points without an immediate use case.
- Keep logic in the same file if it has only one consumer.
- Prefer modifying existing files over creating new ones.
- Small duplication is acceptable when it improves clarity.
- Refactor only after duplication or complexity becomes a real problem.

### Decision Making

- When multiple solutions are valid, choose the simplest one.
- If two solutions are equally correct, choose the one with fewer files and less code.
