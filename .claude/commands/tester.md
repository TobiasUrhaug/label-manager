---
name: tester
description: Run the Tester Agent after frontend and backend implementation is complete
arguments:
  - name: feature
    description: Feature name in kebab-case (must already have acceptance-criteria.md in docs/features/)
    required: true
---

Run the Tester Agent for the feature named "{{feature}}".

Read docs/features/{{feature}}/acceptance-criteria.md and decide whether Playwright
e2e tests are needed. For each criterion, apply the decision heuristic and produce a
verdict. If e2e tests are warranted, scope the scenarios. If not, document the rationale.

Do not read implementation code.
