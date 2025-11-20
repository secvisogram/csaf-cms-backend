package de.bsi.secvisogram.csaf_cms_backend.task;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.bsi.secvisogram.csaf_cms_backend.config.CsafAutoPublishConfiguration;
import de.bsi.secvisogram.csaf_cms_backend.config.CsafConfiguration;
import de.bsi.secvisogram.csaf_cms_backend.model.DocumentTrackingStatus;
import de.bsi.secvisogram.csaf_cms_backend.model.ExportFormat;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryService;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.nio.charset.StandardCharsets;
import reactor.core.publisher.Mono;

@SuppressWarnings({"unchecked"})
public class PublishJobTest {

  @Test
  void fromFile_containsCsafAndTlpParts() throws Exception {
    PublishJob job = new PublishJob();
    Path tmp = Files.createTempFile("csaf-test", ".json");
    MultiValueMap<String, HttpEntity<?>> map = (MultiValueMap<String, HttpEntity<?>>) ReflectionTestUtils.invokeMethod(job, "fromFile", tmp, "TRACK1");
    assertNotNull(map);
    assertTrue(map.containsKey("csaf"));
    assertTrue(map.containsKey("tlp"));
  }

  @Test
  void createWebClient_and_getAuthenticationCode_and_publishJob_happyPath() throws Exception {
    // prepare PublishJob with mocked AdvisoryService and configuration
    PublishJob job = new PublishJob();
    AdvisoryService advisoryService = mock(AdvisoryService.class);
    CsafConfiguration cfg = new CsafConfiguration();
    CsafAutoPublishConfiguration ap = new CsafAutoPublishConfiguration().setEnableInsecureTLS(false)
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

    // mock WebClient static builder to return a mock chain that returns Mono.just("ok")
    WebClient mockWebClient = mock(WebClient.class);
    WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
    WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
    WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
    WebClient.ResponseSpec respSpec = mock(WebClient.ResponseSpec.class);

    when(mockWebClient.post()).thenReturn(uriSpec);
    when(uriSpec.uri(anyString())).thenReturn(bodySpec);
    when(bodySpec.contentType(any(MediaType.class))).thenReturn(bodySpec);
    when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
    when(bodySpec.body(any(BodyInserters.FormInserter.class))).thenReturn(headersSpec);
    when(headersSpec.retrieve()).thenReturn(respSpec);
    when(respSpec.bodyToMono(String.class)).thenReturn(Mono.just("ok"));

    WebClient.Builder builder = mock(WebClient.Builder.class);
    when(builder.clientConnector(any())).thenReturn(builder);
    when(builder.build()).thenReturn(mockWebClient);

    try (MockedStatic<WebClient> ws = Mockito.mockStatic(WebClient.class)) {
      ws.when(WebClient::builder).thenReturn(builder);

      // call publishJob - should exercise createWebClient via the mocked builder
      job.publishJob();
    }

    // verify that workflow state change was requested
    verify(advisoryService).changeAdvisoryWorkflowState(eq("adv-1"), eq("rev-1"), eq(WorkflowState.Published), anyString(), eq(DocumentTrackingStatus.Final));

    // getAuthenticationCode: ensure encoded password matches
    String encoded = (String) ReflectionTestUtils.invokeMethod(job, "getAuthenticationCode");
    assertNotNull(encoded);
    org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    assertTrue(encoder.matches("secret", encoded));
  }

  @Test
  void run_swallowsExceptions() throws Exception {
    PublishJob job = spy(new PublishJob());
    doThrow(new CsafException("fail", null)).when(job).publishJob();
    // should not throw
    job.run();
  }

  @Test
  void publishJob_whenWebClientReturnsError_skipsWorkflowChange() throws Exception {
    PublishJob job = new PublishJob();
    AdvisoryService advisoryService = mock(AdvisoryService.class);
    CsafConfiguration cfg = new CsafConfiguration();
    CsafAutoPublishConfiguration ap = new CsafAutoPublishConfiguration().setEnableInsecureTLS(false)
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

    when(advisoryService.getAdvisoryInformations(""))
        .thenReturn(List.of(adv));

    Path tmp = Files.createTempFile("adv2", ".json");
    when(advisoryService.exportAdvisoryForAutoPublish("adv-2")).thenReturn(tmp);

    // prepare failing WebClient chain
    WebClient mockWebClient = mock(WebClient.class);
    WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
    WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
    WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
    WebClient.ResponseSpec respSpec = mock(WebClient.ResponseSpec.class);

    when(mockWebClient.post()).thenReturn(uriSpec);
    when(uriSpec.uri(anyString())).thenReturn(bodySpec);
    when(bodySpec.contentType(any(MediaType.class))).thenReturn(bodySpec);
    when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
    when(bodySpec.body(any(BodyInserters.FormInserter.class))).thenReturn(headersSpec);
    when(headersSpec.retrieve()).thenReturn(respSpec);
    when(respSpec.bodyToMono(String.class)).thenReturn(Mono.error(
        WebClientResponseException.create(500, "err", null, "errbody".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
    ));

    WebClient.Builder builder = mock(WebClient.Builder.class);
    when(builder.clientConnector(any())).thenReturn(builder);
    when(builder.build()).thenReturn(mockWebClient);

    try (MockedStatic<WebClient> ws = Mockito.mockStatic(WebClient.class)) {
      ws.when(WebClient::builder).thenReturn(builder);
      job.publishJob();
    }

    // verify that changeAdvisoryWorkflowState was NOT called due to PublisherException
    verify(advisoryService, never()).changeAdvisoryWorkflowState(anyString(), anyString(), any(), anyString(), any());
  }
}
