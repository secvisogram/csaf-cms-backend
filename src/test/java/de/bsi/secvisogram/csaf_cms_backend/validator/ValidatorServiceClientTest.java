package de.bsi.secvisogram.csaf_cms_backend.validator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryWrapper;
import de.bsi.secvisogram.csaf_cms_backend.json.VersioningType;
import java.io.IOException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class ValidatorServiceClientTest {

    @Mock
    WebClient webClient;

    @Mock
    WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    WebClient.RequestBodySpec requestBodySpec;

    @Mock
    WebClient.ResponseSpec responseSpec;

    @Test
    @Disabled("Mock Validation ValidatorServiceClient")
    public void validatorTest() throws IOException, CsafException {

        final String csaf = """
        {
            "document": {
                "category": "CSAF_BASE"
            }
        }
        """;
        ValidatorResponse response = new ValidatorResponse();
        final com.fasterxml.jackson.databind.ObjectMapper jacksonMapper = new ObjectMapper();
        String jsonStr = jacksonMapper.writeValueAsString(response);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(any())).thenReturn(requestBodySpec);
        when(responseSpec.bodyToMono(ArgumentMatchers.<Class<String>>notNull()))
                .thenReturn(Mono.just(jsonStr));


        AdvisoryWrapper newAdvisoryNode = AdvisoryWrapper.createNewFromCsaf(csaf, "testuser",
                VersioningType.Semantic.name());

        new ValidatorServiceClient().executeRequest("http://test.de/api/v1", newAdvisoryNode);
    }
}
