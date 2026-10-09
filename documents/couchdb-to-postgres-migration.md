# CouchDB to PostgreSQL Migration

## 1 Background

The backend originally stored all data in a single CouchDB database, with
every document carrying a `type` field (`Advisory`, `AdvisoryVersion`,
`Comment`, `CommentAuditTrail`, `AuditTrailDocument`, `AuditTrailWorkflow`,
`Counter`) to distinguish what it represented. Filtering and searching were
done through CouchDB's Mango selector query language.

Goals of the migration:

- Remove the CouchDB-specific query layer (Mango selectors), which does not
  support joins or transactions.
- Gain real transactional guarantees for multi-step writes (an advisory
  update, its audit trail entry, and its version snapshot should succeed or
  fail together).
- Use a relational DB schema for the parts of the data that are naturally
  relational (advisories, comments, audit trails), while keeping the CSAF
  document itself as a single JSON value, since its structure is defined by
  an external specification and varies considerably between documents.

## 2 Approach

| Phase | Area           | What changed                                                                                                                             |
|-------|----------------|------------------------------------------------------------------------------------------------------------------------------------------|
| 1     | DB Schema      | Seven CouchDB document types became seven tables (Section 3); CSAF content kept as JSONB                                                 |
| 2     | Dependencies   | Removed the Cloudant SDK; added Spring Data JPA, the PostgreSQL driver, and Flyway                                                       |
| 3     | Configuration  | Flyway owns the DB schema; Hibernate runs in validation-only mode (Section 3)                                                            |
| 4     | Entities       | One JPA entity per table                                                                                                                 |
| 5     | Repositories   | One Spring Data repository per entity, wrapped by a single bridge service                                                                |
| 6     | Service layer  | `AdvisoryService` rewritten against the Phase 5 bridge service; Mango selectors replaced by filters compiled to SQL (Section 4)          |
| 7     | Tests          | Data-layer tests run against a real, disposable PostgreSQL instance                                                                      |
| 8     | Data migration | Optional one-time import tool, kept external to the main application (Section 5)                                                         |
| 9     | Cleanup        | Remove remaining CouchDB-era classes once nothing else depends on them (Section 6)                                                       |

## 3 Target DB Schema

See file **V1__initial_schema.sql** for the full schema definition. The
following is a summary of the most important points:

Each of the seven CouchDB document types becomes its own table. The full
CSAF document is kept as a `JSONB` column rather than normalized into
relational columns.

Indexes cover the columns the application actually filters or joins on:
workflow state, owner, the advisory/comment foreign keys, and the tracking
ID (stored in its own `tracking_id` column and enforced unique at the
database level on `advisories`).

**List columns**: `advisories` and `advisory_versions` carry three redundant
copies of values from the CSAF document: `tracking_id`, `document_title` and
`current_release_date`. They exist so the list endpoints can return their
rows without reading the large `csaf` column. `PostgresRepositoryService`
fills them whenever it saves an advisory or a version snapshot, and the
migration tool (Section 5) writes them on import. The CSAF document remains
the source of truth.

**Optimistic locking**: CouchDB's `_rev` field is replaced by a plain
`version` column on `advisories` and `comments`, using the standard JPA
`@Version` mechanism. The client-visible "revision" value returned by the
API is simply this number rendered as a string, so the REST contract is
unchanged.

DB schema changes are managed by Flyway migrations. Hibernate runs in
validation-only mode.

**How Spring Boot runs migrations**: Flyway is triggered automatically at
application startup, before the rest of the application context finishes
initializing. Migration files live under `src/main/resources/db/migration`,
named `V<number>__<description>.sql` (for example `V1__initial_schema.sql`).
Flyway keeps its own bookkeeping table, `flyway_schema_history`, in the
target database, recording every migration it has applied together with a
checksum of its contents.

## 4 Query and Filter

Old query path:

```text
Filter expression (JSON)  ->  CouchDB Mango selector  ->  CouchDB _find
```

New query path: the same JSON-encoded filter expression is parsed once per
request and compiled to a SQL `WHERE` clause by `AdvisoryListQueryService`.
This keeps the REST API's filter syntax unchanged for existing clients, and
the database does the filtering, so only matching rows are read.

