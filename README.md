# Job Helper

A personal job-search management app built for the Irish graduate market. Tracks jobs, manages application roadmaps, practises behavioural interview questions, and keeps LeetCode progress — all in one place.

**Tech stack:** Java 21 · Spring Boot · PostgreSQL · Flyway · React · TypeScript · Vite · Docker

---

## Screenshots

| | |
|---|---|
| **Today's priorities** — smart action feed ranked by urgency | **Job board** — scraped listings filtered and ranked by fit |
| ![Home](docs/images/home.png) | ![Jobs](docs/images/jobs.png) |
| **Application roadmap** — per-company todo lists with due dates | **LeetCode tracker** — Hot 100 progress with spaced review |
| ![Roadmap](docs/images/roadmap.png) | ![LeetCode](docs/images/leetcode.png) |
| **Applications dashboard** — email-parsed job application stats & tracking | **Behavioural prep** — STAR answer bank with local feedback |
| ![Applications](docs/images/applications.png) | ![Behavioral](docs/images/behavioral.png) |
| **CV library** — version-controlled CV file storage | |
| ![CV](docs/images/cv.png) | |

---

## Features

- **Home feed** — daily priority list combining job actions, roadmap todos, and LeetCode reminders, ranked by urgency band
- **Job discovery** — integrates with FreeHire and JobSpy to scrape Ireland listings; deduplication and gate/rank scoring built in
- **Application roadmap** — per-company task lists with folder organisation, dependency tracking, and due-date ordering
- **LeetCode Hot 100** — fetches problem content, tracks solve status, surfaces problems due for review
- **Behavioural question bank** — curated questions with STAR evidence tagging and local rule-based feedback
- **CV management** — upload and store multiple CV versions (PDF/DOCX)
- **Applications dashboard** — bulk-import email confirmations, visual stats by category/status, inline status updates, search and filter
- **Scheduled sync** — automatic job scraping via cron, weekly backup to zip

---

## Quick start

Requires Docker (no local Java or Node needed).

```bash
docker compose up --build
```

- Frontend: http://localhost:5173
- API: http://localhost:8080/api/v1

To stop and remove all data:

```bash
docker compose down -v
```

---

## Project layout

```
jobAssitant/
  backend/    Spring Boot API — Jobs, Roadmap, LeetCode, Behavioral, CV, Profile
  frontend/   React SPA — Vite + TypeScript
delta/        Specs: requirements → design → coding plan
docs/         Architecture notes and screenshots
docker-compose.yaml
```

---

## Architecture notes

- **Outbox pattern** for reliable event publishing between modules
- **Flyway** database migrations (21 versions)
- **Domain / application / infrastructure layering** per module
- **Gate + Rank scoring** for job fit evaluation with duplicate URL normalisation
