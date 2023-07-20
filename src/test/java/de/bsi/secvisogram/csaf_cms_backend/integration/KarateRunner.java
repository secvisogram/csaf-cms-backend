package de.bsi.secvisogram.csaf_cms_backend.integration;

import com.intuit.karate.junit5.Karate;


public class KarateRunner {
  @Karate.Test
  Karate testSample() {
    
    String[] testCases = new String[] { 
      //"classpath:de/bsi/secvisogram/csaf_cms_backend/integration/sample.feature",
      "classpath:de/bsi/secvisogram/csaf_cms_backend/integration/fullworkflow.feature"
      //  "classpath:de/bsi/secvisogram/csaf_cms_backend/integration/export.feature"
    };
    return Karate.run(testCases);
  }
}
