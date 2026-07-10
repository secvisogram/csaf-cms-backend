package de.bsi.secvisogram.csaf_cms_backend.task;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import de.bsi.secvisogram.csaf_cms_backend.config.CsafConfiguration;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.DatabaseException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryWrapper;
import de.bsi.secvisogram.csaf_cms_backend.model.DocumentTrackingStatus;
import de.bsi.secvisogram.csaf_cms_backend.model.ExportFormat;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryService;

@Component
public class PublishJob implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(PublishJob.class);

  @Autowired
  private AdvisoryService advisoryService;

  @Autowired
  private CsafConfiguration configuration;

  private RestClient restClient;

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

          try {
            MultiValueMap<String, Object> body = buildMultipartBody(temporaryAdvisoryFilePath, trackingId);
            getRestClient().post()
                .uri(this.configuration.getAutoPublish().getUrl())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("X-Csaf-Provider-Auth", getAuthenticationCode())
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                  String responseBody = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                  throw new PublisherException("HTTP " + res.getStatusCode() + ": " + responseBody);
                })
                .toBodilessEntity();
          } catch (PublisherException pe) {
            LOG.error(pe.getMessage());
            // Skip workflow state change.
            continue;
          } catch (RestClientResponseException re) {
            LOG.error("HTTP error when publishing advisory {}: {} - {}",
                advisory.getAdvisoryId(), re.getStatusCode(), re.getResponseBodyAsString());
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

  private MultiValueMap<String, Object> buildMultipartBody(Path path, String trackingId) {
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    // The gocsaf provider requires the csaf part to have Content-Type: application/json.
    // Wrap the file resource in an HttpEntity to set the per-part content type explicitly.
    HttpHeaders partHeaders = new HttpHeaders();
    partHeaders.setContentType(MediaType.APPLICATION_JSON);
    body.add("csaf", new HttpEntity<>(new NamedFileResource(path, trackingId + ".json"), partHeaders));
    body.add("tlp", "csaf");
    return body;
  }

  private RestClient getRestClient() {
    if (this.restClient == null) {
      HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
      if (this.configuration.getAutoPublish().isEnableInsecureTLS()) {
        try {
          SSLContext sslContext = SSLContext.getInstance("TLS");
          sslContext.init(null, new TrustManager[]{
            new X509TrustManager() {
              public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
              }
              public void checkClientTrusted(X509Certificate[] certs, String authType) { }
              public void checkServerTrusted(X509Certificate[] certs, String authType) { }
            }
          }, null);
          httpClientBuilder.sslContext(sslContext);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
          throw new IllegalStateException("Failed to create insecure SSL context", e);
        }
      }
      this.restClient = RestClient.builder()
          .requestFactory(new JdkClientHttpRequestFactory(httpClientBuilder.build()))
          .build();
    }
    return this.restClient;
  }

  private String getAuthenticationCode() {
    // headerValue must be the BCrypt hash for the gocsaf/csaf provider
    // Therefore we send a fresh BCrypt hash of the configured password in the X-Csaf-Provider-Auth header.
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    return encoder.encode(this.configuration.getAutoPublish().getPassword());
  }

  /**
   * A {@link FileSystemResource} that overrides the filename used in the
   * multipart {@code Content-Disposition} header, so the provider receives the
   * correct {@code <trackingId>.json} filename regardless of the temp-file name.
   */
  private static class NamedFileResource extends FileSystemResource {
    private final String filename;

    NamedFileResource(Path path, String filename) {
      super(path);
      this.filename = filename;
    }

    @Override
    public String getFilename() {
      return this.filename;
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return super.equals(other);
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }
  }
}
