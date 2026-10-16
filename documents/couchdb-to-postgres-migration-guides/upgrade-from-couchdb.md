# Upgrading from the CouchDB release to the PostgreSQL release

> **Draft.** Written ahead of the **2.0.0** release: the tag `v2.0.0` and the image
> tag `2.0.0` exist only after the release. Open items are listed under
> "TODO before publishing" at the end.

Releases up to and including **1.1.6** store their data in CouchDB. Starting
with **2.0.0** the backend uses PostgreSQL (see
[couchdb-to-postgres-migration.md](../couchdb-to-postgres-migration.md) for the
technical background). The REST API is unchanged, but the **existing data has
to be moved once**. This guide describes how.

- **New installation, nothing to keep:** skip this guide and follow
  [docker/README.md](../../docker/README.md).
- **Existing installation with data you want to keep:** follow the steps
  below.

This guide assumes you run the stack from the setup under `docker/` of a clone
of this repository.

The migration only *reads* your old data. It is copied first and never
modified, so you can always go back (see [Rollback](#rollback)).

## 1 What you need

| Item | Notes |
|---|---|
| Downtime window | The old backend must be stopped while the data is copied. Changes made after the copy are not migrated. For typical data sizes the migration itself takes seconds to minutes. |
| Migration tool | The [cms-db-migration-tool](https://github.com/secvisogram/cms-db-migration-tool), delivered as a container image (`ghcr.io/secvisogram/cms-db-migration-tool:1.0.2`) and started through the compose profile `migrate` in `docker/migrate`. No Java needed. It is also available as a plain jar, see the end of section 3. |
| Docker | The new compose stack runs PostgreSQL; the profile starts a temporary CouchDB on a copy of your old data. |
| Disk space | A temporary second copy of the old CouchDB data, plus the new PostgreSQL volume. |

## 2 Configuration changes

Two sets of settings are involved: the new PostgreSQL settings that the new
release needs, and your old CouchDB settings, which the migration reads one last
time. So: **add the new settings, and keep the old ones until the migration is
verified.** Then remove the old ones.

| | Variables | Where | Used for | After the migration |
|---|---|---|---|---|
| **New** | `CSAF_DB_NAME`, `CSAF_DB_USER`, `CSAF_DB_PASSWORD`, `CSAF_DB_PORT` (host port) | `docker/.env` (the backend container gets them from there; the root `.env` is only needed for running the backend on the host) | creating the new database; the backend and the migration log in with them | keep |
| **Old** | `CSAF_COUCHDB_USER`, `CSAF_COUCHDB_PASSWORD`, `CSAF_COUCHDB_DBNAME`, optionally `CSAF_COUCHDB_DATA_DIR` | `docker/.env` only | opening your copy of the old data | remove |

See [docker/README.md](../../docker/README.md) for which `.env` file holds what.
The old `CSAF_COUCHDB_HOST`, `_PORT` and `_SSL` are not needed any more. The old
`CSAF_COUCHDB_*` lines in the root `.env` can be deleted right away, because the
new backend ignores them.

```shell
# docker/.env — new (keep):
CSAF_DB_NAME=...
CSAF_DB_USER=...
CSAF_DB_PASSWORD=...
# docker/.env — old (keep your existing values until the migration is done):
CSAF_COUCHDB_USER=...
CSAF_COUCHDB_PASSWORD=...
CSAF_COUCHDB_DBNAME=...
# only if the old data is not in docker/data/cms-db:
CSAF_COUCHDB_DATA_DIR=/path/to/old/couchdb/data
```

The PostgreSQL user and password are **new credentials** chosen by you, not the
CouchDB ones. If the old values are missing, the migration falls back to
`admin` / `admin` / `csaf`, which is only right if your old installation used
exactly those.

**Host ports must not clash.** `CSAF_DB_PORT` (PostgreSQL for the CMS) and the
Keycloak database port must be different, and neither may be used by another
database already running on the machine. If a Postgres is already listening on
the port, a client connecting to `localhost:<port>` may reach *that* server
instead of the container and fail with an authentication error. (The migration
profile itself is not affected: it talks to `cms-db` inside the Docker
network.)

**Choose the new database credentials before the first start.** PostgreSQL
creates its user and password only when `cms-db` starts with an empty data
volume. Changing `CSAF_DB_PASSWORD` later does not change the existing
database, and the backend and the migration then fail to log in. If that
happens, either change the password back or recreate the database (remove only
the `cms-db-data` volume, not the Keycloak one).

## 3 Upgrade procedure

Steps 1 and 2 happen in the **old** version, in `docker/`. From step 4 on, all
`docker compose` commands run in the folder `docker/migrate` of the new
version (the old version does not have it). It layers
the migration services (`docker/migrate/compose.migrate.yaml`) on top of the
normal `docker/compose.yaml` and uses the same `docker/.env`, data and volumes,
exactly like `docker/dev`. The migration services belong to a compose profile,
so a plain `docker compose up` does **not** start them (it would otherwise run
the migration, which replaces the contents of the new database). They start only
when you name them, as in the commands below; every other command (`up`, `ps`,
`stop`, ...) behaves as in `docker/`. The folder's `.env` selects the two files
through `COMPOSE_FILE`; if
you run from elsewhere, add `-f ../compose.yaml -f compose.migrate.yaml` to each
command instead.

### 3.1 Back up and stop — with the old version

1. If the old stack is running, make sure your checkout is still the **old**
   version (for example `git checkout v1.1.6`, or the tag or branch you
   started it from). Then stop it with the old files:

   ```shell
   cd docker
   docker compose down
   ```

   Do this *before* switching to the new files. The new compose file no longer
   knows the old CouchDB container, so `down` would leave it running.
2. Copy the CouchDB data folder (in the provided compose setup:
   `docker/data/cms-db`) and your two `.env` files (`docker/.env` and the root
   `.env`) to a safe place. This is your backup. **Do not run `git clean`:** it
   deletes these untracked files.

### 3.2 Get the new version and start it with an empty database

3. Check out the new version. Commit or stash local changes to tracked files
   first (`git status`), then `git fetch --tags` and
   `git checkout v2.0.0`. Your `.env` files
   and `docker/data/cms-db` stay where they are. Adapt the `.env` files as
   described in section 2, comparing them with the new `.env.example` files.
   If you cloned the repository a second time instead, copy the `.env` files
   over and set `CSAF_COUCHDB_DATA_DIR` to the old data folder.
4. Start the new stack and wait until everything is healthy:
   `docker compose up -d --wait`. Check that the new backend image is used
   (`docker compose ps`; the backend log must show Flyway and no CouchDB
   messages).
5. The backend's first start has created the empty schema in `cms-db` through
   Flyway (the backend log shows the Flyway messages). Nothing to do here;
   continue once step 4 has returned.

### 3.3 Dry run

6. Check the old data without touching PostgreSQL:

   ```shell
   docker compose run --rm db-migration-dry-run
   ```

   The profile copies the old data folder (read-only) into a scratch volume,
   starts a temporary CouchDB on the copy, and validates every document. The
   report lists the number of documents per type and every document that
   cannot be migrated, with its `_id` and the reason. Fix the reported
   problems (see [Troubleshooting](#4-troubleshooting)) and repeat until it
   ends with `RESULT: all counts match.`

### 3.4 Migrate

7. Stop the backend so nothing writes while data is loaded:
   `docker compose stop backend-cms`.
8. Run the migration:

   ```shell
   docker compose run --rm db-migration
   ```

   The tool empties the target tables, loads everything in **one transaction**
   (all or nothing) and prints source and inserted counts per type. Any
   mismatch or error gives a non-zero exit code: do not continue, investigate
   first. It can be repeated safely; each run replaces the contents of the new
   database. Extra options can be appended, for example
   `docker compose run --rm db-migration --skip-malformed`.

### 3.5 Verify and go live

9. Compare the counts. Per type, `Source` and `Inserted` must be equal. In
   the database:

   ```shell
   docker compose exec cms-db psql -U <CSAF_DB_USER> -d <CSAF_DB_NAME> -c \
     "SELECT (SELECT count(*) FROM advisories) advisories,
             (SELECT count(*) FROM advisory_versions) versions,
             (SELECT count(*) FROM comments) comments"
   ```

10. Start the backend with `docker compose start backend-cms`, open the CMS in your
    browser (reload the page) and log in. Then check:
    - The advisory list shows the advisories you know, in the same workflow states. The
      number matches the `Advisory` line of the migration report.
    - Open one advisory you know well: its content (and its comments, if it had any)
      look as before.
    - Change something harmless in a test advisory and save. It saves without an error.
11. Clean up the temporary migration containers and the scratch copy:

    ```shell
    docker compose rm -sf old-couchdb old-couchdb-copy
    docker volume rm docker_old-couchdb-data
    ```

    (`docker_` is the compose project name, normally the name of the `docker`
    folder.) **Do not use `docker compose down -v` for this:** it would also
    delete the new database and the Keycloak data. Keep your backup until you
    are confident.

### Alternative: run the tool without the compose profile

The tool is also a plain Java 21 program. Download the jar from the
[release page](https://github.com/secvisogram/cms-db-migration-tool/releases)
(the page shows its SHA-256 digest; compare it with `sha256sum`):

```shell
curl -L -O https://github.com/secvisogram/cms-db-migration-tool/releases/download/v1.0.2/csaf-couchdb-postgres-migrator.jar
```

Start a CouchDB of the same major version on a **copy** of the old data
yourself (CouchDB changes files when it opens them, so never point it at your
only backup), then run:

```shell
java -jar csaf-couchdb-postgres-migrator.jar --dry-run \
  --couchdb-url=http://localhost:5984/<dbname> \
  --couchdb-user=<user> --couchdb-password=<password>

java -jar csaf-couchdb-postgres-migrator.jar \
  --couchdb-url=http://localhost:5984/<dbname> \
  --couchdb-user=<user> --couchdb-password=<password> \
  --pg-url=jdbc:postgresql://localhost:<CSAF_DB_PORT>/<CSAF_DB_NAME> \
  --pg-user=<CSAF_DB_USER> --pg-password=<CSAF_DB_PASSWORD>
```

Here the host port of `cms-db` is used, so the port clash described in
section 2 applies.

## 4 Troubleshooting

| Symptom | Cause and fix |
|---|---|
| Report lists `duplicate tracking id '…'` and aborts | Two advisories share a tracking ID, which PostgreSQL does not allow. Change the tracking ID of one of them in the old system (or delete the duplicate), copy the data again and rerun. Duplicates are never skipped automatically because the tool cannot know which one is right. |
| Report lists malformed documents (missing owner, invalid UUID, comment pointing at a missing advisory, …) | Fix the data in CouchDB, or run with `--skip-malformed` to load all valid documents and list the others. The exit code stays non-zero. Documents that depend on a skipped document (versions, comments, audit entries) are skipped as well. |
| The dry run finds no documents or fails to connect | Check `CSAF_COUCHDB_DBNAME`, the credentials, and that the old data folder is at `docker/data/cms-db` (or `CSAF_COUCHDB_DATA_DIR`) and contains the CouchDB files. |
| `password authentication failed` (jar alternative) | Another PostgreSQL is listening on the same host port. Change `CSAF_DB_PORT` or stop the other server. |
| Backend fails at startup with a Flyway checksum or "non-empty schema" error | The database was not created by this backend's Flyway migrations. Use a fresh, empty database (remove the `cms-db-data` volume of the new stack only, **not** the Keycloak volume) and start again. |
| Backend log shows connection attempts to CouchDB (`:5984`) | The old backend image is still in use. Check the image tag and rebuild or pull. |
| The migration tool runs out of memory (very large documents) | The tool reads CouchDB in pages of 500 documents. Use a smaller page size on either step, for example `docker compose run --rm db-migration-dry-run --page-size=100` and `docker compose run --rm db-migration --page-size=100`. |

## 5 What changes for users

- **Revisions.** The old CouchDB revision strings (`3-ab12…`) are replaced by
  plain counters (`0`, `1`, …), starting at `0` after the migration. Browser
  sessions that were open during the upgrade must be reloaded, otherwise
  saving fails with a wrong-revision error.
- **List order.** Advisories are returned ordered by their ID (the same as
  before).
- **IDs, versions, audit trails and comments** are kept.
- **Comments.** Comment migration (including answers) is covered by automated
  tests, but has so far not been verified on a real data set.

## Rollback

Until you delete it, your original data is untouched. To go back, stop the new
stack, start the old release with the original CouchDB data folder and your
old `.env` files. Changes made in the new system after the migration are not
transferred back.

## TODO before publishing

- In the release PR (`chore/release`, where the pom version is set to `2.0.0`), also
  replace the interim pin in `docker/compose.yaml` (currently the branch tag) with
  `2.0.0`, so the tagged commit points at the image its own release publishes. After
  the release, check that `ghcr.io/secvisogram/csaf-cms-backend:2.0.0` exists. The
  migration tool itself is released (v1.0.2: container image and jar; the default of
  `CSAF_MIGRATOR_IMAGE` in `docker/migrate/compose.migrate.yaml` matches it).
  Its container package must be **public**: check that
  `docker pull ghcr.io/secvisogram/cms-db-migration-tool:1.0.2` works without
  logging in.
- Dry-run the tool against databases created by older releases (v1.0.0 to
  v1.1.6) to confirm that no document shape from an older release is rejected.
- Add the new variable names to `.env.example` hints and link this guide from
  `README.md` and `docker/README.md`.
