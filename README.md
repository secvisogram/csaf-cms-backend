# Secvisogram Backend

![Coverage](.github/badges/jacoco.svg)

##  Getting started

The configuration of the application as well as the compose file is done in 
a local **.env** file. To start, simply copy the **.env.example** file.
If you want different passwords, database names or ports you can change them 
here.

- run `docker-compose up`
- After Keycloak is up, open a second terminal window and run 
  `docker-compose up csaf-keycloak-cli` to import a realm with all the users 
  and roles already set up.
- To set up our CouchDB server open `http://127.0.0.1:5984/_utils/#/setup` 
  and run the [Single Node Setup](https://docs.couchdb.org/en/stable/setup/single-node.html). This creates databases like **_users** and 
  stops CouchDB from spamming our logs
- Open `http://localhost:9000/auth/` and log in with the admin user.
  - On the left side, navigate to "Clients" and select the Secvisogram client.
  - Select the **Credentials** tab and copy the Secret. This is our 
    `CSAF_CLIENT_SECRET` environment variable.
- [Generate a cookie secret](https://oauth2-proxy.github.io/oauth2-proxy/docs/configuration/overview/#generating-a-cookie-secret) 
  and paste it in `CSAF_COOKIE_SECRET`.
- Create a database in CouchDB with the name specified in `CSAF_COUCHDB_DBNAME`
- restart compose

You should now be able to start the spring boot application, navigate to 
`localhost:4180/api/2.0/about`, log in with one of the users and get a 
response from the server.


## Commands

### build and execute tests

./gradlew clean build

### build and run SpotBugs

./gradlew clean assemble spotbugsMain

### start application

./gradlew bootRun

with main class: de.exxcellent.bsi.SecvisogramApplication

### check application running

http://localhost:8081/api/2.0/about

Swagger UI

http://localhost:8081/swagger-ui/

OpenAPI specification

http://localhost:8081/v3/api-docs/

## How to use

...

## Contributing


...

## Dependencies

### Spring Boot

#### Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.6.2/gradle-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/2.6.2/gradle-plugin/reference/html/#build-image)
* [Mustache](https://docs.spring.io/spring-boot/docs/2.6.2/reference/htmlsingle/#boot-features-spring-mvc-template-engines)
* [Spring Data Couchbase](https://docs.spring.io/spring-boot/docs/2.6.2/reference/htmlsingle/#boot-features-couchbase)
* [Spring Web](https://docs.spring.io/spring-boot/docs/2.6.2/reference/htmlsingle/#boot-features-developing-web-applications)

#### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/bookmarks/)

#### Additional Links
These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)


### Code Quality Rules

[Exxcellent Code Quality Rules](https://www.exxcellent.de/confluence/pages/viewpage.action?pageId=65113099)

#### SpotBugs

- [IntelliJ SpotBugs](https://plugins.jetbrains.com/plugin/14014-spotbugs)
- [spotbugs-gradle-plugin](https://github.com/spotbugs/spotbugs-gradle-plugin)
- [find-sec-bugs](https://find-sec-bugs.github.io/)

#### Jacoco

- [Jacoco Plugin](https://docs.gradle.org/current/userguide/jacoco_plugin.html#sec:jacoco_report_configuration)

### Links

#### CSAF 
[OASIS CSAF](https://oasis-open.github.io/csaf-documentation/)

[BSI CSAF](https://www.bsi.bund.de/DE/Themen/Unternehmen-und-Organisationen/Informationen-und-Empfehlungen/Empfehlungen-nach-Angriffszielen/Industrielle-Steuerungs-und-Automatisierungssysteme/CSAF/CSAF_node.html)

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


#### Mustache

[Mustache samskivert](https://github.com/samskivert/jmustache)


#### PoC for Backend

[PoC Backend](https://github.com/csaf-poc/csaf_backend)

#### Open API/ Swagger

[Open API](https://www.openapis.org/)
[Swagger Annotations](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations)

#### diagrams.net (formerly known as draw.io)

- [diagrams.net](https://www.diagrams.net/)

- [Intellij Integration](https://plugins.jetbrains.com/plugin/15635-diagrams-net-integration)