# Local development with Docker

This guide describes how to run the whole CSAF CMS Backend stack (PostgreSQL,
Keycloak, oauth2-proxy, the validator service, Secvisogram and the backend
itself) locally with Docker Compose. **This is the only guide you need if you
just want to try out or develop against the backend locally** — you do not
need to read the ["Getting started"](../README.md#getting-started) section
of the main README, which describes a manual/production setup without
Docker.

Please note that the following setup is for development purposes only and
should not be used in production.

```mermaid
   C4Component
    title Component diagram for CSAF CMS Backend
    Person(user, "User")
    Container(reverseproxy, "Reverse-Proxy", "nginx")

    Container_Boundary(c4, "Internal") {
        Container(secvisogram, "Secvisogram", "nginx + javascript", "Provides secvisogramm via their web browser.")

        Container_Boundary(c2, "Keycloak") {
            Container(keycloak, "Keycloak", "keycloak")
            ContainerDb(keycloak-db, "PostGreSQL", "Keycloak-Database")
        }

        Container_Boundary(c3, "Oauth") {
            Container(oauth, "OAuth2-Proxy", "Authentication for REST-API")
            Container(validator, "CSAF validator service", "node")

            Container_Boundary(c1, "Backend") {
                Container(backend, "CSAF-CMS-Backend", "Spring Boot")
                ContainerDb(backend-db, "PostgreSQL", "CMS-Backend-Database")
            }
        }

        Container(trustedprovider, "Trusted Provider", "nginx + go", "Trusted CSAF provider")
    }

    Rel(user, reverseproxy, "", "HTTPS")
    Rel(reverseproxy, secvisogram, "/")
    Rel(reverseproxy, trustedprovider, "/.well-known/csaf")
    Rel(reverseproxy, oauth, "/api/*")
    Rel(reverseproxy, keycloak, "/realm/csaf/")
    Rel(oauth, validator, "/api/v1/test")
    Rel(oauth, validator, "/api/v1/validate")
    Rel(oauth, backend, "/api/v1/advisories/*")
    Rel(backend, backend-db, "")
    Rel(backend, keycloak, "")
    Rel(keycloak, keycloak-db, "")
    Rel(backend, trustedprovider, "/cgi-bin/csaf_provider.go/api/upload")


```

## Step-by-step setup

Configuration is split across two **.env** files, each with its own
**.env.example** template:

- **`.env`** (repo root): configuration for the backend application itself
  (PostgreSQL connection, OIDC issuer, document templates, versioning, tracking
  IDs, workflow flags, ...). This is read both when running the backend on
  the host (`./mvnw spring-boot:run`) and by the containerized
  `backend-cms` compose service.
- **`docker/.env`**: configuration needed only to orchestrate the local
  docker compose stack itself (Keycloak, oauth2-proxy, PostgreSQL container
  credentials, ports, ...).

Follow these steps **in order**:

1. Copy both `.env.example` files to `.env`:

   ```shell
   cp .env.example .env
   cp docker/.env.example docker/.env
   ```

   A few values (e.g. PostgreSQL credentials, the backend port) intentionally
   appear in both files, with comments cross-referencing each other — keep
   them in sync if you change them.

2. [Generate a cookie secret](https://oauth2-proxy.github.io/oauth2-proxy/configuration/overview#generating-a-cookie-secret)
   and paste it into `CSAF_COOKIE_SECRET` in `docker/.env`. **This must be
   done before the first `docker compose up`.**

3. Start the stack: run `docker compose up -d --build` in folder `docker`.

   The `cms-db` PostgreSQL container creates its database, user and password
   from `CSAF_DB_NAME` / `CSAF_DB_USER` / `CSAF_DB_PASSWORD` in `docker/.env`
   on first start — no manual setup step is needed. The backend then applies
   its schema automatically via Flyway migrations on startup (see
   [documents/couchdb-to-postgres-migration.md](../documents/couchdb-to-postgres-migration.md)).

4. Keycloak is initialized automatically: on startup it imports the `csaf`
   realm, the `secvisogram` client, all client roles and the development
   test users from `docker/config/keycloak/csaf-realm.json` (via
   `--import-realm`). There is no manual setup step and no need to copy the
   client secret out of the Keycloak UI.
    - `CSAF_CLIENT_SECRET` in `docker/.env` is a **fixed, development-only
      value** (see `docker/.env.example`). Keycloak imports the realm with
      this secret and oauth2-proxy is configured with the same value, so
      both sides always match. If you change it after the first start,
      delete the Keycloak database volume (`docker/data/keycloak-db`) so
      the realm gets re-imported with the new secret.
    - Note: `--import-realm` only imports the realm if it does not already
      exist. If you change the realm file later and want it re-imported,
      remove the Keycloak database volume first: `docker compose down` and
      delete `docker/data/keycloak-db`.

5. (optional) Initialize the trusted CSAF provider:
   `docker compose up trusted-provider-setup`.
    - The folder `docker/config/trustedprovider` contains example /
      development PGP keys.
    - More details on configuring the trusted provider can be found at
      [GoCSAF](https://github.com/gocsaf/csaf).

6. (required for exports) Install
   [pandoc (tested with version 2.18)](https://pandoc.org/installing.html)
   as well as [weasyprint (tested with version 56.0)](https://weasyprint.org/)
   and make sure both are in your PATH.

7. (optional for exports) Define the path to a company logo that should be
   used in the exports through the environment variable
   `CSAF_COMPANY_LOGO_PATH`. The path can either be relative to the project
   root or absolute. See `.env.example` for an example.

8. `backend-cms` is now running. If you are actively working on backend
   code, see [Debugging the backend](#debugging-the-backend) below for the
   faster host-based workflow with IDE debugging.

You should now be able to navigate to `http://localhost/api/v1/about`, log in
with one of the users below and get a response from the server.

## Default users

| User       | Password   | Roles                                                       |
|------------|------------|-------------------------------------------------------------|
| registered | registered | **registered**                                              |
| author     | author     | registered, editor, **author**                              |
| editor     | editor     | registered, **editor**                                      |
| publisher  | publisher  | registered, editor, **publisher**                           |
| reviewer   | reviewer   | registered, **reviewer**                                    |
| auditor    | auditor    | **auditor**                                                 |
| all        | all        | **auditor, reviewer, publisher, editor, author, registred** |
| none       | none       |                                                             |

You should now also be able to access Secvisogram by navigating to `http://localhost/`.

## Debugging the backend

To run/debug the backend on the host (e.g. from an IDE) instead of in its container:

1. Stop the container: `docker compose stop backend-cms`
2. In `docker/.env`, set `CSAF_CMS_BACKEND_HOST=host.docker.internal`
3. Restart oauth2-proxy so it picks up the change: `docker compose up -d oauth2-proxy`
4. Start the backend on the host: `./mvnw spring-boot:run`

To switch back to the containerized backend, set `CSAF_CMS_BACKEND_HOST` back to
`backend-cms` and run `docker compose up -d` again.

## Accessing PostgreSQL

The port is defined in `docker/.env` (`CSAF_DB_PORT`, default 5432). Connect
with `psql` or any PostgreSQL client using the host/port and the credentials
from `docker/.env` (`CSAF_DB_USER` / `CSAF_DB_PASSWORD` / `CSAF_DB_NAME`), e.g.:

```shell
psql -h localhost -p 5432 -U csaf -d csaf
```

[(back to top)](#local-development-with-docker)