- `owner` and `workflowState` are compared as plain columns.
- Any other selector is treated as a path into the CSAF document and
  becomes a `jsonb_path_exists(csaf, <jsonpath>)` condition. All values,
  including the search text of `ContainsIgnoreCase`, are bound parameters or
  escaped literals, never concatenated into the statement.
- The list queries select only the scalar list columns (Section 3), not the
  `csaf` document. This applies to the advisory list and to the advisory
  version list shown to auditors.
- Which advisories a user may see is added to the same statement as a
  visibility clause derived from the user's roles.

A filter on a CSAF path matches only documents that actually contain that
path. A document without the path is excluded, as it was with CouchDB.

A malformed filter expression is rejected with a client error matching how
the CouchDB version behaved.

The GIN index on the CSAF column (`idx_advisories_csaf`, using
`jsonb_path_ops`) supports containment (`@>`) lookups. The jsonpath
predicates generated here, with comparison and `like_regex`, are probably
not served by it, so such filters may still scan each row's JSON. This has
not been measured. See Section 6.

## 5 One-Time Data Migration

Moving existing CouchDB data into PostgreSQL is a one-time task, separate
from the schema and code changes above, and deliberately kept out of the main
application. It is done by the standalone
[cms-db-migration-tool](https://github.com/secvisogram/cms-db-migration-tool)
(its own project and release cycle, published as a Java 21 jar and as a
container image). It needs no CouchDB SDK: it reads every document over
CouchDB's plain HTTP API, groups it by type, and writes the equivalent rows
into PostgreSQL, honoring the foreign-key relationships of Section 3.

**How to run it.** The user-facing guides are in
[couchdb-to-postgres-migration-guides/](couchdb-to-postgres-migration-guides/README.md):

- the `docker/` setup of this repository ships a compose layer in
  `docker/migrate` that runs the tool against a copy of the old data;
- for any other setup, the tool runs as a container or as a plain jar.

Every run should start with `--dry-run`, which reads and validates CouchDB
and never touches PostgreSQL.

**What the tool does.** The target database must already carry the schema
from Section 3 (the backend's first start applies it with Flyway). The tool
validates all documents, then truncates the target tables and loads
everything in one transaction. It prints the document count per type and
exits non-zero if a pre-check fails or a count does not match. Treat a
non-zero exit as "do not cut over yet". The field mapping is in the tool's
`FieldMappings.java`. `_id` is kept as the primary key, the two `Counter`
documents keep their labels as `counters.id`, and `_rev` is dropped
(`version` starts at 0).

**Known data issues the tool handles or reports**

- Malformed documents (missing required fields, bad UUIDs or timestamps,
  dangling references) are detected before anything is written and listed by
  `_id`; by default the run aborts, and `--skip-malformed` loads the valid
  documents and reports the rest (exit code stays non-zero).
- Advisories sharing a tracking ID violate the unique index
  `idx_advisories_tracking_id`. The tool lists them and aborts before
  writing anything; fix them in CouchDB first.
- `created_at` is derived from the earliest audit-trail entry of each
  advisory and comment. The old documents' own `createdAt` field is not used:
  it is missing on advisories that were edited at least once, so it cannot be
  relied on. Entities without audit entries, and all `advisory_versions`, get
  the import time. The column is internal: it is not exposed by the REST API,
  and the CSAF document carries its own dates (revision history).

## 6 Status and Remaining Work

| Item | Status                                                                                                                          |
|---|---------------------------------------------------------------------------------------------------------------------------------|
| Schema, entities, repositories, service layer, configuration, tests | Done                                                                                                                            |
| One-time data-migration tool (Section 5) | Separate project (`cms-db-migration-tool`, released as jar and container image, with upgrade guides). Needed only if CouchDB data must be preserved |
| Remaining CouchDB-era classes (field-name constants, exception types) | Renamed out of the `couchdb` package: exceptions are in `db`, the field-name enums in `json.field`, and `CouchDbField` became `LegacyDocumentField`. They still serve the JSON wrappers (`_id`, `_rev` and `type` handling). Can be replaced in a new iteration. |
| Search predicates pushed down into SQL (Section 4) | Done for the advisory list and the advisory version list. |
| Server-side paging | Open. Filtering now runs in SQL, so a database-level `LIMIT`/`OFFSET` can be added correctly. It is not implemented yet. |
| Index support for CSAF-path filters | Open. See Section 4. Add an index or a different predicate shape only if measurements show a need. |
