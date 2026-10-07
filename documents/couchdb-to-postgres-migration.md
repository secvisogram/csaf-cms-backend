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
| 6     | Service layer  | `AdvisoryService` rewritten against the Phase 5 bridge service; Mango selectors replaced by in-application filter evaluation (Section 4) |
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
ID (extracted from the JSON via a functional index, enforced unique at the
database level).

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
request and evaluated against each row's CSAF JSON in the application
layer. This keeps the REST API's filter syntax unchanged for existing
clients.

A malformed filter expression is rejected with a client error matching how
the CouchDB version behaved.

A GIN index on the CSAF column (`idx_advisories_csaf`, using
`jsonb_path_ops`) already exists to support this kind of query directly in
SQL - containment (`@>`) and jsonpath (`@?`) lookups over arbitrary, deep,
or array-valued CSAF fields - but nothing currently issues one. See
Section 6.

## 5 One-Time Data Migration

Moving existing CouchDB data into PostgreSQL is a separate, one-time
concern from the schema and code changes above, and is deliberately kept
out of the main application. The plan (not yet built, see Section 6) is a
separate import tool that would read every document out of the old CouchDB
database over its plain HTTP API, group it by type, and write the
equivalent rows into PostgreSQL, honoring the same foreign-key
relationships as the schema in Section 3.

## 6 Status and Remaining Work

| Item | Status                                                                                                                          |
|---|---------------------------------------------------------------------------------------------------------------------------------|
| Schema, entities, repositories, service layer, configuration, tests | Done                                                                                                                            |
| One-time data-migration tool (Section 5) | Not built. Only needed if CouchDB data must be preserved                                                                        |
| Remaining CouchDB-era classes (field-name constants, exception types) | Still in place and in active use. Left there for compatibility with existing code. Can be replaced in a new iteration.          |
| Search predicates pushed down into SQL, and server-side paging (Section 4) | Open. The current in-application filtering is correct but requires loading every row before filtering, which does not scale. It also blocks adding real server-side paging: a database-level `LIMIT`/`OFFSET` applied before an in-application filter would cut pages at the wrong rows, so paging can only be added correctly once filtering runs in SQL. |
