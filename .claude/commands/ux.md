---
name: ux
description: Run the UX Agent for a feature after BA documents are complete
arguments:
  - name: feature
    description: Feature name in kebab-case (must already have complete BA documents in docs/features/)
    required: true
---

Run the UX Agent for the feature named "{{feature}}".

Verify that all four BA documents exist and are non-empty in docs/features/{{feature}}/ before proceeding.

Produce both UX documents in docs/features/{{feature}}/:
1. ux-flows.md
2. screens.md

Present a draft of each document and wait for approval before writing the file.
