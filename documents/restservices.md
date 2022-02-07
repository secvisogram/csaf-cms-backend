# CSAF-Backend

[Leistungbeschr 2.2]

`In diesem Arbeitspaket werden die „Managementfunktionen“ gemäß CSAF-Standard
als REST-Service implementiert:`

[Leistungbeschr 2.2.1]

`In einem ersten Schritt soll der Auftragnehmer ein Konzept für das Backend
entwickeln. In dem Konzept sind die wesentlichen Architekturentscheidungen
aufzuführen sowie Datenstrukturen und die RESTSchnittstelle zu beschreiben.
Das Konzept umfasst auch die Web-Schnittstelle aus AP 2.3. Das Konzept wird dem
AG abgestimmt`

[CSAF2.0]

`9.1.1 Conformance Clause 1: CSAF document`

`A text file or data stream satisfies the "CSAF document" conformance profile
 if it:`

- `conforms to the syntax and semantics defined in section 3.`(Schema)
- `satisfies at least one profile defined in section 4.`(Profile)
- `does not fail any mandatory test defined in section 6.1.`(Tests)

`9.1.2 Conformance Clause 2: CSAF producer`

`A program satisfies the "CSAF producer" conformance profile if the program:`

- `produces output in the CSAF format, according to the conformance profile
 "CSAF document" .`
- `satisfies those normative requirements in section 3 and 8 that are designated
 as applying to CSAF producers.` (Schema, Security)

`9.1.6 Conformance Clause 6: CSAF content management system`

`A CSAF content management system satisfies the "CSAF content management system"
 conformance profile if the content management system:`

- `satisfies the "CSAF producer" conformance profile.`
- `satisfies the "CSAF viewer" conformance profile`

## Abfrage von CSAF - Daten

## Auflisten/Ansehen von CSAF-Dokumente

[Leistungbeschr 2.2]

`Die Liste der CSAF-Dokument muss entsprechend der
Berechtigung des jeweiligen Nutzers und des Status erfolgen`

- `Registered darf alle CSAF-Dokumente im Status „published“ auflisten und ansehen.`
- `Author darf eigene (von dem Nutzer) CSAF-Dokumente im Status „Draft“
 auflisten, ansehen, bearbeiten und löschen.`
- `Editor darf alle Advisories im Status „Draft“ auflisten, ansehen, bearbeiten
 und löschen`
- `Publisher darf alle CSAF-Dokumente im Status „approved“ auflisten und ansehen`
- `Reviewer darf alle CSAF-Dokumente im Status „review“ und nicht von dem Nutzer
 erstellt wurden, auflisten und ansehen.`
  -`Auditor kann alle CSAF-Dokument auflisten und ansehen`

[CSAF2.0 - 9.1.6]

- `list all CSAF documents within the system`

### Auflisten/Ansehen des Audits-Trails von CSAF-Dokumente

[Leistungbeschr 2.2]

- `Auditor darf den Audit-Trail zu allen CSAF-Dokumenten auflisten und ansehen`
- `Es ist ein Audit Trail für jedes CSAF-Dokument zu führen. Im Audit Trail wird
 erfasst, wer wann welche Änderungen am CSAF Dokument vorgenommen hat. Dazu
 gehören das Erstellen, Editieren, Statusänderungen, Kommentare und Antworten
 darauf, einschließlich der durchführende Nutzer`

[CSAF2.0 - 9.1.6]

- `show an audit log for each CSAF document`

### Auflisten/Ansehen der Versionen von CSAF-Dokumente

[Leistungbeschr 2.2]

- `Auditor kann alle Versionen der CSAF-Dokumente auflisten und ansehen`

[CSAF2.0 - 9.1.6]

- `checkout old versions of a CSAF document`
- `show all differences between versions of a CSAF document`

### Ansehen des Status von CSAF-Dokument

[Leistungbeschr 2.2]

- `Registered darf alle CSAF-Dokumente im Status „published“ auflisten und ansehen`
- `Author darf den Status eigener CSAF-Dokumente einsehen`
- `Editor darf den Status aller CSAF-Dokumente einsehen`

### Auflisten/Ansehen der Statusänderungen von CSAF-Dokument

[Leistungbeschr 2.2]

`Auditor kann alle Statusänderungen zu CSAF-Dokumenten auflisten und ansehen`

