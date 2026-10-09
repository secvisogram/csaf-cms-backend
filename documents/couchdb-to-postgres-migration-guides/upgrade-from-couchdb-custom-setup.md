# Upgrading from the CouchDB release — custom setup

> **Draft.** Written ahead of the **2.0.0** release: the backend image tag `2.0.0`
> exists only after the release. **TODO:** the migration image
> `ghcr.io/secvisogram/cms-db-migration-tool` must be readable without a login (the
> package is currently private); until then use the jar, or build the image (see 2a).

Use this guide if you do **not** run the stack from the `docker/` folder of this
repository, for example with your own compose file, a dedicated compose file that
is independent of git, or a different way of running containers. It explains how
to run the migration tool yourself, either as a **container** or as a **Java jar**.
If you do run the `docker/` setup of this repository, follow
[upgrade-from-couchdb-tldr.md](upgrade-from-couchdb-tldr.md) instead.

Background, what changes for users, the variable names and the troubleshooting
table are in the [full guide](upgrade-from-couchdb.md); they apply here too.

Releases up to **1.1.6** store data in CouchDB, newer ones in PostgreSQL. The
migration tool only *reads* CouchDB and writes into an empty PostgreSQL database.
Plan some downtime: changes made after step 1 are not migrated.

## 1 Prepare (the same for both variants)

1. **Shut down the old system.** At least the backend, so nothing writes to
   CouchDB any more.
2. **Back up CouchDB.** Stop CouchDB too and copy its data directory (in the
   standard image `/opt/couchdb/data`; with a Docker volume, copy the volume's
   content, for example
   `docker run --rm -v <volume>:/from -v "$PWD/couchdb-backup":/to busybox cp -a /from/. /to/`).
   Keep this copy untouched. The migration reads a second copy of it (step 5).
3. **Start a new setup with the PostgreSQL-based backend.** Base it on your
   existing compose file: replace the CouchDB service with PostgreSQL, switch the
   backend image to the new release, and replace the backend's `CSAF_COUCHDB_*`
   variables with `CSAF_DB_*`. Keep all your other settings. The repository's
   [docker/compose.yaml](../../docker/compose.yaml) is the reference. The essential
   parts:

   ```yaml
   services:
     cms-db:
       image: postgres:17-alpine
       environment:
         POSTGRES_DB: csaf
         POSTGRES_USER: csaf
         POSTGRES_PASSWORD: <choose a new password>
       volumes:
         - cms-db-data:/var/lib/postgresql/data
       # ports: ["5432:5432"]   # only needed for the jar variant (2b)
     backend-cms:
       image: ghcr.io/secvisogram/csaf-cms-backend:2.0.0
       environment:
         CSAF_DB_HOST: cms-db
         CSAF_DB_PORT: 5432
         CSAF_DB_NAME: csaf
         CSAF_DB_USER: csaf
         CSAF_DB_PASSWORD: <the same password>
         # ... your other backend settings
   volumes:
     cms-db-data:
   ```

   Choose the PostgreSQL credentials **before** the first start: they are only
   applied when the data volume is empty. On its first start the backend creates
   the (empty) schema itself (Flyway); the backend log shows
   `Successfully applied 1 migration`.
4. **Stop everything except `cms-db`**, so nothing writes during the migration
   (for example `docker compose stop backend-cms <the other services>`).
5. **Prepare a CouchDB for the migration.** The tool reads your old data through
   CouchDB's HTTP API, so a CouchDB has to be running while it works. You do not
   reuse your old installation for that: you start a **throwaway CouchDB** on a
   *copy* of the backup and delete it afterwards. Make the copy now (CouchDB changes
   files when it opens them, so never point it at your only backup):

   ```shell
   cp -r couchdb-backup couchdb-copy
   ```

   The throwaway CouchDB itself is started in the next section, because it differs
   slightly between the two variants.

Now choose how to run the tool.

## 2a Container

The tool is the image `ghcr.io/secvisogram/cms-db-migration-tool:1.0.2`. The
simplest reliable way is to put the tool, the throwaway CouchDB from step 5 and
`cms-db` on the **same Docker network**. Then no database port has to be published, and you
address the databases by container name.

1. Find the network of your new setup, the one `cms-db` is attached to (for a
   compose project usually `<project>_default`):

   ```shell
   docker network ls
   docker inspect <cms-db container> --format '{{range $k,$v := .NetworkSettings.Networks}}{{$k}} {{end}}'
   ```

2. Start the throwaway CouchDB on the copy, on that network. Use the same CouchDB
   major version and admin credentials as your old installation:

   ```shell
   docker run -d --name old-couchdb --network <network> \
     -e COUCHDB_USER=<old user> -e COUCHDB_PASSWORD=<old password> \
     -v "$PWD/couchdb-copy":/opt/couchdb/data couchdb:3.4
   ```

