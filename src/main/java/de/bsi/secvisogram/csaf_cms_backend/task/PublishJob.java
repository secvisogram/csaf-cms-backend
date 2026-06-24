package de.bsi.secvisogram.csaf_cms_backend.task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import de.bsi.secvisogram.csaf_cms_backend.config.CsafConfiguration;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.DatabaseException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryWrapper;
import de.bsi.secvisogram.csaf_cms_backend.model.DocumentTrackingStatus;
import de.bsi.secvisogram.csaf_cms_backend.model.ExportFormat;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryService;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.netty.http.client.HttpClient;

@Component
public class PublishJob implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(PublishJob.class);

  @Autowired
  private AdvisoryService advisoryService;

  @Autowired
  private CsafConfiguration configuration;

  @Override
  public void run() {
    try {
      this.publishJob();
    } catch (CsafException | IOException | DatabaseException e) {
      LOG.error("There was a problem when publishing advisories", e);
    }
  }

  public void publishJob() throws CsafException, IOException, DatabaseException {
	LOG.info("AutoPublisher started");
    List<AdvisoryInformationResponse> advisoryList = this.advisoryService.getAdvisoryInformations("");
    for (AdvisoryInformationResponse advisory : advisoryList) {
      if (advisory.getWorkflowState() == WorkflowState.AutoPublish) {
        if (AdvisoryWrapper.timestampIsBefore(advisory.getCurrentReleaseDate(),
            DateTimeFormatter.ISO_INSTANT.format(Instant.now()))) {
          Path temporaryAdvisoryFilePath = this.advisoryService.exportAdvisory(advisory.getAdvisoryId(), ExportFormat.JSON);
          String trackingId = advisory.getDocumentTrackingId().toLowerCase();
          
          final WebClient webClient = createWebClient();
          try {
            webClient.post().uri(this.configuration.getAutoPublish().getUrl())
                .contentType(MediaType.MULTIPART_FORM_DATA).header("X-Csaf-Provider-Auth", getAuthenticationCode())
                .body(BodyInserters.fromMultipartData(fromFile(temporaryAdvisoryFilePath, trackingId))).retrieve()
                .bodyToMono(String.class).onErrorMap(throwable -> {
                	if (WebClientResponseException.class.isInstance(throwable)) {
                		WebClientResponseException wcre = (WebClientResponseException) throwable;
                		return new PublisherException(throwable.getMessage() + wcre.getResponseBodyAsString());
                	}
                  return new PublisherException(throwable.getMessage());
                }).block();
          } catch (PublisherException pe) {
            LOG.error(pe.getMessage());
            // Skip workflow state change.
            continue;
          } finally {
            Files.deleteIfExists(temporaryAdvisoryFilePath);
          }
          
          this.advisoryService.changeAdvisoryWorkflowState(advisory.getAdvisoryId(), advisory.getRevision(),
              WorkflowState.Published, advisory.getCurrentReleaseDate(), DocumentTrackingStatus.Final);
        }
      }
    }
  }

  private MultiValueMap<String, HttpEntity<?>> fromFile(Path path, String trackingId) {
    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    String header = String.format("form-data; name=\"%s\"; filename=\"%s.json\"", "csaf", trackingId);
    builder.part("csaf", new FileSystemResource(path)).header("Content-Disposition", header);
    builder.part("tlp", "csaf");
    return builder.build();
  }

  private WebClient createWebClient() throws SSLException {
    HttpClient httpClient = null;
    if (this.configuration.getAutoPublish().isEnableInsecureTLS()) {
      SslContext sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
      httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
    } else {
      httpClient = HttpClient.create();
    }
    return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient)).build();
  }

  private String getAuthenticationCode() {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    return encoder.encode(this.configuration.getAutoPublish().getPassword());
  }
}
