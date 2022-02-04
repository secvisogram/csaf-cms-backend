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

## Abfrage von CSAF - Daten

## Auflisten/Ansehen von CSAF-Dokumente

[Leistungbeschr 2.2] `Die Liste der CSAF-Dokument muss entsprechend der
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

### Auflisten/Ansehen des Audits-Trails von CSAF-Dokumente

[Leistungbeschr 2.2]

- `Auditor darf den Audit-Trail zu allen CSAF-Dokumenten auflisten und ansehen`

`Es ist ein Audit Trail für jedes CSAF-Dokument zu führen. Im Audit Trail wird
erfasst, wer wann welche Änderungen am CSAF Dokument vorgenommen hat. Dazu
gehören das Erstellen, Editieren, Statusänderungen, Kommentare und Antworten
darauf, einschließlich der durchführende Nutzer

### Auflisten/Ansehen der Versionen von CSAF-Dokumente

[Leistungbeschr 2.2]

- `Auditor kann alle Versionen der CSAF-Dokumente auflisten und ansehen`

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

[csaf2.0]

- list all CSAF documents within the system
- show all differences between versions of a CSAF document
- list all CSAF documents within the system
- search for CSAF documents by values of required fields at document-level or
  their children within the system
- search for CSAF documents by values of cve within the system
- search for CSAF documents based on properties of product_tree
- filter on all properties which it is required to search for
- show an audit log for each CSAF document
- identify the latest version of CSAF documents with the same
  /document/tracking/id
- provide an API to retrieve all CSAF documents which are currently in the
  status published
- provide a user management and support at least the following roles:
- Registered: Able to see all published CSAF documents (but only in the
  published version).

## Löschen von CSAF-Dokumenten

[Leistungbeschr 2.2]

- `Author darf eigene (von dem Nutzer) CSAF-Dokumente im Status „Draft“
 auflisten, ansehen, bearbeiten und löschen.`
- `Editor darf alle Advisories im Status „Draft“ auflisten, ansehen, bearbeiten
 und löschen`
- `Manager darf alle Advisories (unabhängig vom Status) löschen.`

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

### Status Ändern von CSAF-Dokumenten

[Leistungbeschr 2.2]

- `Author darf den Status eigener CSAF-Dokument von „Draft“ auf „Review“ ändern`
- `Editor darf den Status aller CSAF-Dokument von „Draft“ auf „Review“ ändern`
- `Publisher darf den Status aller CSAF-Dokument von „Approved“ auf „Published“
 ändern (auch zeitbasiert).`
- `Reviewer darf den Status von „Review“ auf „Draft“ oder „approved“ ändern`

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
Diese erlauben es de![img.png](img.png)er, das CSAF Dokument in verschiedene
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

## Adminstrative Aufgaben

### Nutzer/Rollen anlegen

[Leistungbeschr 2.2]

- `Adminstrator darf Nutzer, Rollen anlegen`

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