### Auflisten/Ansehen der Kommentare und Antworten von CSAF-Dokument

[Leistungbeschr 2.2]

- `Author darf Kommentare zu CSAF-Dokumente, die er ansehen und bearbeiten darf,
 sehen und beantworten`
- `Reviewer darf Kommentare zu CSAF-Dokumenten sehen und erstellen`
  `Auditor kann alle Kommentare und Antworten zu CSAF-Dokumenten auflisten und ansehen`

## Suche in bestehenden CSAF-Dokumenten

[CSAF2.0 - 9.1.6]

- `search for CSAF documents by values of required fields at document-level or
  their children within the system`
- `search for CSAF documents by values of cve within the system`
- `search for CSAF documents based on properties of product_tree`
- **filter on all properties which it is required to search for**
- **provide an API to retrieve all CSAF documents which are currently in the
 status published.**

## Löschen von CSAF-Dokumenten

[Leistungbeschr 2.2]

- `Author darf eigene (von dem Nutzer) CSAF-Dokumente im Status „Draft“
 auflisten, ansehen, bearbeiten und löschen.`
- `Editor darf alle Advisories im Status „Draft“ auflisten, ansehen, bearbeiten
 und löschen`
- `Manager darf alle Advisories (unabhängig vom Status) löschen.`

[CSAF2.0 - 9.1.6]

- `delete CSAF documents from the system`

## Erstellen und Editieren von CSAF-Dokumenten

[Leistungbeschr 2.2]

- `Author darf neue CSAF-Dokumente anlegen`
- `Author darf eigene (von dem Nutzer) CSAF-Dokumente im Status „Draft“
 auflisten, ansehen, bearbeiten und löschen.`
- `Editor darf alle Advisories im Status „Draft“ auflisten, ansehen, bearbeiten
 und löschen`

`Es ist ein Audit Trail für jedes CSAF-Dokument zu führen. Im Audit Trail wird
erfasst, wer wann welche Änderungen am CSAF Dokument vorgenommen hat. Dazu
gehören das Erstellen, Editieren, Statusänderungen, Kommentare und Antworten
darauf, einschließlich der durchführende Nutzer.`

`Es muss die Möglichkeit geben, ein mit konfigurierbaren Werten vorausgefülltes
Template für ein neues Advisory gemäß dem Standard5 abrufen zu können. Die
Konfiguration von vorbelegten Werten in dem Template kann über eine Datei oder
die REST-Schnittstelle dynamisch erfolgen. Die entsprechenden Felder ergeben
sich aus dem Standard. Der Auftragnehmer kann im Rahmen des Konzeptes weitere
Vorschläge für Felder zu machen, die vorausgefüllt werden können. Beim Erstellen
und Editieren sind Versionsnummer gemäß dem Standard und Datumsangaben
automatisch durch das Backend zu befüllen und zu verwalten. Semantic Versioning6
muss unterstützt werden`

[CSAF2.0 - 9.1.6]

- `create new CSAF documents`
- `prefill CSAF documents based on values given in the configuration (see below)`
- `create a new version of an existing CSAF document`

- **identify the latest version of CSAF documents with the same /document/tracking/id**
- **suggest a /document/tracking/id based on the given configuration.**

- **track of the version of CSAF documents automatically and increment
 according to the versioning scheme (see also subsections of 3.1.11)
 selected in the configuration.**
- **check that the document version is set correctly based on the changes in
 comparison to the previous version (see also subsections of 3.1.11).**
- **suggest to use the document status interim if a CSAF document is updated
 more frequent than the given threshold in the configuration (default: 3 weeks)**
- **suggest to publish a new version of the CSAF document with the document
 status final if the document status was interim and no new release has be
 done during the the given threshold in the configuration (default: 6 weeks)**
- support the following workflows:
- `"New Advisory": create a new advisory, request a review, provide review
 comments or approve it, resolve review comments; if the review approved it,
 the approval for publication can be requested; if granted the document status
 changes to final (or  ìnterim based on the selection in approval or
 configuration) and the advisory is provided for publication (manual or timebased)`
- `"Update Advisory": open an existing advisory, create new revision & change
 content, request a review, provide review comments or approve it, resolve
 review comments; if the review approved it, the approval for publication can
 be requested; if  granted the document status changes to final (or ìnterim
 based on the selection in approval or configuration) and the  advisory is
 provided for publication (manual or time-based)`

