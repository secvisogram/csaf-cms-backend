package de.bsi.secvisogram.csaf_cms_backend.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.HttpHeaders;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey;
import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryWrapper;
import de.bsi.secvisogram.csaf_cms_backend.json.RemoveIdHelper;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class ValidatorServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(ValidatorServiceClient.class);

    private static final String VALIDATE_ENDPOINT = "/validate";
    private static final ValidationRequestTest csafTest = new ValidationRequestTest("test", "csaf_2_0");
    private static final ValidationRequestTest optionalTest = new ValidationRequestTest("preset", "optional");
    private static final ValidationRequestTest[] allValidationTests = {csafTest};

    public static boolean isAdvisoryValid(String baseUrl, AdvisoryWrapper advisory) throws CsafException {

        return new ValidatorServiceClient().isValid(baseUrl, advisory);
    }

    /**
     * Call the validator service to check whether the given advisory is valid
     * @param baseUrl base url of the service
     * @param advisory the advisory to check
     * @return true - advisory is valid
     * @throws CsafException error in accessing the server
     */
    boolean isValid(String baseUrl, AdvisoryWrapper advisory) throws CsafException {

        ValidatorResponse response = executeRequest(baseUrl, advisory);
        return isValid(response);
    }

    /**
     * Check whether the validation response is valid
     * @param response the response to check
     * @return true - response is valid
     */
    boolean isValid(ValidatorResponse response) {
        return response.isValid();
    }

    /**
     * Execute request to the validator service to validate the given advisory
     * @param baseUrl base url of the service
     * @param advisory the advisory to check
     * @return the validation response
     * @throws CsafException error in accessing the server
     */
    ValidatorResponse executeRequest(String baseUrl, AdvisoryWrapper advisory) throws CsafException {

        WebClient client = WebClient.create(baseUrl);
        WebClient.UriSpec<WebClient.RequestBodySpec> uriSpec = client.post();
        WebClient.RequestBodySpec bodySpec = uriSpec.uri(VALIDATE_ENDPOINT);

        try {
            final WebClient.RequestHeadersSpec<?> headersSpec = bodySpec.bodyValue(advisoryToRequest(advisory));
            final String resultText = headersSpec.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .accept(MediaType.APPLICATION_JSON)
                    .acceptCharset(StandardCharsets.UTF_8)
                    .ifNoneMatch("*")
                    .ifModifiedSince(ZonedDateTime.now())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            final ObjectMapper jacksonMapper = new ObjectMapper();
            return jacksonMapper.readValue(resultText, ValidatorResponse.class);
        } catch (WebClientResponseException | WebClientRequestException ex) {
            LOG.error("Error in access to validation server", ex);
            throw new CsafException("Error in call to validation server",
                    CsafExceptionKey.ErrorAccessingValidationServer, HttpStatus.UNPROCESSABLE_ENTITY);
        } catch (JsonProcessingException ex) {
            LOG.error("Error creating request to validation server", ex);
            throw new CsafException("Error creating request to validation server",
                    CsafExceptionKey.ErrorAccessingValidationServer, HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    /**
     * Convert csaf part of advisory into a validator service request
     * @param advisory the advisory to convert
     * @return the request
     * @throws JsonProcessingException error in creating request
     */
    String advisoryToRequest(AdvisoryWrapper advisory) throws JsonProcessingException {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        String jsonStr = jacksonMapper.writeValueAsString(advisory.getCsaf());
        ObjectNode csafNode = jacksonMapper.readValue(jsonStr, ObjectNode.class);
        RemoveIdHelper.removeCommentIds(csafNode);
        ValidationRequest request = new ValidationRequest(csafNode, allValidationTests);
        ObjectWriter writer = jacksonMapper.writer(new DefaultPrettyPrinter());
        return writer.writeValueAsString(request);
    }

    /**
     * s <a href="https://stackoverflow.com/questions/59735951/databufferlimitexception-exceeded-limit-on-max-bytes-to-buffer-webflux-error?noredirect=1">DataBufferLimitException</a>
     */
    @Bean
    public WebClient webClient() {
        final int size = 16 * 1024 * 1024;
        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
                .build();
        return WebClient.builder()
                .exchangeStrategies(strategies)
                .build();
    }
}
