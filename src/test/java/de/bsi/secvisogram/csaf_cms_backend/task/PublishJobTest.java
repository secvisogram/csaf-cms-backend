package de.bsi.secvisogram.csaf_cms_backend.task;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.bsi.secvisogram.csaf_cms_backend.config.CsafAutoPublishConfiguration;
import de.bsi.secvisogram.csaf_cms_backend.config.CsafConfiguration;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.model.DocumentTrackingStatus;
import de.bsi.secvisogram.csaf_cms_backend.model.ExportFormat;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@SuppressWarnings({"unchecked"})
public class PublishJobTest {

  @Test
  void buildMultipartBody_containsCsafAndTlpParts() throws Exception {
    PublishJob job = new PublishJob();
    Path tmp = Files.createTempFile("csaf-test", ".json");
    MultiValueMap<String, Object> map = ReflectionTestUtils.invokeMethod(job, "buildMultipartBody", tmp, "track1");
    assertNotNull(map);
    assertTrue(map.containsKey("csaf"));
    assertTrue(map.containsKey("tlp"));
    // csaf part should be wrapped in an HttpEntity with application/json content type
    Object csafPart = map.getFirst("csaf");
    assertInstanceOf(HttpEntity.class, csafPart);
    HttpEntity<?> csafEntity = (HttpEntity<?>) csafPart;
    assertEquals(MediaType.APPLICATION_JSON, csafEntity.getHeaders().getContentType());
    Files.deleteIfExists(tmp);
  }

  @Test
  void publishJob_happyPath() throws Exception {
    // prepare PublishJob with mocked AdvisoryService and configuration
    PublishJob job = new PublishJob();
    AdvisoryService advisoryService = mock(AdvisoryService.class);
    CsafConfiguration cfg = new CsafConfiguration();
    CsafAutoPublishConfiguration ap = new CsafAutoPublishConfiguration()
        .setEnableInsecureTLS(false)
        .setUrl("http://localhost")
        .setPassword("secret")
        .setEnabled(true);
    cfg.setAutoPublish(ap);
    ReflectionTestUtils.setField(job, "advisoryService", advisoryService);
    ReflectionTestUtils.setField(job, "configuration", cfg);

    // prepare advisory to publish
    AdvisoryInformationResponse adv = new AdvisoryInformationResponse("adv-1", WorkflowState.AutoPublish);
    adv.setCurrentReleaseDate(DateTimeFormatter.ISO_INSTANT.format(Instant.now().minusSeconds(60)));
    adv.setAdvisoryId("adv-1");
    adv.setDocumentTrackingId("TRACK1");
    adv.setRevision("rev-1");

    when(advisoryService.getAdvisoryInformations("")).thenReturn(List.of(adv));

    Path tmp = Files.createTempFile("adv", ".json");
    when(advisoryService.exportAdvisory("adv-1", ExportFormat.JSON)).thenReturn(tmp);

    // mock RestClient chain that returns successfully
    RestClient mockRestClient = mock(RestClient.class);
    RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
    RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
    RestClient.ResponseSpec respSpec = mock(RestClient.ResponseSpec.class);

    when(mockRestClient.post()).thenReturn(uriSpec);
    when(uriSpec.uri(anyString())).thenReturn(bodySpec);
    when(bodySpec.contentType(any(MediaType.class))).thenReturn(bodySpec);
    when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
    when(bodySpec.body(any(MultiValueMap.class))).thenReturn(bodySpec);
    when(bodySpec.retrieve()).thenReturn(respSpec);
    when(respSpec.onStatus(any(), any())).thenReturn(respSpec);
    when(respSpec.toBodilessEntity()).thenReturn(null);

    // inject the mock RestClient directly into the job
    ReflectionTestUtils.setField(job, "restClient", mockRestClient);

    job.publishJob();

    // verify that workflow state change was requested
    verify(advisoryService).changeAdvisoryWorkflowState(
        eq("adv-1"), eq("rev-1"), eq(WorkflowState.Published), anyString(), eq(DocumentTrackingStatus.Final));
  }

  @Test
  void getAuthenticationCode_returnsBcryptHashOfPassword() {
    PublishJob job = new PublishJob();
    CsafConfiguration cfg = new CsafConfiguration();
    CsafAutoPublishConfiguration ap = new CsafAutoPublishConfiguration()
        .setPassword("secret");
    cfg.setAutoPublish(ap);
    ReflectionTestUtils.setField(job, "configuration", cfg);

    String encoded = ReflectionTestUtils.invokeMethod(job, "getAuthenticationCode");
    assertNotNull(encoded);
    org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
        new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    assertTrue(encoder.matches("secret", encoded));
  }

  @Test
  void run_swallowsExceptions() throws Exception {
    PublishJob job = spy(new PublishJob());
    doThrow(new CsafException("fail", null)).when(job).publishJob();
    // should not throw
    assertDoesNotThrow(job::run);
  }

  @Test
  void publishJob_whenRestClientReturnsError_skipsWorkflowChange() throws Exception {
    PublishJob job = new PublishJob();
    AdvisoryService advisoryService = mock(AdvisoryService.class);
    CsafConfiguration cfg = new CsafConfiguration();
    CsafAutoPublishConfiguration ap = new CsafAutoPublishConfiguration()
        .setEnableInsecureTLS(false)
        .setUrl("http://localhost")
        .setPassword("secret")
        .setEnabled(true);
    cfg.setAutoPublish(ap);
    ReflectionTestUtils.setField(job, "advisoryService", advisoryService);
    ReflectionTestUtils.setField(job, "configuration", cfg);

    AdvisoryInformationResponse adv = new AdvisoryInformationResponse("adv-2", WorkflowState.AutoPublish);
    adv.setCurrentReleaseDate(DateTimeFormatter.ISO_INSTANT.format(Instant.now().minusSeconds(60)));
    adv.setAdvisoryId("adv-2");
    adv.setDocumentTrackingId("TRACK2");
    adv.setRevision("rev-2");

    when(advisoryService.getAdvisoryInformations("")).thenReturn(List.of(adv));

    Path tmp = Files.createTempFile("adv2", ".json");
    when(advisoryService.exportAdvisory("adv-2", ExportFormat.JSON)).thenReturn(tmp);

    // mock RestClient chain that triggers the onStatus error handler
    RestClient mockRestClient = mock(RestClient.class);
    RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
    RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
    RestClient.ResponseSpec respSpec = mock(RestClient.ResponseSpec.class);

    when(mockRestClient.post()).thenReturn(uriSpec);
    when(uriSpec.uri(anyString())).thenReturn(bodySpec);
    when(bodySpec.contentType(any(MediaType.class))).thenReturn(bodySpec);
    when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
    when(bodySpec.body(any(MultiValueMap.class))).thenReturn(bodySpec);
    when(bodySpec.retrieve()).thenReturn(respSpec);
    // simulate the onStatus handler being invoked and resulting in a RestClientResponseException
    when(respSpec.onStatus(any(), any())).thenReturn(respSpec);
    when(respSpec.toBodilessEntity()).thenThrow(
        new RestClientResponseException("Internal Server Error", 500, "Internal Server Error", new HttpHeaders(), new byte[0], null)
    );

    ReflectionTestUtils.setField(job, "restClient", mockRestClient);

    job.publishJob();

    // verify that changeAdvisoryWorkflowState was NOT called due to the error
    verify(advisoryService, never()).changeAdvisoryWorkflowState(
        anyString(), anyString(), any(), anyString(), any());
  }
}