- **handling of date/time and version is automated.**

- `prefills the following fields in new CSAF documents with the values given
 below or based on the templates from configuration`
  - `/document/csaf_version with the value 2.0`
  - `/document/language`
  - `/document/notes`
    - `legal_disclaimer (Terms of use from the configuration)`
    - `general (General Security recommendations from the configuration)`
  - `/document/tracking/current_release_date with the current date`
  - `/document/tracking/generator and children`
  - `/document/tracking/initial_release_date with the current date`
  - `/document/tracking/revision_history`
    - `date with the current date`
    - `number (based on the templates according to the versioning scheme configured)`
    - `summary (based on the templates from configuration; default: "Initial version.")`
  - `/document/tracking/status with draft`
  - `/document/tracking/version with the value of number the latest
   /document/tracking/revision_history[] element`
  - `/document/publisher and children`
  - `/document/category (based on the templates from configuration)`

- When updating an existing CSAF document
  - prefills all fields which have be present in the existing CSAF document
  - adds a new item in /document/tracking/revision_history[]
  - updates the following fields with the values given below or based on the
   templates from configuration:
    - /document/csaf_version with the value 2.0
    - /document/language
    - /document/notes
      - legal_disclaimer (Terms of use from the configuration)
      - general (General Security recommendations from the configuration)
  - /document/tracking/current_release_date with the current date
  - /document/tracking/generator and children
  - the new item in /document/tracking/revision_history[]
    - date with the current date
    - number (based on the templates according to the versioning scheme configured)
  - /document/tracking/status with draft
  - /document/tracking/version with the value of number the latest
   /document/tracking/revision_history[] element
  - /document/publisher and children

### Status Ändern von CSAF-Dokumenten

[Leistungbeschr 2.2]

- `Author darf den Status eigener CSAF-Dokument von „Draft“ auf „Review“ ändern`
- `Editor darf den Status aller CSAF-Dokument von „Draft“ auf „Review“ ändern`
- `Publisher darf den Status aller CSAF-Dokument von „Approved“ auf „Published“
 ändern (auch zeitbasiert).`
- `Reviewer darf den Status von „Review“ auf „Draft“ oder „approved“ ändern`

[CSAF2.0 - 9.1.6]

- `review CSAF documents in the system`
- `approve CSAF documents`
- `publication may be immediately or at a given date/time.`

## Kommentieren von CSAF-Dokumenten

[Leistungbeschr 2.2]

`Es muss die Möglichkeit geben, für ein CSAF-Dokument Kommentare zu verfassen.
In dem Kommentar muss mindestens der Nutzer, der diesen erstellt hat, der
Zeitpunkt und ein Freitext enthalten sein. Ein Kommentar kann allgemein zum
CSAF-Dokument sein oder sich auf eine Zeile oder einen Bereich eines
CSAFDokuments beziehen. Es muss die Möglichkeit geben, auf den Kommentar zu antworten`

`Author darf Kommentare zu CSAF-Dokumente, die er ansehen und bearbeiten darf,
sehen und beantworten`
`Reviewer darf Kommentare zu CSAF-Dokumenten sehen und erstellen`

## Export von CSAF-Dokumenten

[Leistungbeschr 2.2]

`Für den Export müssen die folgenden Transformationen zur Verfügung stehen.
Diese erlauben es, das CSAF Dokument in verschiedene
Formate umzuwandeln. Als Konfigurationsoption steht mindestens, das Firmenlogo
zur Verfügung und ein Template. Der AN muss ein Template erstellen.7 Das
Template muss nach Bedarf angepasst werden können. Hierzu ist eine ausführliche
Dokumentation erforderlich. CSAF-Dokumente werden in ein bestimmtes
Ausgabeformat umgewandelt. Folgende Module sind zu implementieren:`

- `HTML-Konverter: Dieses Modul wandelt die Daten aus dem CSAF Dokument in eine
 HTML Seite um. Es kann genutzt werden, um den Preview zu erzeugen.`
- `PDF-Konverter: Das Modul wandelt die Daten aus dem CSAF Dokument in ein
 PDF-Dokument um.`
- `Markdown-Konverter: Das Modul wandelt die Daten aus dem CSAF Dokument in ein
 Markdown- Dokument um.`

