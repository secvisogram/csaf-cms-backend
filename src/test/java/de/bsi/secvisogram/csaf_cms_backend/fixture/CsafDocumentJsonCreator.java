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
}
