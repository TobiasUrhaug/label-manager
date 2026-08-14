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

    if (!operation || typeof operation !== 'object') {
      problems.push(`${where}: empty operation`);
      continue;
    }

    // Type, not truthiness. A response block indented one level too far lands *under*
    // operationId, which is still valid YAML — operationId just becomes a map. That is the
    // exact slip this script exists to catch, and a truthiness check waves it through.
    if (typeof operation.operationId !== 'string') {
      if (operation.operationId === undefined) problems.push(`${where}: no operationId`);
      else if (operation.operationId === null) problems.push(`${where}: operationId is empty`);
      else {
        problems.push(
          `${where}: operationId is ${typeof operation.operationId}, not a string — something is indented under it`,
        );
      }
    } else if (operation.operationId.trim() === '') {
      // A quoted "" is a string, so it slips past the null check above.
      problems.push(`${where}: operationId is empty`);
    } else if (operationIds.has(operation.operationId)) {
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
      for (const segment of value.slice(2).split('/')) {
        // RFC 6901: ~1 is an escaped "/", ~0 an escaped "~". Path keys contain slashes, so a
        // ref to a path item is all escapes; resolving without decoding fails on a valid spec.
        const segmentKey = segment.replace(/~1/g, '/').replace(/~0/g, '~');
        // Own properties only. Plain member access walks the prototype chain, so a typo'd
        // ref to "constructor" or "toString" resolves to a Function and passes.
        if (target === null || typeof target !== 'object' || !Object.hasOwn(target, segmentKey)) {
          target = undefined;
          break;
        }
        target = target[segmentKey];
      }
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
