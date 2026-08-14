// Parses openapi.yaml and asserts the structural invariants a regex sweep cannot see.
//
// This exists because the spec was once committed in a state no YAML parser accepted — 404
// blocks indented under `operationId:` instead of `responses:` — and nothing in the build
// noticed. A check that does not load the document is not a check.
//
// It deliberately does not verify the spec against the running application: that is the
// conformance test in Phase 3, which boots the app and diffs /v3/api-docs.

import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import yaml from 'js-yaml';

const here = dirname(fileURLToPath(import.meta.url));
const file = join(here, 'openapi.yaml');

const problems = [];
let doc;

try {
  doc = yaml.load(readFileSync(file, 'utf8'));
} catch (error) {
  console.error(`openapi.yaml does not parse:\n  ${error.message}`);
  process.exit(1);
}

const METHODS = ['get', 'put', 'post', 'delete', 'patch', 'options', 'head', 'trace'];

if (!doc?.paths) problems.push('no paths object');

let operationCount = 0;
const operationIds = new Map();

for (const [path, item] of Object.entries(doc.paths ?? {})) {
  for (const [method, operation] of Object.entries(item ?? {})) {
    if (!METHODS.includes(method)) continue;
    operationCount++;
    const where = `${method.toUpperCase()} ${path}`;

    if (!operation.operationId) problems.push(`${where}: no operationId`);
    else if (operationIds.has(operation.operationId)) {
      problems.push(
        `${where}: operationId "${operation.operationId}" also used by ${operationIds.get(operation.operationId)}`,
      );
    } else operationIds.set(operation.operationId, where);

    if (!operation.responses || Object.keys(operation.responses).length === 0) {
      problems.push(`${where}: no responses`);
    }
  }
}

// Every $ref must resolve against the loaded document, not merely look well-formed.
(function walk(node, trail) {
  if (!node || typeof node !== 'object') return;
  for (const [key, value] of Object.entries(node)) {
    if (key === '$ref' && typeof value === 'string') {
      if (!value.startsWith('#/')) {
        problems.push(`${trail}: external $ref "${value}" — this spec is self-contained`);
        continue;
      }
      let target = doc;
      for (const segment of value.slice(2).split('/')) target = target?.[segment];
      if (target === undefined) problems.push(`${trail}: unresolved $ref "${value}"`);
    } else walk(value, `${trail}/${key}`);
  }
})(doc, '');

if (problems.length > 0) {
  console.error(`openapi.yaml has ${problems.length} problem(s):`);
  for (const problem of problems) console.error(`  - ${problem}`);
  process.exit(1);
}

console.log(
  `openapi.yaml OK — ${Object.keys(doc.paths).length} paths, ${operationCount} operations, all $refs resolve`,
);
