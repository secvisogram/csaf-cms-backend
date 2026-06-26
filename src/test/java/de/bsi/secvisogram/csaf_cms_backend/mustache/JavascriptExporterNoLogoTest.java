package de.bsi.secvisogram.csaf_cms_backend.mustache;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StringUtils;

@SpringBootTest(properties = "csaf.document.templates.companyLogoPath=")
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
  <meta charset="utf-8" />
    <style>
      <!--
      /* File from https://github.com/BafS/Gutenberg/blob/master/scss/themes/modern.scss */
      /*! * Gutenberg * * MIT Fabien Sa * https://github.com/BafS/Gutenberg */
      -->
      h1,
      h2,
      h3,
      h4,
      h5,
      h6 {
          font-family: 'Montserrat', 'Arial Black', 'Arial Bold', 'Helvetica Neue', Helvetica, sans-serif;
      }

      body {
          font-family: 'Open Sans', 'Helvetica Neue', Helvetica, arial, sans-serif;
          text-align: justify;
      }

      h1 {
          font-weight: 700;
          letter-spacing: -1px;
          text-align: center;
      }

      h2 {
          letter-spacing: -1px;
      }

      h2,
      h3,
      h4,
      h5 {
          color: #262626;
      }

      pre,
      code {
          border: 0;
      }

      pre,
      code,
      blockquote {
          background: #f8f8f9;
      }

      blockquote {
          margin-left: 1.5rem;
          margin-right: 1.5rem;
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
  <a href="https:&#x2F;&#x2F;exccellent.de">https:&#x2F;&#x2F;exccellent.de</a>
, 
  <a href="https:&#x2F;heise.de">https:&#x2F;heise.de</a>
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
        Assertions.assertEquals(StringUtils.trimAllWhitespace(html), StringUtils.trimAllWhitespace(resultHtmlNoLogo));
    }

}