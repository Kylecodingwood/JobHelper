# Job Helper

Local-first Ireland job-prep app (M1). Specs live under `delta/`; runnable code under `jobAssitant/`.

## Quick start

```bash
# API (PostgreSQL localhost:5432/jobhelper)
cd jobAssitant/backend && ./mvnw spring-boot:run

# UI (proxies /api → :8080)
cd jobAssitant/frontend && npm install && npm run dev
```

Or run the whole stack with Docker:

```bash
docker compose up --build
```

- Frontend: http://localhost:5173  
- API: http://localhost:8080/api/v1  
- Specs: [`delta/README.md`](delta/README.md) · Progress: [`delta/SRS/progress-log.md`](delta/SRS/progress-log.md)

## Layout

| Path | Role |
|---|---|
| `delta/` | SRS → 1.req → 2.design → 3.coding |
| `jobAssitant/backend/` | Spring Boot API (M1 slice) |
| `jobAssitant/frontend/` | Vite React UI (prototype-aligned) |
| `add.md` | Forward-looking notes (e.g. Company-forward) |

## M1 status (2026-09-08)

**Implemented:** Profile · Home (today-priority-v1) · Jobs (Gate/Rank/status) · Sources (FreeHire + JobSpy) · Roadmap (multi-folder Todo/Company/Document) · LeetCode · CV files · Behavioral local domain · scheduled sync/backup · Docker Compose.

**Remaining:** tests/CI · full optimistic locking · Outbox hardening · remove legacy Roadmap generate code.

Details: [`delta/SRS/progress-log.md`](delta/SRS/progress-log.md) · API: [`delta/3.coding/api-contract.md`](delta/3.coding/api-contract.md)
