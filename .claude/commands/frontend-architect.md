---
name: frontend-architect
description: Run the Frontend Architect Agent for a feature after BA, UX, and contract work are complete
arguments:
  - name: feature
    description: Feature name in kebab-case (must already have complete BA/UX docs in docs/features/ and endpoints in contracts/openapi.yaml)
    required: true
---

Run the Frontend Architect Agent for the feature named "{{feature}}".

Verify before proceeding:
1. All six BA/UX documents in docs/features/{{feature}}/ are present and non-empty.
2. contracts/openapi.yaml contains the endpoints for this feature.

If frontend/.claude/features/{{feature}}/spec.md already exists, enter revision mode:
read the existing spec and update only the sections affected by changes to BA/UX docs
or the contract. Do not rewrite from scratch.

Otherwise, produce both planning documents in frontend/.claude/features/{{feature}}/:
1. spec.md
2. tasks.md

Also create index.md and progress.md from the templates in frontend/.claude/templates/.
