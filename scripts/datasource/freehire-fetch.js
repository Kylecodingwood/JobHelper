#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');
const { fetchJson, stamp, ensureDirSync } = require('./lib/http');

const BASE = 'https://freehire.me/api/v1/jobs/search';
const OUT_DIR = path.join(__dirname, 'out');

/** Ireland-focused discovery queries — facet filters + keywords */
const QUERIES = [
  { label: 'ie-software-engineer', params: { countries: 'ie', q: 'software engineer', limit: 100 } },
  { label: 'ie-graduate-developer', params: { countries: 'ie', q: 'graduate developer', limit: 100 } },
  { label: 'ie-junior-backend', params: { countries: 'ie', seniority: 'junior', category: 'backend', limit: 100 } },
  { label: 'ie-backend', params: { countries: 'ie', category: 'backend', limit: 100 } },
  { label: 'ie-dublin-software', params: { countries: 'ie', cities: 'Dublin', q: 'software', limit: 100 } },
  { label: 'ie-fullstack', params: { countries: 'ie', category: 'fullstack', limit: 100 } },
  { label: 'ie-devops', params: { countries: 'ie', category: 'devops', limit: 100 } },
  { label: 'ie-intern', params: { countries: 'ie', q: 'intern software', limit: 100 } },
];

const MAX_PAGES_PER_QUERY = 10; // up to 1000 jobs/query (API cap offset+limit ≤ 10000)

function buildUrl(params, offset = 0) {
  const u = new URL(BASE);
  for (const [k, v] of Object.entries(params)) {
    if (v != null && v !== '') u.searchParams.set(k, String(v));
  }
  u.searchParams.set('offset', String(offset));
  u.searchParams.set('semantic_ratio', '0');
  return u.toString();
}

function normalizeJob(raw) {
  return {
    source: 'freehire',
    id: raw.public_slug || raw.external_id || null,
    title: raw.title,
    company: raw.company,
    company_slug: raw.company_slug,
    location: raw.location,
    url: raw.url,
    countries: raw.countries || [],
    cities: raw.cities || [],
    work_mode: raw.work_mode,
    seniority: raw.enrichment?.seniority || null,
    category: raw.enrichment?.category || null,
    skills: raw.skills || [],
    posted_at: raw.posted_at,
    ats_source: raw.source,
    is_tech: raw.is_tech,
  };
}

async function fetchQuery(queryDef) {
  const jobs = [];
  let total = null;
  let offset = 0;
  const limit = Number(queryDef.params.limit) || 100;

  for (let page = 0; page < MAX_PAGES_PER_QUERY; page += 1) {
    const url = buildUrl(queryDef.params, offset);
    const body = await fetchJson(url);
    const batch = body.data || [];
    if (total == null) total = body.meta?.total ?? batch.length;
    jobs.push(...batch);
    offset += limit;
    if (batch.length < limit || offset >= total) break;
    await new Promise((r) => setTimeout(r, 300));
  }

  return { label: queryDef.label, total, fetched: jobs.length, jobs };
}

async function main() {
  ensureDirSync(fs, OUT_DIR);
  const started = new Date().toISOString();
  const bySlug = new Map();
  const queryStats = [];
  const errors = [];

  for (const q of QUERIES) {
    try {
      console.log(`[freehire] ${q.label}...`);
      const result = await fetchQuery(q);
      queryStats.push({ label: result.label, total: result.total, fetched: result.fetched });
      for (const job of result.jobs) {
        const key = job.public_slug || job.url;
        if (key && !bySlug.has(key)) bySlug.set(key, normalizeJob(job));
      }
    } catch (err) {
      errors.push({ query: q.label, error: String(err.message || err) });
      console.error(`[freehire] ${q.label} failed:`, err.message);
    }
  }

  const jobs = [...bySlug.values()];
  const payload = {
    source: 'freehire',
    started_at: started,
    finished_at: new Date().toISOString(),
    query_stats: queryStats,
    unique_jobs: jobs.length,
    errors,
    jobs,
  };

  const ts = stamp();
  fs.writeFileSync(path.join(OUT_DIR, `freehire-${ts}.json`), JSON.stringify(payload, null, 2));
  fs.writeFileSync(path.join(OUT_DIR, 'freehire-latest.json'), JSON.stringify(payload, null, 2));
  console.log(`[freehire] done: ${jobs.length} unique jobs, ${errors.length} query errors`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
