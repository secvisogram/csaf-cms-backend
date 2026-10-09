# Upgrading from the CouchDB release (<= 1.1.x)

Releases up to 1.1.x store data in CouchDB, newer ones in PostgreSQL. Existing
data must be moved once, with the
[cms-db-migration-tool](https://github.com/secvisogram/cms-db-migration-tool).

Follow its
[upgrade guide](https://github.com/secvisogram/cms-db-migration-tool/blob/main/docs/upgrade-from-couchdb.md).
As stated there, only the migration compose file (`compose.yaml`) and
`.env.example` from the migration tool repository are needed. Either clone
that repository or just download those two files.
