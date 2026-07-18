# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

A small marketing site + backend for "Harbourfront Digital". Static frontend in `public/` (vanilla HTML/CSS/JS) is served directly by the Java backend in `server/`, which also exposes a few `/api/*` endpoints (contact form, newsletter signup/confirmation).

## Common commands

All commands are run from the repo root.

- `./do.sh build` — compiles the server (`mvn package -q -DskipTests` in `server/`), producing `server/target/harbourfront-server-1.0.jar`.
- `./do.sh run` — builds (if `.env` exists) and runs the server. Locally (with `.env` present) it runs `./env.sh java -jar server/target/harbourfront-server-1.0.jar` directly; without `.env` it uses `pm2 startOrReload ecosystem.config.js`.
- `./do.sh seed` — builds (if needed) and runs `java -jar server/target/harbourfront-server-1.0.jar seed`, which drops and recreates all DB tables (see `Seed.java`) and creates the database if missing.
- `./env.sh <cmd>` — loads env vars before running `<cmd>`. Uses `.env` if present (dev), AWS SSM under `/harbourfront/prod/` if `.env` is absent and not in Docker (prod), or assumes Docker Compose already injected vars.
- `./deploy.sh` — builds the jar locally, `git pull`s on the remote `sky` host, uploads the jar, and restarts via `pm2 startOrReload ecosystem.config.js --update-env`. The jar must be run from the project root so the `public/` static directory and `logs/` dir resolve correctly.
- The jar **must be launched from the repo root** — `AppRouter` serves `public/` and `Log` writes to `logs/YYYY-MM-DD/` as relative paths.

### Load testing

`stressor/` contains autocannon-based load scripts (not part of the build):
- `node stressor/contacts.mjs --url <base-url> [--connections N] [--duration S] [--rate R]` — hammers `POST /api/contact` with generated names/emails/messages.
- `node stressor/page-load.mjs` — load-tests static page loads.

**Before stress-testing any deployed instance**, set `SKIP_SNS=true` (and `SKIP_SES=true` if newsletter endpoints are ever included) in that instance's env config. `contacts.mjs` triggers a real SNS publish per request via `SnsService`, which forwards to the owner's real subscribed email — without the skip flag this floods their inbox and writes fake rows into `contact_submissions`. Unset the flags again afterward so real production traffic isn't silently skipped.

## Architecture

**Stack**: Java 17, Vert.x 4.5 (core + web + reactive Postgres client), AWS SDK v2 (SNS + SES v2), Maven shade plugin for a fat jar. No ORM — raw SQL via `DB.java`.

### Request flow

`Main` → deploys `MainVerticle` → builds a `DB` (wrapping a `PgPool` from `Database.createPool`), `SnsService`, `SesService` → passes them to `AppRouter.create()`, which wires up the Vert.x `Router`:

1. `RequestLoggerHandler` logs every request (IP, method, path, status, response time) to `logs/YYYY-MM-DD/info.log` via `Log`.
2. `BodyHandler` parses JSON bodies for `/api/*`.
3. API routes: `POST /api/contact` (`ContactHandler`), `POST /api/newsletter` (`NewsletterHandler`), `GET /api/newsletter/confirm` (`ConfirmHandler`).
4. Everything else falls through to `StaticHandler.create("public")` serving the frontend.
5. 404/500 error handlers; 500s log the failure via `Log.error`.

### DB access (`database/DB.java`)

A thin helper over Vert.x's reactive `Pool`, all methods return `Future<...>`:
- `find` / `findMany` — run a parameterized query, map rows to `JsonObject`/`JsonArray`.
- `insert(table, JsonObject data)` — builds `INSERT ... VALUES (...) RETURNING *` from the JSON's fields.
- `update(table, JsonObject data, whereClause, whereParams)` — builds `UPDATE ... SET ...`; **note the where-clause placeholders you write (`$1`, `$2`, ...) are auto-shifted past the data columns** by `shiftPlaceholders`.
- `delete(table, whereClause, params)` — `DELETE ... RETURNING *`.
- `query` — arbitrary SQL.

Temporal types (`OffsetDateTime`/`LocalDateTime`/`LocalDate`) are converted to ISO strings when rows become JSON.

### Database schema/seeding

- Table DDL lives in `server/src/main/resources/database/tables/*.sql`, one file per table, listed in `manifest.txt` (creation order; drops happen in reverse order for FK safety).
- `Seed.run()` (invoked via `java -jar ... seed`) connects via plain JDBC, creates the database if missing, drops/recreates all tables per the manifest, then runs `triggers.sql`.
- Current tables: `subscribers` (email, UUID `token`, `confirmed` flag) and `contact_submissions` (name, email, message, ip_address).

### Handlers

- `ContactHandler` — validates `name`/`email`/`message`, inserts into `contact_submissions` with the requester IP (`x-forwarded-for` if present), then fires an async `SnsService.sendContactNotification` (failure is logged, not surfaced to the client).
- `NewsletterHandler` — inserts into `subscribers`; on unique-constraint violation it silently returns success (avoids leaking whether an email is already subscribed); on success sends a confirmation email via `SesService.sendConfirmation`.
- `ConfirmHandler` — looks up `subscribers` by UUID `token`, marks `confirmed = true`, and serves `public/confirmed.html`.

### Services (AWS)

- `SnsService` — publishes contact-form notifications to `SNS_TOPIC_ARN` using IAM role credentials; if the client fails to init or the ARN is unset, it logs and fails gracefully without crashing the app.
- `SesService` — sends the newsletter confirmation email (inline HTML template) from `SES_SENDER_EMAIL`, linking to `${APP_BASE_URL}/api/newsletter/confirm?token=...`. Uses `InstanceProfileCredentialsProvider` (EC2 IAM role).

### Logging (`Log.java`)

File-based, not java.util.logging output: `logs/YYYY-MM-DD/info.log` (info messages) and `logs/YYYY-MM-DD/errors.log` (dashed blocks with exception name/message/stack trace). Directories are created on demand.

### Environment variables

See `.env.example` for the full list (`PORT`, `DB_*`, `AWS_REGION`, `SNS_TOPIC_ARN`, `SES_SENDER_EMAIL`, `APP_BASE_URL`). Locally these come from `.env`; in production `env.sh` pulls them from AWS SSM under `/harbourfront/prod/`.