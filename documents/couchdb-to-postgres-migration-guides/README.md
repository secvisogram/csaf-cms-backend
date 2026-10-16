# Migrating from CouchDB to PostgreSQL — which guide?

Releases up to **1.1.6** store their data in CouchDB. Starting with
**2.0.0** the backend uses PostgreSQL. The REST API is unchanged,
but existing data has to be moved **once**, with the migration tool. Pick the guide
that matches how you run the system:

| Your situation | Guide |
|---|---|
| New installation, nothing to keep | No migration needed. Install fresh: [docker/README.md](../../docker/README.md) |
| You run the stack from the `docker/` folder of a clone of this repository | [Short version](upgrade-from-couchdb-tldr.md): the steps only. For explanations, variants and troubleshooting use the [full guide](upgrade-from-couchdb.md). |
| You run your own compose file, a setup independent of this repository, or something else | [Custom setup](upgrade-from-couchdb-custom-setup.md): run the migration tool as a container or as a jar yourself. |

For the technical background (schema, design decisions, status) see
[couchdb-to-postgres-migration.md](../couchdb-to-postgres-migration.md).

## What all guides have in common

- **Downtime.** The old backend must be stopped while the data is copied. Changes
  made after the copy are not migrated.
- **Backup first.** Your old CouchDB data is only read, never changed, and you
  should keep a backup anyway.
- **Empty target.** The new PostgreSQL database is created empty by the new
  backend's first start (it creates the schema itself); the migration then fills it.
- **The tool.** The
  [cms-db-migration-tool](https://github.com/secvisogram/cms-db-migration-tool) is
  available as a Java 21 jar and as a container image (see its
  [releases](https://github.com/secvisogram/cms-db-migration-tool/releases)). Every
  run starts with a dry run that validates the data and writes nothing.
- **Success check.** The tool prints the number of documents per type and ends with
  `RESULT: all counts match.` Any other result means: do not continue.
- **Repeatable.** The real run replaces the contents of the new database, so it can
  be repeated after fixing data problems.
- **Rollback.** Start the old release with the original CouchDB data. Changes made
  in the new system after the migration are not transferred back.
