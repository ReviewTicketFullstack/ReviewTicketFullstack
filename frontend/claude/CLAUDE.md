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
3. Propose a plan if needed.
4. Implement and verify.
5. Summarize the changes.

## Engineering Principles

### Correctness

- State assumptions explicitly.
- Use only verifiable information.
- Distinguish facts from assumptions.
- Ask questions instead of guessing.

### Code

- Respect the existing architecture and patterns.
- Keep changes minimal and focused.
- Avoid unnecessary abstractions (YAGNI).
- Do not refactor unrelated code.

### Debugging

- Fix root causes, not symptoms.
- Do not hide errors.
- Consider failure paths.
- Explain remaining uncertainty.

### Frontend

- Follow the existing design system.
- Avoid generic AI-style UI.
- Preserve DOM structure, state, props, and behavior unless requested.
- Prefer CSS/Tailwind over JSX changes for visual updates.

### TypeScript

- Never use `any`; prefer `unknown`.
- Minimize type assertions.
- Follow strict mode.

### Testing

- Update tests when behavior changes.
- Recommend tests if none exist.
- Never claim success without verification.

### Dependencies

- Reuse existing dependencies.
- Explain and get approval before adding new ones.

### Git & Security

- Commit/push only when requested.
- Never run destructive Git commands.
- Never expose secrets.
- Confirm before modifying `.env`.

### Performance

- Consider complexity for large datasets.
- Explain performance trade-offs.
- Optimize only with measurable evidence.

### Language

- Default response: Korean.
- Keep code, filenames, CLI, APIs, and errors in their original language.

## Engineering Mindset

- Correctness over cleverness.
- Readability over brevity.
- Evidence over assumptions.
