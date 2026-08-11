'use strict';

const DEFAULT_TIMEOUT_MS = 120_000;

async function fetchJson(url, options = {}) {
  const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS;
  const retries = options.retries ?? 2;
  let lastErr;

  for (let attempt = 0; attempt <= retries; attempt += 1) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), timeoutMs);
    try {
      const res = await fetch(url, {
        ...options,
        signal: controller.signal,
        headers: {
          Accept: 'application/json',
          'User-Agent': 'job-helper-datasource-test/1.0',
          ...(options.headers || {}),
        },
      });
      const text = await res.text();
      let body;
      try {
        body = text ? JSON.parse(text) : null;
      } catch {
        throw new Error(`Non-JSON response (${res.status}) from ${url}: ${text.slice(0, 200)}`);
      }
      if (!res.ok) {
        throw new Error(`HTTP ${res.status} from ${url}: ${JSON.stringify(body).slice(0, 300)}`);
      }
      return body;
    } catch (err) {
      lastErr = err;
      const cause = err.cause?.code || err.cause?.message || err.message;
      if (attempt < retries) {
        await new Promise((r) => setTimeout(r, 1500 * (attempt + 1)));
        continue;
      }
      throw new Error(`${cause} (${url})`);
    } finally {
      clearTimeout(timeout);
    }
  }
  throw lastErr;
}

function stamp() {
  return new Date().toISOString().replace(/[:.]/g, '-');
}

function ensureDirSync(fs, dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
}

module.exports = { fetchJson, stamp, ensureDirSync, DEFAULT_TIMEOUT_MS };
