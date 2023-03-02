# OWASP Top 10 - Secvisogam

[OWASP Top 10:2021](https://owasp.org/Top10/)

## A01:2021 – Broken Access Control

[A01  Broken Access Control - OWASP Top 10:2021](https://owasp.org/Top10/A01_2021-Broken_Access_Control/)

We use Keycloak as Identity and Access Management solution.

This means that such a defect can only occur through misconfiguration, a
security vulnerability in Keycloak or missing checks in the backend application.

As a software provider we can only make sure the last case cannot happen. To
prevent this, we have automated tests through GH Actions in place that check if
API functionality can only be used with proper permissions.
New API endpoints will require additional test cases.

## A02:2021 – Cryptographic Failures

[A02 Cryptographic Failures - OWASP Top 10:2021](https://owasp.org/Top10/A02_2021-Cryptographic_Failures/)

We do not use any cryptography in the developed applications. We also have no
control over possible environments of possible users of the software. Therefore,
this does not apply.

## A03:2021 – Injection

[A03 Injection - OWASP Top 10:2021](https://owasp.org/Top10/A03_2021-Injection/)

### Filter Expression

The filter expression is used to select documents from the database. To prevent
arbitrary database queries, the expression is converted into an object
structure. Only allowed and implemented expressions like `and` can be used.
All selected documents from the database are then filtered. Only documents that
the user is allowed to view will be returned.

### Log Spoofing

A sanitize function is being used that removes newlines.

## A04:2021 – Insecure Design

[A04 Insecure Design - OWASP Top 10:2021](https://owasp.org/Top10/A04_2021-Insecure_Design/)

To minimize the risk for insecure design several steps were taken. Before
starting with the implementation we iterated multiple times over the following
design documents:

- General system architecture, using proven software for critical tasks. Like
  Keycloak and oauth2-proxy for identity and access management.

- Possible actions for each document state

- Data model

- Workflow states

- Semantic and Integer Versioning behavior

These documents were used throughout the development process.

## A05:2021 – Security Misconfiguration

[A05 Security Misconfiguration - OWASP Top 10:2021](https://owasp.org/Top10/A05_2021-Security_Misconfiguration/)

This is mostly relevant for those that run the provided software. Though there
are a few points that we can influence.

- No default users or passwords

- Error handling does not reveal stack traces or other sensitive information

- Framework settings are in a sensible state when starting with the default
  settings

## A06:2021 – Vulnerable and Outdated Components

[A06 Vulnerable and Outdated Components - OWASP Top 10:2021](https://owasp.org/Top10/A06_2021-Vulnerable_and_Outdated_Components/)

- There are no unused dependencies, unnecessary features, components, files, and
  documentation.

- We are using [dependabot](https://docs.github.com/en/code-security/dependabot)
  and Renovate to get notifications about CVEs in our dependencies

- The Team supporting the software after our initial release should regularly
  check for EOL or unmaintained dependencies.
  Before releasing new versions, dependencies should be checked for updates.

## A07:2021 – Identification and Authentication Failures

[A07 Identification and Authentication Failures - OWASP Top 10:2021](https://owasp.org/Top10/A07_2021-Identification_and_Authentication_Failures/)

The person running the provided software is responsible for the proper
configuration of the Keycloak instance.

Some things to check for would be brute force attacks, weak passwords and 2fa.
All of these can be influenced by properly configuring the Keycloak and
OAuth2-proxy instances.

## A08:2021 – Software and Data Integrity Failures

[A08 Software and Data Integrity Failures - OWASP Top 10:2021](https://owasp.org/Top10/A08_2021-Software_and_Data_Integrity_Failures/)

Currently, there are no binary releases that would need signatures or hashes.
The software also has no auto-update functionality.

## A09:2021 – Security Logging and Monitoring Failures

[A09 Security Logging and Monitoring Failures - OWASP Top 10:2021](https://owasp.org/Top10/A09_2021-Security_Logging_and_Monitoring_Failures/)

Since the user is hosting the software himself, everything related to the secure
storage of logs is out of scope.
We recommend the usage of [Open Telemetry](https://opentelemetry.io/) or similar
software to get observability data of the application as well as a system to
monitor that data.

## A10:2021 – Server-Side Request Forgery (SSRF)

[A10 Server Side Request Forgery (SSRF) - OWASP Top 10:2021](https://owasp.org/Top10/A10_2021-Server-Side_Request_Forgery_%28SSRF%29/)

This is relevant when validating URIs inside a CSAF document. We have taken the
following security measures to prevent these attacks:

- Only fetch the header of a URL

- We do not follow redirects

- We never use any returned values and do not return them to the user.
