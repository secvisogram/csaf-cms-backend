package de.bsi.secvisogram.csaf_cms_backend.fixture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bsi.secvisogram.csaf_cms_backend.model.DocumentTrackingStatus;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.CreateAdvisoryRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Utility class to create CSAF document json's
 */
public class CsafDocumentJsonCreator {

  public static CreateAdvisoryRequest csafToRequest(String csafJson) throws IOException {

    final ObjectMapper jacksonMapper = new ObjectMapper();
    var request = new CreateAdvisoryRequest();
    request.setSummary("Test Summary");
    try (final InputStream csafStream = csafToInputstream(csafJson)) {
      JsonNode csafRootNode = jacksonMapper.readValue(csafStream, JsonNode.class);
      request.setCsaf(csafRootNode);
    }
    return request;
  }

  public static InputStream csafToInputstream(String csafJson) {
    return new ByteArrayInputStream(csafJson.getBytes(StandardCharsets.UTF_8));
  }

  public static String csafMinimalValidDoc(DocumentTrackingStatus status, String version) {
    try {
      return """
             {
               "document": {
                 "category": "CSAF Base",
                 "csaf_version": "2.0",
                 "title": "Minimal Valid Doc",
                 "lang": "en",
                 "distribution": {
                   "tlp": {
                     "label": "GREEN"
                   }
                 },
                 "publisher": {
                   "category": "other",
                   "name": "Secvisogram Automated Tester",
                   "namespace": "https://github.com/secvisogram/secvisogram"
                 },
                 "references": [
                   {
                      "category": "self",
                      "summary": "A non-canonical URL",
                      "url": "https://example.test/security/data/csaf/2021/my-thing-_10.json"
                    }
                 ],
                 "tracking": {
                   "current_release_date": "2022-09-08T12:33:45.678Z",
                   "id": "My-Thing-.10-%s",
                   "initial_release_date": "2022-09-08T12:33:45.678Z",
                   "revision_history": [
                      {
                        "number": "%s",
                        "date": "2022-09-08T12:33:45.678Z",
                        "summary": "initial draft"
                      }
                   ],
                   "status": "%s",
                   "version": "%s"
                 }
               }
             }
             """.formatted(SecureRandom.getInstanceStrong().nextInt(10000), version, status.getCsafValue(), version);
    } catch (NoSuchAlgorithmException e) {
      return "";
    }
  }

  public static String csafJsonCategoryTitleId(String category, String documentTitle, String documentTrackingId) {

    return """
           {
             "document": {
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
           { 
             "document": {
               "category": "Category1",
               "title": "%s"
             }
           }""".formatted(documentTitle);
  }

  public static String csafJsonTitleReleaseDate(String documentTitle, String releaseDate) {

    return """
           { 
             "document": {
               "category": "Category1",
               "title": "%s",
               "tracking": {
                 "current_release_date": "%s"
               }
             }
           }""".formatted(documentTitle, releaseDate);
  }

  public static String csafJsonCategoryTitle(String documentCategory, String documentTitle) {

    return """
           { 
             "document": {
               "category": "%s",
               "title":"%s"
             }
           }""".formatted(documentCategory, documentTitle);
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
                   "organization": "contributing organization",
                   "summary": "Summary 1234",
                   "urls": [
                     "https://test-vendor.test",
                     "https://test-vendor-2.test"
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
                   "name": "Test CSAF Product",
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
                   "name": "Test vendor",
                   "product": {
                     "product_id": "CSAFPID-0002",
                     "name": "Test vendor"
                   }
                 }
               ]
             }
           }""".formatted(category);
  }
}
