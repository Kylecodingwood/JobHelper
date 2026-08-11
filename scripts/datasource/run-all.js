#!/usr/bin/env node
'use strict';

const { spawnSync } = require('child_process');
const path = require('path');

const ROOT = __dirname;
const PY = path.join(ROOT, '.venv', 'bin', 'python');

function run(cmd, args, label) {
  console.log(`\n========== ${label} ==========\n`);
  const r = spawnSync(cmd, args, { cwd: ROOT, stdio: 'inherit', env: process.env });
  if (r.error) throw r.error;
  return r.status ?? 1;
}

function main() {
  const started = Date.now();
  const codes = [];

  codes.push(run('node', ['freehire-fetch.js'], 'FreeHire API'));
  codes.push(run(PY, ['jobspy_fetch.py'], 'JobSpy (all sites)'));
  run('node', ['summarize.js'], 'Summary');

  const elapsed = ((Date.now() - started) / 1000).toFixed(1);
  console.log(`\n[run-all] finished in ${elapsed}s; step exit codes: ${codes.join(', ')}`);
  process.exit(Math.max(...codes, 0));
}

main();
