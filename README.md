# BSI Secvisogram CSAF Backend

![Coverage](https://raw.githubusercontent.com/secvisogram/csaf-cms-backend/badges/.github/badges/jacoco.svg)

- [About The Project](#about-the-project)
- [Getting started](#getting-started)
- [How to use](#how-to-use)
- [Developing](#developing) — see [docker/README.md](docker/README.md) for the full local Docker guide
- [Contributing](#contributing)
- [Dependencies](#dependencies)

## About The Project

This is the backend for a Content Management System for CSAF documents.
It offers a REST service for listing, searching, deleting, creating, commenting on and exporting CSAF documents.

[(back to top)](#bsi-secvisogram-csaf-backend)

## Getting started

> **Just want to run/try the backend locally?** This section describes a manual/production
> setup (building the jar yourself, providing your own Keycloak/CouchDB/proxy infrastructure).
> If you just want to develop against or try out the backend locally with Docker, you don't
> need any of the steps below — skip straight to [Developing](#developing) and follow
> [docker/README.md](docker/README.md) instead.

To run the CSAF CMS server you need the following:

- [Keycloak](https://www.keycloak.org/)
- A proxy like [oauth2-proxy](https://oauth2-proxy.github.io/oauth2-proxy/)
- [CouchDB](https://couchdb.apache.org/)

You can find an example setup for local development in the 'compose.yaml' and
an example configuration for Keycloak in 'docker/config/keycloak/csaf-realm.json'. You can
take this as a starting point, but please check the documentation of the
individual projects for a proper production setup. We also recommend
running everything behind some kind of reverse proxy. Please take a look at our
[Architecture](https://github.com/secvisogram/csaf-cms-backend/blob/main/documents/BSISecvisogramArchitecture.drawio.svg)
for an overview.

The [secvisogram](https://github.com/secvisogram/secvisogram) frontend is usable
as a standalone version without this server. You can still use this standalone
mode if the frontend is not behind the proxy, like in the development setup.
In this setup where both standalone and server mode are available, the login is
only required to manage documents on the server or validate against the
[validator service](https://github.com/secvisogram/csaf-validator-service).

To build the application run:

```shell
./mvnw package
```

The resulting jar file in the `target` folder can then be run with
`java -jar filename.jar`. To manage the process you can use Docker or an init
system of your choice.

Alternatively, a ready-to-run container image is published to
[`ghcr.io/secvisogram/csaf-cms-backend`](https://github.com/secvisogram/csaf-cms-backend/pkgs/container/csaf-cms-backend)
(built from `Dockerfile` by [`.github/workflows/docker-publish.yml`](.github/workflows/docker-publish.yml)
on every release), configured entirely through environment variables (see `.env.example`).

[(back to top)](#bsi-secvisogram-csaf-backend)

## How to use

Please have a look at the [API documentation](https://secvisogram.github.io/csaf-cms-backend/) on how to use this application.

[(back to top)](#bsi-secvisogram-csaf-backend)

### Management of tracking information

The system automatically manages information under `document/tracking` of CSAF documents.
The revision history is managed as described in the [architecture decisions document](documents/architecture-decisions.md).

The tracking ID is automatically set to a temporary ID when creating a new advisory and updated to a final ID when the document is published.
For generating the tracking IDs, a company name should be set in the environment variable `CSAF_TRACKINGID_COMPANY`.
The variable `CSAF_TRACKINGID_DIGITS` defines the number of digits used in the tracking ID. It defaults to 5 if nothing is set.
If `CSAF_REFERENCES_BASE_URL` is defined, a JSON reference in `document/references` with the set URL is added when publishing the document.
To also add an HTML reference (`.html` variant), set `CSAF_WORKFLOW_CREATE_HTML_REFERENCE=true` (default: `false`).
See **.env.example** for an example configuration.

### Management of engine data

When creating or updating an advisory, the information for `document/tracking/engine` is updated.
The `name` and `version` are set according to the corresponding values of the backend's build.

### Importing

Existing valid and published advisories can be imported on startup of the application.
The advisories to be imported must be stored in JSON format in a directory called `import` in the root directory.
Duplicates are identified by their tracking ID and not imported again.

## Developing

For the full, step-by-step guide to running the whole stack (CouchDB, Keycloak, oauth2-proxy,
validator service, Secvisogram and the backend) locally with Docker Compose — including the
required `.env` setup, cookie secret generation, default users and how to debug the backend
on the host — see **[docker/README.md](docker/README.md)**.

The sections below cover topics that are relevant regardless of how you run the backend.

### Login & Logout in combination with Secvisogram

Some explanation on the logoutUrl configured in `.well-known/appspecific/de.bsi.secvisogram.json` for Secvisogram

``` 
"logoutUrl": "/oauth2/sign_out?rd=http%3A%2F%2Flocalhost%2Frealms%2Fcsaf%2Fprotocol%2Fopenid-connect%2Flogout%3Fpost_logout_redirect_uri%3Dhttp%3A%2F%2Flocalhost%26client_id%3Dsecvisogram", 
```

`/oauth2/sign_out` is the logout URI from the OAUTH-Proxy. This will invalidate the session on the proxy. Then, a redirect to Keycloak (`http://localhost/realms/csaf/protocol/openid-connect/logout?post_logout_redirect_uri=http%3A%2F%2Flocalhost&client_id=secvisogram`) is necessary to log out from the session on Keycloak. Subsequently, there is a redirect back to Secvisogram (`localhost`).
When hostnames are changed, this has to adapted.

This has to be correctly encoded in the logoutUrl. The following code snippet shows how to do this:
```js
var postLogoutUrl = "http://localhost/realms/csaf/protocol/openid-connect/logout?post_logout_redirect_uri=http://localhost&client_id=secvisogram"
var logoutUrl = "/oauth2/sign_out?rd=" + encodeURIComponent(postLogoutUrl) 
``` 

### build and execute tests

`` ./mvnw clean verify``


### start application

To run/debug the backend on the host:

`` ./mvnw spring-boot:run``

with main class: de.bsi.secvisogram.csaf_cms_backend.SecvisogramApplication

### check application running

The port is defined in .env - CSAF_CMS_BACKEND_PORT, default 8081.

http://localhost:8081/api/v1/about

Swagger UI

http://localhost:8081/swagger-ui/index.html

OpenAPI specification

http://localhost:8081/api-docs

For accessing CouchDB when running the local Docker setup, see
[Accessing CouchDB](docker/README.md#accessing-couchdb) in `docker/README.md`.

## Contributing

You can find our guidelines here [CONTRIBUTING.md](https://github.com/secvisogram/secvisogram/blob/main/CONTRIBUTING.md)

[(back to top)](#bsi-secvisogram-csaf-backend)

## Dependencies

### Check for Maven Plugin update

`` ./mvnw versions:display-plugin-updates ``

## Check for dependency update
`` ./mvnw versions:display-dependency-updates ``

### Spring Boot

#### Reference Documentation

For further reference, please consider the following sections:

* [Mustache](https://docs.spring.io/spring-boot/docs/2.6.2/reference/htmlsingle/#boot-features-spring-mvc-template-engines)
* [Spring Data Couchbase](https://docs.spring.io/spring-boot/docs/2.6.2/reference/htmlsingle/#boot-features-couchbase)
* [Spring Web](https://docs.spring.io/spring-boot/docs/2.6.2/reference/htmlsingle/#boot-features-developing-web-applications)

[(back to top)](#bsi-secvisogram-csaf-backend)

#### Guides

The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/bookmarks/)

[(back to top)](#bsi-secvisogram-csaf-backend)

[(back to top)](#bsi-secvisogram-csaf-backend)

### Code Quality Rules

[Exxcellent Code Quality Rules](https://www.exxcellent.de/confluence/pages/viewpage.action?pageId=65113099)

[(back to top)](#bsi-secvisogram-csaf-backend)

#### SpotBugs

- [IntelliJ SpotBugs](https://plugins.jetbrains.com/plugin/14014-spotbugs)
- [find-sec-bugs](https://find-sec-bugs.github.io/)

[(back to top)](#bsi-secvisogram-csaf-backend)

### Links

#### CSAF

[OASIS CSAF](https://oasis-open.github.io/csaf-documentation/)

[BSI CSAF](https://www.bsi.bund.de/DE/Themen/Unternehmen-und-Organisationen/Informationen-und-Empfehlungen/Empfehlungen-nach-Angriffszielen/Industrielle-Steuerungs-und-Automatisierungssysteme/CSAF/CSAF_node.html)

[(back to top)](#bsi-secvisogram-csaf-backend)

#### JSON

- [CSAF 2.0 JSON Schema](https://docs.oasis-open.org/csaf/csaf/v2.0/csaf_json_schema.json)
- [JSON Schema](https://json-schema.org/draft/2019-09/json-schema-core.html)
- [JSON Schema Validation](https://json-schema.org/draft/2019-09/json-schema-validation.html)
- [JSON Hyper-Schema](https://json-schema.org/draft/2019-09/json-schema-hypermedia.html)
- [CVSS 2.0](https://www.first.org/cvss/cvss-v2.0.json)
- [CVSS 3.0](https://www.first.org/cvss/cvss-v3.0.json)
- [CVSS 3.1](https://www.first.org/cvss/cvss-v3.1.json)
- [JSON API](https://jsonapi.org/)
- [JSON Patch](http://jsonpatch.com/)
- [JSON Pointer](https://datatracker.ietf.org/doc/html/rfc6901)

[(back to top)](#bsi-secvisogram-csaf-backend)

#### Mustache

[Mustache samskivert](https://github.com/samskivert/jmustache)

[(back to top)](#bsi-secvisogram-csaf-backend)

#### PoC for Backend

[PoC Backend](https://github.com/csaf-poc/csaf_backend)

[(back to top)](#bsi-secvisogram-csaf-backend)

#### Open API/ Swagger

[Open API](https://www.openapis.org/)
[Swagger Annotations](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations)

[(back to top)](#bsi-secvisogram-csaf-backend)

