package de.bsi.secvisogram.csaf_cms_backend.validator;

import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafToRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey;
import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryWrapper;
import de.bsi.secvisogram.csaf_cms_backend.json.VersioningType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class ValidatorServiceClientTest {

    @Mock
    WebClient mockWebClient;

    @Mock
    WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    WebClient.RequestBodySpec requestBodySpec;

    @Mock
    WebClient.ResponseSpec responseSpec;

    private static final String TEST_CSAF = """
            {
                "document": {
                    "category": "CSAF_BASE"
                }
            }
            """;

    @Test
    void validatorRequestJsonTest() throws JsonProcessingException {


        var resultText = """
                { "isValid":true,
                  "tests":[ 
                    {"errors":[],"infos":[],"warnings":[],"isValid":true,"name":"csaf_2_0"}]}
                """;
        final ObjectMapper jacksonMapper = new ObjectMapper();
        jacksonMapper.readValue(resultText, ValidatorResponse.class);

    }

    @Test
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
            justification = "Bug in SpotBugs: https://github.com/spotbugs/spotbugs/issues/1338")
    public void successTest() throws IOException, CsafException {
        final ValidatorResponse response = new ValidatorResponse()
                .setTests(new ValidatorResponseTest[0]);
        final com.fasterxml.jackson.databind.ObjectMapper jacksonMapper = new ObjectMapper();
        final String jsonStr = jacksonMapper.writeValueAsString(response);

        try (final MockedStatic<WebClient> staticWebClient = Mockito.mockStatic(WebClient.class)) {
            staticWebClient.when(() -> WebClient.create(anyString())).thenReturn(mockWebClient);
            when(mockWebClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(any(), any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.accept(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.acceptCharset(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.ifNoneMatch(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.ifModifiedSince(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(ArgumentMatchers.<Class<String>>notNull())).thenReturn(Mono.just(jsonStr));

            final AdvisoryWrapper newAdvisoryNode = AdvisoryWrapper.createNewFromCsaf(
                    csafToRequest(TEST_CSAF),
                    "testuser",
                    VersioningType.Semantic.name()
            );

            ValidatorServiceClient.isAdvisoryValid("http://test.de/api/v1", newAdvisoryNode);
        }
    }

    @Test
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
            justification = "Bug in SpotBugs: https://github.com/spotbugs/spotbugs/issues/1338")
    public void malformedResponseTest() throws IOException, CsafException {
        final String jsonStr = "Not even valid json at all!";

        try (final MockedStatic<WebClient> staticWebClient = Mockito.mockStatic(WebClient.class)) {
            staticWebClient.when(() -> WebClient.create(anyString())).thenReturn(mockWebClient);
            when(mockWebClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(any(), any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.accept(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.acceptCharset(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.ifNoneMatch(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.ifModifiedSince(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(ArgumentMatchers.<Class<String>>notNull())).thenReturn(Mono.just(jsonStr));

            final AdvisoryWrapper newAdvisoryNode = AdvisoryWrapper.createNewFromCsaf(
                    csafToRequest(TEST_CSAF),
                    "testuser",
                    VersioningType.Semantic.name()
            );

            final CsafException exception = Assertions.assertThrows(
                    CsafException.class,
                    () -> ValidatorServiceClient.isAdvisoryValid("http://test.de/api/v1", newAdvisoryNode)
            );
            Assertions.assertEquals(CsafExceptionKey.ErrorAccessingValidationServer, exception.getExceptionKey());
        }
    }

    @Test
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
            justification = "Bug in SpotBugs: https://github.com/spotbugs/spotbugs/issues/1338")
    public void unreachableServiceTest() throws IOException, CsafException {
        try (final MockedStatic<WebClient> staticWebClient = Mockito.mockStatic(WebClient.class)) {
            staticWebClient.when(() -> WebClient.create(anyString())).thenReturn(mockWebClient);
            when(mockWebClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(any(), any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.accept(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.acceptCharset(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.ifNoneMatch(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.ifModifiedSince(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenThrow(Mockito.mock(WebClientRequestException.class));

            final AdvisoryWrapper newAdvisoryNode = AdvisoryWrapper.createNewFromCsaf(
                    csafToRequest(TEST_CSAF),
                    "testuser",
                    VersioningType.Semantic.name()
            );

            final CsafException exception = Assertions.assertThrows(
                    CsafException.class,
                    () -> ValidatorServiceClient.isAdvisoryValid("http://test.de/api/v1", newAdvisoryNode)
            );
            Assertions.assertEquals(CsafExceptionKey.ErrorAccessingValidationServer, exception.getExceptionKey());
        }
    }
}
