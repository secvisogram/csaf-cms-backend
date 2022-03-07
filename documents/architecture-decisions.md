Introduction and Goals
======================

This document describes the architecture decisions for the csaf-cms-backend. This software is used to manage CSAF documents and provide a workflow to handle different document states like 'draft' or 'published'.

Requirements Overview
---------------------

The system must provide all functionality of a CSAF management system as described in [Common Security Advisory Framework Version 2.0](https://docs.oasis-open.org/csaf/csaf/v2.0/csd01/csaf-v2.0-csd01.html#9112-conformance-clause-12-csaf-management-system)

Additional features and requirements:

- Use a document database

- Export documents as html, markdown and pdf

- The structure of the exported document is described by a mustache html template

- It must be possible to write custom functions that generate further data which can then be added to the template.

- The ability to provide a company logo that is visible on the exported document

- Use Keycloak for user/group/role management and to allow LDAP integration

- Provide an API where the user can download prefilled Documents as a starting point for new documents

- Provide workflow functionality for the document states Draft, Review, Approved and Published

- Each change in a document has to be traceable. This will be done by saving the [JSON Patch](https://datatracker.ietf.org/doc/html/rfc6902) between each CSAF document version.

Quality Goals
-------------

- Static code checks

- Code reviews for pull requests

- Provide documentation for:
  
  - Data structures
  
  - API functionality
  
  - How to run/deploy the application
  
  - How to change the export template

- Test coverage of at least 95%

Stakeholders
------------

| Name                                               | Expectations                                               |
| -------------------------------------------------- | ---------------------------------------------------------- |
| tschmidtb51                                        | Provides knowledge and insight into the CSAF specification |
| mfd2007                                            | Provides knowledge and insight into the CSAF specification |
| [eXXcellent solutions GmbH](https://exxcellent.de) | Develops the application                                   |

Architecture Constraints
========================

## Technical Constraints

|     | Constraint                   | Description                                                                                                                                                                                  |
| --- | ---------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| TC1 | Implementation in Java       | The application is using Java 17 and Spring Boot.                                                                                                                                            |
| TC2 | Rest API                     | The API should be language and framework agnostic, however. It should be possible that clients can be implemented using various frameworks and languages.                                    |
| TC3 | OS indepentent development   | It should be possible to compile and run this application on all mayor operating systems (Linux, Mac and Windows)                                                                            |
| TC4 | Deployable to a Linux server | The target platform for deployment is Linux. There must be documentation available on how to deploy and run the application. Docker is not strictly required but should be provided as well. |

## Organizational Constraints

|     | Constraint       | Description                                                                                                                                           |
| --- | ---------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| OC1 | Time schedule    | The application should be finished until ...                                                                                                          |
| OC2 | IDE independence | No special IDE should be required. Use whatever fits your workflow. The repository should therefore not contain any IDE specific configuration files. |
| OC3 | Testing          | Provide tests to ensure functional correctness. At least 95% test coverage is required.                                                               |
| OC4 | Licensing        | The software must be available under the [MIT License](https://opensource.org/licenses/MIT).                                                          |

## Conventions

|     | Convention                 | Description                                                                              |
| --- | -------------------------- | ---------------------------------------------------------------------------------------- |
| C1  | Architecture documentation | Provide architecture documentation by using the [arc42](https://arc42.org/) method.      |
| C2  | Coding conventions         | This project is using .... This is enforced through....                                  |
| C3  | Language                   | The language used throughout the project is English. (code comments, documentation, ...) |

System Scope and Context
========================

Business Context
----------------

![Business Context](business-context.drawio.png)

[csaf-validator-service GIT repository](https://github.com/secvisogram/csaf-validator-service)

[csaf-validator-lib GIT repository](https://github.com/secvisogram/csaf-validator-lib)

Technical Context
-----------------



Risks and Technical Debts
=========================

## Document size limit is 8MB

At the current stage each document is stored as a single document in the database. At the moment CouchDB has a limit of 8MB per document. Since the current focus is the development of a user friendly [editor](https://github.com/secvisogram/secvisogram) for CSAF documents this should be enough.

In the future the documents can be split up and thus remove this restriction. This will also require a change to the API.