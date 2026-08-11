#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');

const OUT_DIR = path.join(__dirname, 'out');

function readJson(name) {
  const p = path.join(OUT_DIR, name);
  if (!fs.existsSync(p)) return null;
  return JSON.parse(fs.readFileSync(p, 'utf8'));
}

function urlHost(url) {
  try {
    return new URL(url).hostname.replace(/^www\./, '');
  } catch {
    return null;
  }
}

function titleLooksSoftware(title = '') {
  return /software|developer|engineer|backend|frontend|full.?stack|devops|sde|graduate|intern|junior/i.test(
    title
  );
}

function summarizeFreehire(data) {
  if (!data) return { status: 'missing' };
  const jobs = data.jobs || [];
  const errors = data.errors || [];
  const networkBlocked = errors.some((e) =>
    /connect timeout|fetch failed|UND_ERR_CONNECT_TIMEOUT/i.test(e.error || '')
  );
  const withUrl = jobs.filter((j) => j.url);
  const hosts = {};
  for (const j of withUrl) {
    const h = urlHost(j.url);
    if (h) hosts[h] = (hosts[h] || 0) + 1;
  }
  return {
    status: networkBlocked && jobs.length === 0 ? 'network_blocked' : 'ok',
    unique_jobs: jobs.length,
    query_errors: errors.length,
    network_blocked: networkBlocked,
    software_like_titles: jobs.filter((j) => titleLooksSoftware(j.title)).length,
    with_apply_url: withUrl.length,
    top_ats_hosts: Object.entries(hosts)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 8)
      .map(([host, count]) => ({ host, count })),
    sample_titles: jobs.slice(0, 5).map((j) => j.title),
    error_sample: errors[0]?.error || null,
  };
}

function summarizeJobspy(data) {
  if (!data) return { status: 'missing' };
  const jobs = data.jobs || [];
  const bySite = {};
  for (const j of jobs) {
    bySite[j.site] = (bySite[j.site] || 0) + 1;
  }
  return {
    status: 'ok',
    unique_jobs: jobs.length,
    run_errors: (data.errors || []).length,
    by_site: bySite,
    software_like_titles: jobs.filter((j) => titleLooksSoftware(j.title)).length,
    failed_runs: (data.errors || []).map((e) => e.label),
    sample_titles: jobs.slice(0, 5).map((j) => j.title),
  };
}

function overlap(freehire, jobspy) {
  if (!freehire?.jobs?.length || !jobspy?.jobs?.length) return { shared_urls: 0 };
  const fhUrls = new Set(freehire.jobs.map((j) => j.url).filter(Boolean));
  const shared = jobspy.jobs.filter((j) => j.url && fhUrls.has(j.url));
  return { shared_urls: shared.length, examples: shared.slice(0, 5).map((j) => j.url) };
}

function verdict(fh, js) {
  const notes = [];
  let feasible = true;

  if (!fh || fh.status === 'missing') {
    feasible = false;
    notes.push('FreeHire dataset missing — API fetch failed or not run.');
  } else if (fh.status === 'network_blocked') {
    feasible = false;
    notes.push(
      'FreeHire API unreachable from this network (connect timeout to freehire.me:443). Try VPN or run on another host.'
    );
  } else if (fh.unique_jobs === 0) {
    feasible = false;
    notes.push('FreeHire returned zero Ireland jobs for configured queries.');
  } else {
    notes.push(`FreeHire: ${fh.unique_jobs} unique IE jobs; ${fh.software_like_titles} software-like titles.`);
  }

  if (!js || js.status === 'missing') {
    feasible = false;
    notes.push('JobSpy dataset missing — Python run failed or not run.');
  } else if (js.unique_jobs === 0) {
    notes.push('JobSpy returned zero jobs (likely blocks or empty market on all sites).');
    feasible = false;
  } else {
    notes.push(`JobSpy: ${js.unique_jobs} unique jobs across ${Object.keys(js.by_site || {}).length} sites.`);
    if (js.run_errors > 0) notes.push(`${js.run_errors} site×term runs failed (403/captcha/library bug).`);
  }

  const jobspyOk = (js?.unique_jobs || 0) > 0;
  const freehireOk = fh?.unique_jobs > 0;

  return {
    dual_source_feasible: freehireOk && jobspyOk,
    partial_feasible: freehireOk || jobspyOk,
    recommendation: freehireOk
      ? 'Use FreeHire as primary discovery; merge JobSpy by apply URL.'
      : jobspyOk
        ? 'JobSpy (LinkedIn+Indeed) works now; unblock FreeHire network or run freehire-fetch.js elsewhere and merge JSON.'
        : 'Both sources failed — check network and retry.',
    notes,
  };
}

function main() {
  const freehire = readJson('freehire-latest.json');
  const jobspy = readJson('jobspy-latest.json');
  const report = {
    generated_at: new Date().toISOString(),
    freehire: summarizeFreehire(freehire),
    jobspy: summarizeJobspy(jobspy),
    overlap: overlap(freehire, jobspy),
    verdict: verdict(summarizeFreehire(freehire), summarizeJobspy(jobspy)),
  };

  fs.writeFileSync(path.join(OUT_DIR, 'summary-latest.json'), JSON.stringify(report, null, 2));

  const combined = {
    generated_at: report.generated_at,
    total_unique: (freehire?.jobs?.length || 0) + (jobspy?.jobs?.length || 0),
    sources: {
      freehire: freehire?.jobs || [],
      jobspy: jobspy?.jobs || [],
    },
    summary: report,
  };
  fs.writeFileSync(path.join(OUT_DIR, 'dataset-latest.json'), JSON.stringify(combined, null, 2));

  console.log(JSON.stringify(report, null, 2));
}

main();
