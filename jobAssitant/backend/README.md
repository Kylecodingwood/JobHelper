# Job Helper — Backend (M1)

Spring Boot API aligned with `delta/2.design/server` and ADR PostgreSQL.

## Prerequisites

- PostgreSQL running locally
- Database/user: `jobhelper` / `jobhelper` on `localhost:5432`

```bash
# one-time (Homebrew example)
brew services start postgresql@18
createuser -s jobhelper 2>/dev/null || true
psql -d postgres -c "ALTER ROLE jobhelper WITH LOGIN PASSWORD 'jobhelper';"
createdb -O jobhelper jobhelper
```

## Run

```bash
cd jobAssitant/backend
./mvnw spring-boot:run
```

API: http://localhost:8080/api/v1

## Implemented (slice 1)

- `GET/PUT /api/v1/profile` + field-usage + backups stub
- `GET /api/v1/home`, actions list/decisions/navigation/priority-evidence
- `GET/PATCH /api/v1/jobs*`, `POST /jobs/manual-url`, sources stubs
- `GET /api/v1/roadmap` → 404 until generate (stub)

Optimistic lock: `expectedProfileVersion` / `expectedVersion` → 409.
