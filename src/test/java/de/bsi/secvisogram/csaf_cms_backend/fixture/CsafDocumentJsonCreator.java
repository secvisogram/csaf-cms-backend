package de.bsi.secvisogram.csaf_cms_backend.fixture;

/**
 * Utility class to create CSAF document json's
 */
public class CsafDocumentJsonCreator {

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
