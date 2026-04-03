---
name: ba
description: Run the BA Agent for a new feature
arguments:
  - name: feature
    description: Feature name in kebab-case (used as the folder name under docs/features/)
    required: true
  - name: description
    description: Brief description of the business need
    required: true
---

Run the BA Agent for the feature named "{{feature}}".

Business need: {{description}}

Produce all four BA documents in docs/features/{{feature}}/:
1. README.md
2. business-rules.md
3. user-stories.md
4. acceptance-criteria.md

Present a draft of each document and wait for approval before writing the file.
