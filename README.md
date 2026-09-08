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

## M1 slice (current)

Profile create/update · Home feed · Actions decisions · Jobs list/detail/manual-url/status · Roadmap 404 stub · Sources stubs.  
Not yet: JobSpy/FreeHire sync, Gate/Rank, Roadmap generate, Outbox, backups scheduler.
