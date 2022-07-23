package de.bsi.secvisogram.csaf_cms_backend.fixture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.CreateAdvisoryRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility class to create CSAF document json's
 */
public class CsafDocumentJsonCreator {

    public static CreateAdvisoryRequest csafToRequest(String csafJson) throws IOException {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        var request = new CreateAdvisoryRequest();
        try (final InputStream csafStream = new ByteArrayInputStream(csafJson.getBytes(StandardCharsets.UTF_8))) {
            JsonNode csafRootNode = jacksonMapper.readValue(csafStream, JsonNode.class);
            request.setCsaf(csafRootNode);
        }
        return request;
    }

    public static String csafJsonCategoryTitleId(String category, String documentTitle, String documentTrackingId) {

        return """
                { "document": {
                      "category": "%s",
                      "title": "%s",
                      "tracking": {
                        "id": "%s"
                      }
                   }
                }""".formatted(category, documentTitle, documentTrackingId);
    }

    public static String csafJsonTitle(String documentTitle) {

        return """
                { "document": {
                      "category": "Category1",
                      "title": "%s"
                   }
                }""".formatted(documentTitle);
    }

    public static String csafJsonTitleReleaseDate(String documentTitle, String releaseDate) {

        return """
                { "document": {
                      "category": "Category1",
                      "title": "%s",
                      "tracking": {
                          "current_release_date": "%s"
                      }
                   }
                }""".formatted(documentTitle, releaseDate);
    }

    public static String csafJsonTitleReleaseDateVersion(String documentTitle, String releaseDate, String version) {

        return """
                { "document": {
                      "category": "Category1",
                      "title": "%s",
                      "tracking": {
                          "current_release_date": "%s",
                          "version":"%s",
                          "status":"draft"
                      }
                   }
                }""".formatted(documentTitle, releaseDate, version);

    }
    public static String csafJsonCategoryTitle(String documentCategory, String documentTitle) {

        return """
                { "document": {
                      "category": "%s",
                      "title":"%s"
                   }
                }""".formatted(documentCategory, documentTitle);
    }


    public static String csafJsonTrackingGenratorVersion(String version) {

        return """
                {
                  "document": {
                      "category": "Category1",
                      "title": "title1",
                      "tracking": {
                          "current_release_date": "2022-03-17T13:03:42.105Z",
                          "generator": {
                              "date": "2022-03-17T13:09:42.105Z",
                              "engine": {
                                  "name": "Secvisogram",
                                  "version": "%s"
                            }
                          }
                      }
                  }
                }""".formatted(version);
    }

    public static String csafJsonRevisionHistorySummary(String summary) {

        return """
                {
                  "document": {
                      "category": "Category1",
                      "title": "title1",
                      "tracking": {
                          "current_release_date": "2022-03-17T13:03:42.105Z",
                          "revision_history": [
                             {
                               "date": "2022-01-12T11:00:00.000Z",
                               "number": "0.0.1",
                               "summary": "%s"
                             }
                          ]
                      }
                  }
                }""".formatted(summary);
    }

    public static String csafAcknowledgmentsNames(String name) {

        return """
                {
                  "document": {
                      "category": "Category1",
                      "title": "title1",
                       "acknowledgments": [
                          {
                            "names": [
                              "%s"
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
                }""".formatted(name);
    }

    public static String csafVulnerabilitiesCve(String cve) {

        return """
                {
                  "document": {
                      "category": "Category1",
                      "title": "title1"
                  },
                  "vulnerabilities": [
                      {
                        "cve": "%s"
                      }
                  ]
                }""".formatted(cve);
    }

    public static String docWithVulnerabilities(String vulnerabilities) {

        return """
                {
                  "document": {
                      "category": "Category1",
                      "title": "title1"
                  },
                  "vulnerabilities": %s
                }""".formatted(vulnerabilities);
    }

    public static String docWithProductTree(String productTree) {

        return """
                {
                  "document": {
                      "category": "Category1",
                      "title": "title1"
                  },
                  "product_tree": %s
                }""".formatted(productTree);
    }


    public static String csafProductTreeFullProductNamesProductId(String productId) {

        return """
                {
                  "document": {
                      "category": "Category1",
                      "title": "title1"
                  },
                  "product_tree": {
                       "full_product_names": [
                         {
                           "product_id": "%s",
                           "name": "Exxcellent CSAF",
                           "product_identification_helper": {
                             "cpe": "cpe"
                           }
                         }
                       ]
                  }
                }""".formatted(productId);
    }

    public static String csafProductTreeBranchesCategory(String category) {

        return """
                {
                  "document": {
                      "category": "Category1",
                      "title": "title1"
                  },
                  "product_tree": {
                      "branches": [
                         {
                           "category": "%s",
                           "name": "Exxcellent",
                           "product": {
                             "product_id": "CSAFPID-0002",
                             "name": "Exxcellent"
                           }
                         }
                     ]
                  }
                }""".formatted(category);
    }

}
