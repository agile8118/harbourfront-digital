# Harbourfront Digital

A small marketing site for "Harbourfront Digital" with a Java backend behind it. The frontend is plain HTML/CSS/JS and gets served directly by the backend, which also handles a contact form and a newsletter signup flow.

## Project structure

```
public/          static frontend (served directly by the Java backend)
  index.html
  confirmed.html
  main.js
  styles.css

server/          Java backend (Vert.x)
  src/main/java/com/harbourfront/
    Main.java              entry point
    MainVerticle.java      deploys the verticle, wires up DB + AWS services
    AppRouter.java         sets up all the routes
    Log.java               file based logging to logs/YYYY-MM-DD/
    database/
      Database.java        creates the Postgres connection pool
      DB.java               query helpers (find, insert, update, delete)
      Seed.java             drops and recreates all tables, used by ./do.sh seed
    handlers/
      ContactHandler.java      POST /api/contact
      NewsletterHandler.java   POST /api/newsletter
      ConfirmHandler.java      GET /api/newsletter/confirm
    middleware/
      RequestLoggerHandler.java   logs every request
    services/
      SnsService.java   sends contact form notifications via AWS SNS
      SesService.java   sends newsletter confirmation emails via AWS SES
  src/main/resources/database/
    tables/          one .sql file per table, plus manifest.txt for creation order
    triggers.sql

stressor/        autocannon load test scripts, not part of the build
  contacts.mjs    hammers POST /api/contact
  page-load.mjs   load tests static page loads

do.sh            build / run / seed the server
env.sh           loads env vars (from .env locally, AWS SSM in prod) then runs a command
deploy.sh        builds the jar and deploys it to the remote host
ecosystem.config.js   pm2 config used in prod
```

There's no ORM here, just raw SQL through `DB.java`. Tables live in `server/src/main/resources/database/tables/*.sql` and `manifest.txt` lists them in creation order (they get dropped in reverse order to keep foreign keys happy).

## Requirements

- Java 17
- Maven
- PostgreSQL
- Node (only if you want to run the stressor scripts)

## Setup

Copy the example env file and fill in your own values:

```
cp .env.example .env
```

You'll need a Postgres database, plus AWS credentials/config if you want SNS notifications or SES confirmation emails to actually go out. If you leave `SNS_TOPIC_ARN` or `SES_SENDER_EMAIL` unset, those bits fail gracefully and just get logged instead of crashing the app.

## Running the project

All commands run from the repo root using `./do.sh`.

```
./do.sh build   # compiles the server into server/target/harbourfront-server-1.0.jar
./do.sh run     # builds (if .env exists) and starts the server
./do.sh seed    # builds (if needed) and seeds the database
```

`./do.sh run` behaves differently depending on your setup. If you have a `.env` file (local dev) it just runs the jar directly with `./env.sh java -jar ...`. If there's no `.env` (prod) it assumes pm2 is set up and runs `pm2 startOrReload ecosystem.config.js --update-env`.

Important: the jar has to be run from the repo root, not from `server/`. It resolves `public/` and `logs/` as relative paths, so running it from the wrong directory will break static file serving and logging.

### Seeding the database

`./do.sh seed` runs the jar with a `seed` argument, which:

1. Creates the database if it doesn't exist yet
2. Drops all existing tables (in reverse manifest order)
3. Recreates them from the .sql files in `server/src/main/resources/database/tables/`
4. Runs `triggers.sql`

This is destructive, it wipes whatever data is already there, so don't run it against a database you care about.

### Environment variables

`env.sh` is what actually loads your environment before running a command. It picks one of three sources:

- If `DOCKER=true`, it assumes Docker Compose already injected the vars and does nothing
- If a `.env` file exists, it loads that (local dev)
- Otherwise it pulls everything from AWS SSM under `/harbourfront/prod/` (production)

See `.env.example` for the full list of variables you can set (port, DB connection info, AWS region, SNS topic ARN, SES sender email, app base URL, etc).

## Deploying

`./deploy.sh` builds the jar locally, pulls the latest code on the remote host (aliased as `sky` in SSH config), uploads the freshly built jar, and restarts the app with pm2. The remote host just needs Java and pm2 installed, no build tools required there.

## Load testing

The `stressor/` directory has some autocannon based scripts for hammering the server:

```
node stressor/contacts.mjs --url <base-url> [--connections N] [--duration S] [--rate R]
node stressor/page-load.mjs
```

Before you point `contacts.mjs` at any deployed instance, set `SKIP_SNS=true` (and `SKIP_SES=true` too if you ever add newsletter endpoints to the load test) in that instance's environment. Without it, every fake request triggers a real SNS notification straight to the owner's inbox and writes junk rows into `contact_submissions`. Don't forget to unset those flags again afterward so real traffic doesn't get silently skipped.

## API endpoints

- `POST /api/contact` - validates name/email/message, saves it, fires off an SNS notification (async, failures are just logged)
- `POST /api/newsletter` - adds an email to `subscribers`, sends a confirmation email via SES. If the email's already subscribed it still returns success, just doesn't leak that info
- `GET /api/newsletter/confirm?token=...` - marks a subscriber as confirmed and serves `confirmed.html`

Everything else falls through to the static file handler serving `public/`.