3. Run the migration:

   ```shell
   # Dry run: reads and validates CouchDB, writes nothing
   docker run --rm --network <network> \
     ghcr.io/secvisogram/cms-db-migration-tool:1.0.2 --dry-run \
     --couchdb-url=http://old-couchdb:5984/<dbname> \
     --couchdb-user=<old user> --couchdb-password=<old password>

   # The real migration
   docker run --rm --network <network> \
     ghcr.io/secvisogram/cms-db-migration-tool:1.0.2 \
     --couchdb-url=http://old-couchdb:5984/<dbname> \
     --couchdb-user=<old user> --couchdb-password=<old password> \
     --pg-url=jdbc:postgresql://cms-db:5432/csaf \
     --pg-user=csaf --pg-password=<new password>
   ```

   `cms-db` is the name of your PostgreSQL service or container, and 5432 its
   internal port. If a database does not run in Docker, use its host name or IP
   address instead, as long as the container can reach it.

If the image cannot be pulled (`unauthorized`), build it from the repository, or
use the jar:

```shell
docker build -t ghcr.io/secvisogram/cms-db-migration-tool:1.0.2 \
  https://github.com/secvisogram/cms-db-migration-tool.git#v1.0.2
```

(Publishing the databases on `127.0.0.1` only and reaching them through
`host.docker.internal` does **not** work: a container cannot connect to a port that
is bound to the host's loopback interface only.)

## 2b Java jar

Requires **Java 21** on the machine that runs it. The tool connects to both
databases through **published ports** on that machine.

1. Publish the PostgreSQL port: enable the `ports:` line of `cms-db` from step 3
   (for example `"5432:5432"`) and restart `cms-db`. Remove it again afterwards if
   you do not need it.
2. Start the throwaway CouchDB on the copy, with its port published on the
   loopback interface only:

   ```shell
   docker run -d --name old-couchdb -p 127.0.0.1:5984:5984 \
     -e COUCHDB_USER=<old user> -e COUCHDB_PASSWORD=<old password> \
     -v "$PWD/couchdb-copy":/opt/couchdb/data couchdb:3.4
   ```

3. Check that both are reachable:

   ```shell
   curl -u <old user>:<old password> http://localhost:5984/<dbname>
   docker compose exec cms-db pg_isready -U csaf
   ```

   The first prints the database information, the second says "accepting
   connections".
4. Download the jar from the
   [release page](https://github.com/secvisogram/cms-db-migration-tool/releases)
   and compare its SHA-256 digest shown there with `sha256sum`:

   ```shell
   curl -L -O https://github.com/secvisogram/cms-db-migration-tool/releases/download/v1.0.2/csaf-couchdb-postgres-migrator.jar
   ```

5. Run the migration:

   ```shell
   # Dry run
   java -jar csaf-couchdb-postgres-migrator.jar --dry-run \
     --couchdb-url=http://localhost:5984/<dbname> \
     --couchdb-user=<old user> --couchdb-password=<old password>

   # The real migration
   java -jar csaf-couchdb-postgres-migrator.jar \
     --couchdb-url=http://localhost:5984/<dbname> \
     --couchdb-user=<old user> --couchdb-password=<old password> \
     --pg-url=jdbc:postgresql://localhost:5432/csaf \
     --pg-user=csaf --pg-password=<new password>
   ```

If another PostgreSQL is already listening on the host port, `localhost:5432`
reaches that one and the migration fails with an authentication error. Use a
different published port for `cms-db`.

## 3 Finish (the same for both variants)

Both runs must end with `RESULT: all counts match.` The real run replaces the
contents of the new database each time, so you can repeat it after fixing the data.

1. **Verify:** compare the counts of the report with the database (for example
   `SELECT count(*) FROM advisories;`).
2. **Go live:** start the backend and the rest of the new setup, open the CMS in your
   browser (reload the page) and log in. Then check:
   - The advisory list shows the advisories you know, in the same workflow states. The
     number matches the `Advisory` line of the migration report.
   - Open one advisory you know well: its content (and its comments, if it had any)
     look as before.
   - Change something harmless in a test advisory and save. It saves without an error.
3. **Clean up:** remove the throwaway CouchDB and its copy, and remove the `ports:`
   entry of `cms-db` again if you enabled it. Keep the backup until you are
   confident.

   ```shell
   docker rm -f old-couchdb
   docker run --rm -v "$PWD":/w busybox rm -rf /w/couchdb-copy
   ```

   The copy cannot be deleted with a plain `rm`: the CouchDB container changes the
   owner of its data files. The second command removes it from inside a container
   (or use `sudo rm -rf couchdb-copy`).

Rollback: start the old release with the original CouchDB data. Changes made in
the new system after the migration are not transferred back.

## 4 If something goes wrong

| Symptom | Cause and fix |
|---|---|
| `Migration failed: null` with `java.net.ConnectException` right after "Reading documents from …" | The tool cannot reach CouchDB. Check that `old-couchdb` is running, the host name and port in `--couchdb-url`, that the container is on the same network (2a), or that a loopback-only port is not used from a container. |
| `FATAL: password authentication failed` | Wrong PostgreSQL credentials, or another PostgreSQL answers on that port (2b). |
| `Read 0 documents` or an HTTP error from CouchDB | Wrong database name or credentials, or the copy does not contain the CouchDB files. |
| Duplicate tracking IDs, malformed documents, `--skip-malformed` | See the [troubleshooting table](upgrade-from-couchdb.md#4-troubleshooting) of the full guide. |

Note that the passwords on the command line are visible in the process list and in
your shell history.
