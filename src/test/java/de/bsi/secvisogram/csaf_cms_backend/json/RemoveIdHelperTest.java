package de.bsi.secvisogram.csaf_cms_backend.json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class RemoveIdHelperTest {

    public static final String csaf = """
         {
             "document": {
                "nodeId": "1"
             }
          }
        """;

    public static final String csafWithIds = """
         {
         "document": {
            "nodeId": "1",
            "aggregate_severity": {
              "nodeId": "2",
              "text": "Moderate"
            },
            "category": "csaf_security_advisory",
            "csaf_version": "2.0",
            "distribution": {
              "tlp": {
                "label": "WHITE",
                "url": "https://www.first.org/tlp/",
                "nodeId": "3"
              }
            },
            "lang": "en-US",
            "publisher": {
              "category": "coordinator",
              "nodeId": "4",
              "name": "Bundesamt für Sicherheit in der Informationstechnik"
            },
            "title": "CVRF-CSAF-Converter: XML External Entities Vulnerability",
            "tracking": {
              "current_release_date": "2022-03-17T13:03:42.105Z",
              "generator": {
                "date": "2022-03-17T13:09:42.105Z",
                "nodeId": "5",
                "engine": {
                  "name": "Secvisogram",
                  "nodeId": "6",
                  "version": "1.12.1"
                }
              },
              "id": "BSI-2022-0001",
              "initial_release_date": "2022-03-17T13:03:42.105Z",
              "revision_history": [
                {
                  "nodeId": "7",
                  "number": "1"
                }
              ],
              "status": "final",
              "version": "1"
            }
          },
          "product_tree": {
            "nodeId": "8",
            "branches": [
              {
                "branches": [
                  {
                    "branches": [
                      {
                        "category": "product_version",
                        "name": "1.0.0-alpha",
                        "product": {
                          "nodeId": "9",
                          "name": "CSAF Tools CVRF-CSAF-Converter 1.0.0-alpha",
                          "product_id": "CSAFPID-0001",
                          "product_identification_helper": {
                            "cpe": "cpe:/a:csaf-tools:cvrf-csaf-converter:1.0.0-alpha"
                          }
                        }
                      },
                      {
                        "category": "product_version",
                        "name": "1.0.0-dev1",
                        "product": {
                          "name": "CSAF Tools CVRF-CSAF-Converter 1.0.0-dev1",
                          "product_id": "CSAFPID-0002",
                          "product_identification_helper": {
                          "nodeId": "10",
                            "cpe": "cpe:/a:csaf-tools:cvrf-csaf-converter:1.0.0-dev1"
                          }
                        }
                      }
                     ],
                    "category": "product_name",
                    "nodeId": "11",
                    "name": "CVRF-CSAF-Converter"
                  }
                ],
                "category": "vendor",
                "name": "CSAF Tools"
              }
            ]
          },
          "vulnerabilities": [
            {
              "acknowledgments": [
                {
                  "nodeId": "12",
                  "names": [
                    "Damian Pfammatter"
                  ],
                  "organization": "Cyber-Defense Campus",
                  "summary": "Finding and reporting the vulnerability"
                }
              ],
              "cve": "CVE-2022-27193",
              "cwe": {
                "id": "CWE-611",
                "name": "Improper Restriction of XML External Entity Reference"
              },
              "ids": [
                {
                  "nodeId": "12",
                  "system_name": "Github Issue",
                  "text": "csaf-tools/CVRF-CSAF-Converter#78"
                }
              ],
              "notes": [
                {
                  "nodeId": "14",
                  "category": "description",
                  "text": "CSAF Tools CVRF-CSAF-Converter 1.0.0-rc1 resolves XML External Entities (XXE). This leads to the inclusion of arbitrary (local) file content into the generated output document. An attacker can exploit this to disclose information from the system running the converter.",
                  "title": "Vulnerability description"
                }
              ],
              "product_status": {
                "first_fixed": [
                  "CSAFPID-0006"
                ],
                "fixed": [
                  "CSAFPID-0006"
                ],
                "known_affected": [
                  "CSAFPID-0001",
                  "CSAFPID-0002",
                  "CSAFPID-0003",
                  "CSAFPID-0004",
                  "CSAFPID-0005"
                ]
              },
              "remediations": [
                {
                  "nodeId": "15",
                  "category": "vendor_fix",
                  "date": "2022-03-14T13:10:55.000+01:00",
                  "details": "Update to the latest version of the product. At least version 1.0.0-rc2",
                  "product_ids": [
                    "CSAFPID-0001",
                    "CSAFPID-0002",
                    "CSAFPID-0003",
                    "CSAFPID-0004",
                    "CSAFPID-0005"
                  ],
                  "url": "https://github.com/csaf-tools/CVRF-CSAF-Converter/releases/tag/1.0.0-rc2"
                }
              ],
              "scores": [
                {
                  "cvss_v3": {
                    "nodeId": "16",
                    "attackComplexity": "LOW",
                    "attackVector": "LOCAL",
                    "availabilityImpact": "LOW",
                    "userInteraction": "REQUIRED",
                    "vectorString": "CVSS:3.1/AV:L/AC:L/PR:N/UI:R/S:U/C:H/I:N/A:L/E:F/RL:O/RC:C",
                    "version": "3.1"
                  },
                  "products": [
                    "CSAFPID-0001",
                    "CSAFPID-0002",
                    "CSAFPID-0003",
                    "CSAFPID-0004",
                    "CSAFPID-0005"
                  ]
                }
              ]
            }
          ]
        }      
       """;

    public static final String casfWithoutIds = """
            
         {
         "document": {
            "aggregate_severity": {
              "text": "Moderate"
            },
            "category": "csaf_security_advisory",
            "csaf_version": "2.0",
            "distribution": {
              "tlp": {
                "label": "WHITE",
                "url": "https://www.first.org/tlp/"
              }
            },
            "lang": "en-US",
            "publisher": {
              "category": "coordinator",
              "name": "Bundesamt für Sicherheit in der Informationstechnik"
            },
            "title": "CVRF-CSAF-Converter: XML External Entities Vulnerability",
            "tracking": {
              "current_release_date": "2022-03-17T13:03:42.105Z",
              "generator": {
                "date": "2022-03-17T13:09:42.105Z",
                "engine": {
                  "name": "Secvisogram",
                  "version": "1.12.1"
                }
              },
              "id": "BSI-2022-0001",
              "initial_release_date": "2022-03-17T13:03:42.105Z",
              "revision_history": [
                {
                  "number": "1"
                }
              ],
              "status": "final",
              "version": "1"
            }
          },
          "product_tree": {
            "branches": [
              {
                "branches": [
                  {
                    "branches": [
                      {
                        "category": "product_version",
                        "name": "1.0.0-alpha",
                        "product": {
                          "name": "CSAF Tools CVRF-CSAF-Converter 1.0.0-alpha",
                          "product_id": "CSAFPID-0001",
                          "product_identification_helper": {
                            "cpe": "cpe:/a:csaf-tools:cvrf-csaf-converter:1.0.0-alpha"
                          }
                        }
                      },
                      {
                        "category": "product_version",
                        "name": "1.0.0-dev1",
                        "product": {
                          "name": "CSAF Tools CVRF-CSAF-Converter 1.0.0-dev1",
                          "product_id": "CSAFPID-0002",
                          "product_identification_helper": {
                            "cpe": "cpe:/a:csaf-tools:cvrf-csaf-converter:1.0.0-dev1"
                          }
                        }
                      }
                     ],
                    "category": "product_name",
                    "name": "CVRF-CSAF-Converter"
                  }
                ],
                "category": "vendor",
                "name": "CSAF Tools"
              }
            ]
          },
          "vulnerabilities": [
            {
              "acknowledgments": [
                {
                  "names": [
                    "Damian Pfammatter"
                  ],
                  "organization": "Cyber-Defense Campus",
                  "summary": "Finding and reporting the vulnerability"
                }
              ],
              "cve": "CVE-2022-27193",
              "cwe": {
                "id": "CWE-611",
                "name": "Improper Restriction of XML External Entity Reference"
              },
              "ids": [
                {
                  "system_name": "Github Issue",
                  "text": "csaf-tools/CVRF-CSAF-Converter#78"
                }
              ],
              "notes": [
                {
                  "category": "description",
                  "text": "CSAF Tools CVRF-CSAF-Converter 1.0.0-rc1 resolves XML External Entities (XXE). This leads to the inclusion of arbitrary (local) file content into the generated output document. An attacker can exploit this to disclose information from the system running the converter.",
                  "title": "Vulnerability description"
                }
              ],
              "product_status": {
                "first_fixed": [
                  "CSAFPID-0006"
                ],
                "fixed": [
                  "CSAFPID-0006"
                ],
                "known_affected": [
                  "CSAFPID-0001",
                  "CSAFPID-0002",
                  "CSAFPID-0003",
                  "CSAFPID-0004",
                  "CSAFPID-0005"
                ]
              },
              "remediations": [
                {
                  "category": "vendor_fix",
                  "date": "2022-03-14T13:10:55.000+01:00",
                  "details": "Update to the latest version of the product. At least version 1.0.0-rc2",
                  "product_ids": [
                    "CSAFPID-0001",
                    "CSAFPID-0002",
                    "CSAFPID-0003",
                    "CSAFPID-0004",
                    "CSAFPID-0005"
                  ],
                  "url": "https://github.com/csaf-tools/CVRF-CSAF-Converter/releases/tag/1.0.0-rc2"
                }
              ],
              "scores": [
                {
                  "cvss_v3": {
                    "attackComplexity": "LOW",
                    "attackVector": "LOCAL",
                    "availabilityImpact": "LOW",
                    "userInteraction": "REQUIRED",
                    "vectorString": "CVSS:3.1/AV:L/AC:L/PR:N/UI:R/S:U/C:H/I:N/A:L/E:F/RL:O/RC:C",
                    "version": "3.1"
                  },
                  "products": [
                    "CSAFPID-0001",
                    "CSAFPID-0002",
                    "CSAFPID-0003",
                    "CSAFPID-0004",
                    "CSAFPID-0005"
                  ]
                }
              ]
            }
          ]
        }        
       """;

    @Test
    public void removeIds() throws IOException {

        var advisoryDbString = """
                {   "owner": "Musterfrau",
                    "type": "Advisory",
                    "workflowState": "Draft",
                    "csaf": %s,
                    "_rev": "reavison",
                    "_id": "id124214"}""".formatted(csafWithIds);

        var advisoryStream = new ByteArrayInputStream(advisoryDbString.getBytes(StandardCharsets.UTF_8));
        var advisory = AdvisoryWrapper.createFromCouchDb(advisoryStream);
        RemoveIdHelper.removeIds(advisory.getCsaf(), "nodeId");
        assertThat(advisory.getCsaf().toString().replaceAll("\\s+", ""), is(casfWithoutIds.replaceAll("\\s+", "")));
    }
}
