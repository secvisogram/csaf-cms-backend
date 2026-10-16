# Upgrading from the CouchDB release — short version

> **Draft.** Short form of [upgrade-from-couchdb.md](upgrade-from-couchdb.md).
> Details, troubleshooting and background are there. Written ahead of the **2.0.0**
> release: the tag `v2.0.0` exists only after the release.

Releases up to **1.1.6** store data in CouchDB, newer ones in PostgreSQL. The API
is unchanged, but existing data must be moved **once**. Nothing to keep? Just
install fresh ([docker/README.md](../../docker/README.md)).

This guide assumes you run the stack from the setup under `docker/` of a clone of this
repository.

Your old data is only read, never changed. Plan some downtime: changes made after
step 1 are not migrated.

## Steps

1. **Stop and back up — with the old version.** If the old stack is running, make sure
   your checkout is still the old version (for example `git checkout v1.1.6`, or the
   tag or branch you started it from). Then stop the stack and copy its data:

   ```shell
   cd docker
   docker compose down
   ```

   Copy the CouchDB data folder `docker/data/cms-db` and your two `.env` files
   (`docker/.env` and the root `.env`) somewhere safe. **Do not run `git clean`:** it
   deletes these untracked files.

2. **Check out the new version.** Commit or stash local changes to tracked files first
   (`git status`), then:

   ```shell
   git fetch --tags
   git checkout v2.0.0
   ```

   Your `.env` files and `docker/data/cms-db` stay where they are.

3. **Configure.** Compare both `.env` files with the new `.env.example` files. Add the
   new PostgreSQL settings and **keep** the old CouchDB ones until the end:

   | File | Add / keep |
   |---|---|
   | `docker/.env` | new `CSAF_DB_NAME`, `CSAF_DB_USER`, `CSAF_DB_PASSWORD` (**new** credentials, chosen before the first start; `CSAF_DB_PORT` must not clash with another database on the host) |
   | `docker/.env` | old `CSAF_COUCHDB_USER`, `CSAF_COUCHDB_PASSWORD`, `CSAF_COUCHDB_DBNAME` (defaults: `admin` / `admin` / `csaf`) |

   The root `.env` needs nothing new for the containers: the backend gets its database
   settings from `docker/.env`. Remove the old `CSAF_COUCHDB_*` lines from it.

4. **Switch to the migration folder.** Every `docker compose` command from here on
   runs in it:

   ```shell
   cd docker/migrate
   ```

5. **Start the new stack** (new release, new image tags). `--wait` returns once
   everything is healthy, including the backend's first start, which creates the empty
   schema. Then stop the backend so nothing writes during the migration:

   ```shell
   docker compose up -d --wait
   docker compose stop backend-cms
   ```

6. **Dry run** — validates the old data, writes nothing:

   ```shell
   docker compose run --rm db-migration-dry-run
   ```

7. **Migrate:**

   ```shell
   docker compose run --rm db-migration
   ```

   Both runs must end with `RESULT: all counts match.` Otherwise stop and see the
   [full guide](upgrade-from-couchdb.md#4-troubleshooting) (typical causes: duplicate
   tracking IDs, malformed documents).

8. **Go live:** start the backend with `docker compose start backend-cms`, open the CMS
   in your browser (reload the page) and log in. Then check:
   - The advisory list shows the advisories you know, in the same workflow states. The
     number matches the `Advisory` line of the migration report.
   - Open one advisory you know well: its content (and its comments, if it had any)
     look as before.
   - Change something harmless in a test advisory and save. It saves without an error.

9. **Clean up:**

   ```shell
   docker compose rm -sf old-couchdb old-couchdb-copy
   docker volume rm docker_old-couchdb-data
   ```

   Then delete the old `CSAF_COUCHDB_*` lines. **Never use `docker compose down -v`**
   for this: it also deletes the new database and the Keycloak data. Keep the backup
   until you are confident.

## Good to know

- **Browsers:** reload open sessions. Revisions are now counters (`0`, `1`, …), so
  edits from before the upgrade fail with a wrong-revision error.
- **Rollback:** start the old release with the original data folder and your old
  `.env` files. Changes made in the new system are not transferred back.
- **Repeatable:** step 7 replaces the contents of the new database each time, so you
  can run it again after fixing the data.
