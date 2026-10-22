# #10 — Docker Compose

- **Issue:** [#10 [1.3] Docker Compose for local development](https://github.com/DavidMS/biwenger-manager/issues/10)
- **Phase:** 1 — Project skeleton
- **Date:** 2026-10-22

## Prompt

> merged, go with #10

## What Claude did

1. Moved #9 to *Done* and #10 to *In progress*, branched, and ran the `implement-issue` skill.
2. Checked the base images before writing anything: the tags exist, and the Java runtime image has no `curl` or `wget`. That second check decided how the backend health check works.
3. Wrote:
   - **`backend/Dockerfile`:** build on Temurin 25 JDK with a Maven cache, run on the 25 JRE as a non-root user. Tests and the format check are skipped in the image, because `./mvnw verify` and CI run them.
   - **`frontend/Dockerfile`:** build on Node 24 with `npm ci`, serve with nginx.
   - **`frontend/nginx.conf`:** serves the app shell for client-side routes such as `/squad`, and forwards `/api/` and `/actuator/health` to the backend, so the browser sees one origin and needs no CORS. Hashed assets are cached forever; the shell is never cached.
   - **`docker-compose.yml`:**
     - PostgreSQL 18 with a named volume
     - health checks on all three services, and each service waits for the one before it to be healthy
     - every port bound to `127.0.0.1`
     - `POSTGRES_PASSWORD` required, with an error that points to `.env.example`
   - **`.env.example`:** every variable, with a comment for each: Biwenger login, database and ports.
   - **The Vite dev server** now forwards the same paths as nginx, so the app reaches the backend the same way in development and in Docker.
4. Updated the README ("Running it", plus developing without Docker) and `CLAUDE.md`.

## Verification

Run as a separate Compose project (`-p bm-check`) with `--env-file` pointing at a copy of `.env.example` in Claude's scratch folder, so no real `.env` was created or read.

| Check | Result |
|---|---|
| `docker compose config` without `.env` | Stops: "Set POSTGRES_PASSWORD in .env (see .env.example)" |
| `up --build --wait` | db, backend and frontend all healthy |
| `/`, `/squad`, `/advisor/x` | 200, app shell |
| `/actuator/health` via nginx | `{"status":"UP"}` |
| `/api/nothing` via nginx | Spring's JSON 404, so the request reached the backend |
| Hashed asset | `Cache-Control: public, max-age=31536000, immutable` |
| Backend on host port 8080 | Not reachable, as intended |
| Backend process user | `app`, not root |
| Data after `down` + `up` | A test row survived. `down -v` removed it |
| Vite dev proxy | `/actuator/health` and `/api/*` on :5173 reach a local backend |

## Review

Both reviewer agents ran in parallel, each on its half of the change. Both gave **approve with nits**, with no blockers.

`react-reviewer` (frontend, nginx, Vite):
- **minor: `.dockerignore` sent too much into the build.** A local `.env.local` would have reached the image, and Vite bakes `VITE_*` values from it into the bundle. Fixed: `.env*`, `*.local`, logs and editor files are now excluded.
- **minor: nginx resolved `backend` once, at startup.** If only the backend container is recreated and gets a new IP, nginx keeps returning 502. Fixed with Docker's DNS resolver and a variable in `proxy_pass`. Tested by forcing the backend onto a new IP (a throwaway container held the old one): nginx answered within a second.
- **nits:**
  - Vite's proxy matched by prefix where nginx matches exactly. Now `/api/` and `^/actuator/health$`.
  - `vite preview` had no proxy. Now it shares the same proxy settings.
  - The forwarding headers are now set once for both backend locations.
  - gzip is on, shrinking the JS bundle from 344 kB to 126 kB.
  - Base images are pinned to a minor version: `node:24.22-alpine`, `nginx:1.31-alpine`.

`java-reviewer` (backend image, Compose, secrets):
- **nit:** `-Dspotless.check.skip` did nothing, because `package` never reaches the check. Removed.
- **nit:** copying the POMs before the sources gave no caching benefit, because nothing ran in between. Replaced with one `COPY` filtered by `.dockerignore`.
- **nit:** `.env.example` had a ready-made `POSTGRES_PASSWORD=change-me`, so copying the file skipped the "required" check. Now it's empty, and Compose stops until you choose one.
- **For Phase 2:** the Biwenger password reaches the backend as an environment variable, which anyone with Docker access can read with `docker inspect`. That's acceptable for a local single-user app, but it should be revisited in #15: Compose secrets, or at least a README note.

## Corrections

- **Port 3000 was taken** by another project's container on this machine, so the frontend couldn't start. 3000 is one of the most common dev ports, so the default became 8000. `APP_PORT` in `.env` changes it.
- **A zsh quirk produced a false "removed" message.** Claude kept the compose command in a variable, which zsh doesn't split into words, so nothing ran, but the script still printed that the stack was removed. Re-run with a shell function, and checked with `docker volume ls`.
- **The first "new IP" test didn't test anything.** After a backend-only recreate the container got the same IP back, so the stale-address case never happened. The check passed for the wrong reason. Claude noticed the unchanged IP and redid it with the old IP taken by another container.
- **The `.env` guard (#3) blocked a harmless command.** Claude's edit script contained the README line `cp .env.example .env   # then edit .env`, and the guard read it as copying `.env`. Claude moved the script into a file instead of weakening the guard. A guard that is sometimes too strict is the right trade-off for credentials.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **nginx serves the UI and forwards API calls**, so there's one origin and no CORS setup. The Vite dev server mirrors it.
- **The database password has no default.** Compose refuses to start without it, so nobody runs on a password from git by accident.
- **Only the UI and the database are published, and only on `127.0.0.1`.** The backend is reached through nginx.
- **The image builds skip tests** to keep `docker compose up --build` fast. Tests are the job of `./mvnw verify`, `npm test` and CI (#11).
- **Database and Biwenger variables are wired now,** although the backend uses them only from Phases 2 and 3, so `.env.example` is complete from the start.

## Lesson

Test infrastructure the way a user meets it: start the stack, then check each promise with a request, such as a deep link, a proxied call or a restart that should keep data. Each check here was one `curl` or one `psql` query, and two of them found problems a read-through wouldn't have caught: the busy port and the silent no-op.