[CSAF2.0 - 9.1.6]

- `export of CSAF documents`

## Import von CSAF Dokumenten

[CSAF2.0 - 9.1.6]

- **should provide an API to import or create new advisories from outside
 systems (e.g. bug tracker, CVD platform,...).**

## Adminstrative Aufgaben

### Nutzer/Rollen anlegen

[Leistungbeschr 2.2]

- `Adminstrator darf Nutzer, Rollen anlegen`

[CSAF2.0 - 9.1.6]

- **provide a user management and support at least the following roles:**
- `Registered: Able to see all published CSAF documents (but only in the
  published version).`
- `Author: inherits Registered permissions and also can Create and Edit Own
 (mostly used for automated creation, see above)`
- `Editor: inherits Author permissions and can Edit (mostly used in PSIRT)`
- `Publisher: inherits Editor permissions and can Change state and Review any
 (mostly used as HEAD of PSIRT or team lead)`
- `Reviewer: inherits Registered permissions and can Review advisories assigned
 to him (might be a subject matter expert or management)`
- `Manager: inherits Publisher permissions and can Delete; User management up
 to Publisher`
- `Administrator: inherits Manager permissions and can Change the configuration`

- **may use groups to support client separation (multitenancy) and therefore
 restrict the roles to actions within their group. In this case, there must
 be a Group configurator which is able to change the values which are used to
 prefill fields in new advisories for that group. Hemight also do the user
 management for the group up to a configured level.**

### Einstellungen setzen konfigurieren

[Leistungbeschr 2.2]

- `Adminstrator darf Einstellungen setzen konfigurieren`

### Template ändern

[Leistungbeschr 2.2]

- `Adminstrator darf das Template ändern.`

### User Management durchführen

[Leistungbeschr 2.2]

- `Manager darf das User Management (einschließlich ändern der Besitzerschaft
  von CSAF-Dokumenten) bis einschließlich der Rolle „Publisher“ durchführen`
- `Adminstrator Manager darf das User Management (einschließlich ändern der
  Besitzerschaft von CSAF-Dokumenten) für alle Rollen durchführen`

## Anbindung des Validator

[Leistungbeschr 2.3.2]

`Arbeitspaket 3.2 (OPTIONAL): Anbindung an das CSAFBackend und Secvisogram
Es ist eine Anbindung an das in AP 2 entwickelte CSAF-Backend zu implementieren
und dokumentieren, um CSAF-Dokumente zu prüfen und sicherzustellen, dass nur
gültige CSAF-Dokumente in den Status veröffentlicht gesetzt werden können.
Insbesondere ist zu dokumentieren, wie beide Services miteinander interagieren
und wie diese Interaktion konfiguriert und aufgesetzt werden kann. Dafür ist ein
entsprechender Prüfschritt in den implementierten Workflows zu ergänzen. Analog
zu AP 2.3 muss ein Button „Validate“ im Server Mode ergänzt werden,
der das JSON-Dokument an den in AP 3.1 implementierten Service übermittelt und
das Ergebnis der Prüfung in Secvisogram darstellt.`

## Erweiterung zum ‚CSAF full validator‘ als REST-Service

[Leistungbeschr 3.3]

In diesem Arbeitspaket erstellt der AN einen „CSAF full validator“ gemäß der
Spezifikation9 als REST-Service. Als Grundlage dafür dient der „CSAF extended
validator“ aus AP 3.1. Der Service muss Aufrufe bereitstellen, die es
ermöglichen:

- jeden implementierten Test einzeln aufzurufen
- alle verpflichtenden Tests zusammen aufzurufen („CSAF basic validator“)
- alle optionalen Tests zusammen aufzurufen
- alle verpflichtenden und optionalen Tests zusammen aufzurufen („CSAF extended
  validator“) alle informativen Tests zusammen aufzurufen
- alle verpflichtenden, optionalen und informativen Test zusammen aufzurufen (
  „CSAF full validator“)

- Dokumentation, Tests und Anbindung aus AP 3.1 sind entsprechend zu
  aktualisieren. Der Rechtschreibtest darf fehlschlagen, wenn die entsprechende
  Sprache auf dem System nicht zur Verfügung steht. Es ist explizit zu
  dokumentieren, wie zusätzliche Sprachen neben amerikanischem Englisch geladen
  werden können.
