# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Session Protocol

1. Read this file + any `.claude/features/<FEATURE_NAME>/progress.md` at session start.
2. One feature at a time.
3. Update `progress.md` and `index.md` before session ends.

## Domain

A multi-tenant application for managing independent music labels.

- **Users** own one or more **Labels**
- **Labels** have **Artists** and **Releases**
- **Releases** contain **Tracks** (with artist, duration) and can be physical (vinyl, CD) or digital
- **Costs** track expenses (mastering, distribution) with VAT calculations and document attachments

## Key References

| Topic | File |
|-------|------|
| Architecture, module structure, patterns | [ARCHITECTURE.md](ARCHITECTURE.md) |
| Testing strategy and patterns | [TESTING.md](TESTING.md) |
| Development workflow, formatting, logging | [DEVELOPMENT.md](DEVELOPMENT.md) |

## Statuses

All status fields: `Draft` → `In Progress` → `In Review` → `Done`

## Roles

Tell Claude: "Act as [Role] for feature [FEATURE_NAME]."

- **Analyst** → `.claude/profiles/analyst.md`
- **Architect** → `.claude/profiles/architect.md`
- **Developer** → `.claude/profiles/developer.md`
- **Reviewer** → `.claude/profiles/reviewer.md`

## Feature Workflow

Each feature lives in `.claude/features/<FEATURE_NAME>/`.
Templates are in `.claude/templates/`.

```
Analyst   → index.md, progress.md, requirements.md, context.md
Architect → spec.md, tasks.md, updates index.md + progress.md
Developer → implements, checks off tasks, updates index.md + progress.md
Reviewer  → comments.md, updates index.md + progress.md
Developer → addresses comments, updates progress.md
Reviewer  → resolves comments → updates index.md status to Done
```

## Git Workflow

- Create a feature branch per feature: `feature/<FEATURE_NAME>`
- Merge to `main` via pull request when Done
- Always show the proposed commit message and ask for confirmation before executing a commit

## Quick Command Reference

```bash
./gradlew build                             # Build
./gradlew bootRun                           # Run application
./gradlew test                              # All Java tests
./gradlew test --tests LabelControllerTest  # Single test class
./gradlew checkstyleMain checkstyleTest     # Checkstyle
npm run test                                # JavaScript unit tests (Thymeleaf static JS — temporary)
```
