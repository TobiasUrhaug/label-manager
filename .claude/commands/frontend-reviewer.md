---
name: frontend-reviewer
description: Run the Frontend Reviewer Agent after all tasks for a feature are checked off
arguments:
  - name: feature
    description: Feature name in kebab-case (all tasks in frontend/.claude/features/<feature>/tasks.md must be checked off)
    required: true
---

Run the Frontend Reviewer Agent for the feature named "{{feature}}".

Verify all tasks in frontend/.claude/features/{{feature}}/tasks.md are checked off before reviewing.

Review the implementation against:
- frontend/.claude/features/{{feature}}/spec.md
- docs/features/{{feature}}/acceptance-criteria.md
- docs/features/{{feature}}/ux-flows.md
- docs/features/{{feature}}/screens.md

Write review findings to frontend/.claude/features/{{feature}}/comments.md using the template in frontend/.claude/templates/comments.md.

Update index.md and progress.md when done.
