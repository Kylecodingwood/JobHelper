#!/usr/bin/env python3
"""JobSpy all-sites Ireland capture — user accepts ToS/rate-limit risk."""

from __future__ import annotations

import json
import os
import sys
import traceback
from datetime import datetime, timezone
from pathlib import Path

import pandas as pd
from jobspy import scrape_jobs

OUT_DIR = Path(__file__).resolve().parent / "out"

# All sites supported by python-jobspy (Jul 2026)
_DEFAULT_SITES = [
    "linkedin",
    "indeed",
    "zip_recruiter",
    "glassdoor",
    "google",
    "bayt",
    "naukri",
    "bdjobs",
]

_DEFAULT_SEARCH_TERMS = [
    "software engineer",
    "graduate software developer",
    "junior backend developer",
]

LOCATION = "Ireland"
HOURS_OLD = 720  # 30 days


def _env_list(name: str, default: list[str]) -> list[str]:
    raw = os.environ.get(name, "").strip()
    if not raw:
        return list(default)
    return [p.strip() for p in raw.split(",") if p.strip()]


def _env_int(name: str, default: int) -> int:
    raw = os.environ.get(name, "").strip()
    if not raw:
        return default
    try:
        return int(raw)
    except ValueError:
        return default


# Job Helper backend may inject scoped env at spawn time
ALL_SITES = _env_list("JOBHELPER_SITES", _DEFAULT_SITES)
SEARCH_TERMS = _env_list("JOBHELPER_SEARCH_TERMS", _DEFAULT_SEARCH_TERMS)
RESULTS_WANTED = _env_int("JOBHELPER_RESULTS_WANTED", 100)


def row_to_job(row) -> dict:
    def g(col):
        v = row.get(col)
        if pd.isna(v):
            return None
        return v.item() if hasattr(v, "item") else v

    return {
        "source": "jobspy",
        "site": g("site"),
        "id": g("id"),
        "title": g("title"),
        "company": g("company"),
        "location": g("location"),
        "url": g("job_url") or g("link"),
        "date_posted": str(g("date_posted")) if g("date_posted") is not None else None,
        "job_type": g("job_type"),
        "is_remote": g("is_remote"),
        "salary_source": g("salary_source"),
        "interval": g("interval"),
        "min_amount": g("min_amount"),
        "max_amount": g("max_amount"),
        "currency": g("currency"),
        "description_snippet": (g("description") or "")[:500] or None,
    }


def dedupe_key(job: dict) -> str:
    return job.get("url") or f"{job.get('site')}:{job.get('title')}:{job.get('company')}"


def run_site_search(site: str, search_term: str) -> tuple[list[dict], dict | None]:
    kwargs = {
        "site_name": [site],
        "search_term": search_term,
        "location": LOCATION,
        "results_wanted": RESULTS_WANTED,
        "hours_old": HOURS_OLD,
        "country_indeed": "Ireland",
        "linkedin_fetch_description": False,
        "verbose": 1,
    }
    if site == "google":
        kwargs["google_search_term"] = f"{search_term} jobs near Dublin Ireland since yesterday"

    df = scrape_jobs(**kwargs)
    jobs = [row_to_job(row) for _, row in df.iterrows()]
    return jobs, None


def main() -> int:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    started = datetime.now(timezone.utc).isoformat()
    by_key: dict[str, dict] = {}
    run_stats: list[dict] = []
    errors: list[dict] = []

    for site in ALL_SITES:
        for term in SEARCH_TERMS:
            label = f"{site}:{term}"
            print(f"[jobspy] {label}...", flush=True)
            try:
                jobs, _ = run_site_search(site, term)
                added = 0
                for job in jobs:
                    key = dedupe_key(job)
                    if key not in by_key:
                        by_key[key] = job
                        added += 1
                run_stats.append(
                    {
                        "label": label,
                        "site": site,
                        "search_term": term,
                        "fetched": len(jobs),
                        "new_unique": added,
                    }
                )
                print(f"[jobspy] {label} -> {len(jobs)} rows, +{added} unique", flush=True)
            except Exception as exc:
                err = {"label": label, "error": str(exc), "trace": traceback.format_exc()}
                errors.append(err)
                print(f"[jobspy] {label} FAILED: {exc}", file=sys.stderr, flush=True)

    jobs = list(by_key.values())
    payload = {
        "source": "jobspy",
        "started_at": started,
        "finished_at": datetime.now(timezone.utc).isoformat(),
        "sites": ALL_SITES,
        "search_terms": SEARCH_TERMS,
        "location": LOCATION,
        "results_wanted_per_run": RESULTS_WANTED,
        "run_stats": run_stats,
        "unique_jobs": len(jobs),
        "errors": errors,
        "jobs": jobs,
    }

    ts = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H-%M-%S-%fZ")
    out_path = OUT_DIR / f"jobspy-{ts}.json"
    latest_path = OUT_DIR / "jobspy-latest.json"
    text = json.dumps(payload, indent=2, default=str)
    out_path.write_text(text, encoding="utf-8")
    latest_path.write_text(text, encoding="utf-8")
    print(f"[jobspy] done: {len(jobs)} unique jobs, {len(errors)} run errors", flush=True)
    return 0 if not errors or jobs else 1


if __name__ == "__main__":
    raise SystemExit(main())
