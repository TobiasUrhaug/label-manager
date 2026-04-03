---
name: contract
description: Run the Contract Agent for a feature after UX documents are complete
arguments:
  - name: feature
    description: Feature name in kebab-case (must already have all six BA/UX documents in docs/features/)
    required: true
---

Run the Contract Agent for the feature named "{{feature}}".

Verify that all six documents exist and are non-empty in docs/features/{{feature}}/ before proceeding.

Derive the REST API contract from the feature documents and write the new paths
and schemas into contracts/openapi.yaml:
1. Draft the new endpoints and schemas
2. Present the draft and wait for approval before writing

Report what was added and hand off to Frontend Agent and Backend Agent.
