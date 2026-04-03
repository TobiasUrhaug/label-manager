---
name: frontend-developer
description: Run the Frontend Developer Agent to implement tasks for a feature
arguments:
  - name: feature
    description: Feature name in kebab-case (must already have spec.md and tasks.md in frontend/.claude/features/)
    required: true
---

Run the Frontend Developer Agent for the feature named "{{feature}}".

First, check whether frontend/.claude/features/{{feature}}/comments.md exists and has
unresolved 🔴 Must Fix items. If yes, enter review-response mode (address each comment,
add responses, then tell the user to run /frontend-reviewer). Otherwise, enter
implementation mode.

Implementation mode — read spec.md, tasks.md, and progress.md, then implement tasks
one at a time using Red-Green-Refactor:
1. Write a failing test (Red)
2. Write minimal code to pass (Green)
3. Refactor
4. Check off the task and update progress.md
5. Show the proposed commit message and ask for confirmation before committing

When all tasks are complete, update index.md and progress.md and tell the user the
feature is ready for the Frontend Reviewer.
