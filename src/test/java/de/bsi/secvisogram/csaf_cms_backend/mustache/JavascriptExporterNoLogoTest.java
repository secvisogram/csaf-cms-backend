package de.bsi.secvisogram.csaf_cms_backend.mustache;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToCompressingWhiteSpace;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(properties = "csaf.document.templates.companyLogoPath=")
@ExtendWith(SpringExtension.class)
class JavascriptExporterNoLogoTest {

    private static final String json = """
            {
              "document": {
                "category": "generic_csaf",
                "csaf_version": "2.0",
                "publisher": {
                  "category": "coordinator",
                  "name": "exccellent",
                  "namespace": "https://exccellent.de"
                },
                "title": "TestRSc",
                "tracking": {
                  "current_release_date": "2022-01-11T11:00:00.000Z",
                  "id": "exxcellent-2021AB123",
                  "initial_release_date": "2022-01-12T11:00:00.000Z",
                  "revision_history": [
                    {
                      "date": "2022-01-12T11:00:00.000Z",
                      "number": "0.0.1",
                      "summary": "Test rsvSummary"
                    }
                  ],
                  "status": "draft",
                  "version": "0.0.1",
                  "generator": {
                    "date": "2022-01-11T04:07:27.246Z",
                    "engine": {
                      "version": "1.10.0",
                      "name": "Secvisogram"
                    }
                  }
                },
                "acknowledgments": [
                  {
                    "names": [
                      "Rainer",
                      "Gregor",
                      "Timo"
                    ],
                    "organization": "exxcellent contribute",
                    "summary": "Summary 1234",
                    "urls": [
                      "https://exccellent.de",
                      "https:/heise.de"
                    ]
                  }
                ]
              }
            }
            """;

    private static final String resultHtmlNoLogo =
            """
<!DOCTYPE html>
<html lang="en">

<head>
  <link rel="stylesheet" href="https://unpkg.com/gutenberg-css" charset="utf-8">
  <link rel="stylesheet" href="https://unpkg.com/gutenberg-css/dist/themes/modern.min.css" charset="utf-8">
  <meta charset="utf-8" />
  <style>
    @page {
        size: A4;
    }

    table {
      text-align: left;
    }

    h1 {
      font-size: 2rem;
    }

    h2 {
      font-size: 1.7rem;
    }

    h3 {
      font-size: 1.5rem;
    }

    h4 {
      font-size: 1.35rem;
    }

    h5 {
      font-size: 1.2rem;
    }

    h6 {
      font-size: 1.1rem;
    }

    .logo {
        display: inline-block;
        max-height: 40mm;
        margin-bottom: 20mm;
    }

    .tlp {
        display: table;
        background-color: #000000 !important;
        margin: 0 auto 30px auto;
        padding: 0 3px;
    }

    .RED {
        color: #ff0033;
    }

    .AMBER {
        color: #ffc000;
    }

    .GREEN {
        color: #33ff00;
    }

    .WHITE {
        color: #ffffff;
    }
  </style>
</head>

<body>


  <h1>exxcellent-2021AB123: TestRSc</h1>

  <table>
    <tr>
      <td>Publisher: exccellent</td>
      <td>Document category: generic_csaf</td>
    </tr>
    <tr>
      <td>Initial release date: 2022-01-12T11:00:00.000Z</td>
      <td>Engine: Secvisogram 1.10.0</td>
    </tr>
    <tr>
      <td>Current release date: 2022-01-11T11:00:00.000Z</td>
      <td>Build Date: 2022-01-11T04:07:27.246Z</td>
    </tr>
    <tr>
      <td>Current version: 0.0.1</td>
      <td>Status: draft</td>
    </tr>
    <tr>
      <td>CVSSv3.1 Base Score: 0</td>
      <td>Severity:
        
         
      </td>
    </tr>
    <tr>
      <td>Original language: </td>
      <td>Language: </td>
    </tr>
    <tr>
      <td colspan="2">Also referred to: </td>
    </tr>
  </table>



  <h2>Vulnerabilities</h2>

  <h2>Acknowledgments</h2>
  exccellent thanks the following parties for their efforts:
  <ul>

      <li>Rainer, Gregor, Timo from exxcellent contribute  for Summary 1234 (see: 
  <a >https:&#x2F;&#x2F;exccellent.de</a>
, 
  <a >https:&#x2F;heise.de</a>
)</li>
  </ul>


    <h2>exccellent</h2>
    <p>Namespace: https:&#x2F;&#x2F;exccellent.de</p>
    <p></p>
    <p></p>


  <h2>Revision history</h2>
  <table>
    <thead>
      <tr>
        <th>Version</th>
        <th>Date of the revision</th>
        <th>Summary of the revision</th>
      </tr>
    </thead>
    <tbody>
        <tr>
          <td>0.0.1</td>
          <td>2022-01-12T11:00:00.000Z</td>
          <td>Test rsvSummary</td>
        </tr>
    </tbody>
  </table>



</body>

</html>
                    """;

    @Autowired
    private JavascriptExporter javascriptExporter;


    @Test
    void createHtml() throws IOException {
        String html = this.javascriptExporter.createHtml(json);
        assertThat(html, equalToCompressingWhiteSpace(resultHtmlNoLogo));
    }

}