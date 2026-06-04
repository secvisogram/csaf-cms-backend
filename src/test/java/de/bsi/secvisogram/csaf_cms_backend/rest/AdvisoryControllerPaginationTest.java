package de.bsi.secvisogram.csaf_cms_backend.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.model.template.DocumentTemplateService;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationPageResponse;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * Controller-layer tests for the opt-in pagination of {@code GET /api/v1/advisories}.
 * The service layer is mocked exactly as the neighbouring {@link AdvisoryControllerTest} does
 * (WebMvcTest + MockitoBean). These tests assert the wire-format branch:
 * <ul>
 *   <li>no {@code limit} -> bare JSON array (legacy, byte-identical shape);</li>
 *   <li>with {@code limit} -> the {@code AdvisoryDocumentInformationPage} envelope object;</li>
 *   <li>{@code limit} out of range -> 400;</li>
 *   <li>undecodable / stale {@code bookmark} -> 400.</li>
 * </ul>
 */
@WebMvcTest(AdvisoryController.class)
@SuppressFBWarnings(value = "VA_FORMAT_STRING_USES_NEWLINE", justification = "False positives on multiline format strings")
class AdvisoryControllerPaginationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdvisoryService advisoryService;

    @MockitoBean
    private DocumentTemplateService templateService;

    private static final String advisoryRoute = "/api/v1/advisories";

    @Test
    @WithMockUser
    void legacyModeNoLimitReturnsBareArray() throws Exception {
        String advisoryId = UUID.randomUUID().toString();
        AdvisoryInformationResponse info = new AdvisoryInformationResponse(advisoryId, WorkflowState.Draft);
        when(advisoryService.getAdvisoryInformations(null)).thenReturn(List.of(info));

        this.mockMvc.perform(get(advisoryRoute))
                .andExpect(status().isOk())
                // Body must be a bare JSON array, NOT an envelope object.
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].advisoryId").value(advisoryId))
                .andExpect(content().json(String.format("[{\"advisoryId\": \"%s\"}]", advisoryId)));

        // The paged service path must not be touched in legacy mode.
        verify(advisoryService, never()).getAdvisoryInformationsPage(any(), anyInt(), any());
    }

    @Test
    @WithMockUser
    void paginatedModeReturnsEnvelopeObject() throws Exception {
        String advisoryId = UUID.randomUUID().toString();
        AdvisoryInformationResponse info = new AdvisoryInformationResponse(advisoryId, WorkflowState.Draft);
        AdvisoryInformationPageResponse page =
                new AdvisoryInformationPageResponse(List.of(info), "next-bookmark", true, 50);
        when(advisoryService.getAdvisoryInformationsPage(null, 50, null)).thenReturn(page);

        this.mockMvc.perform(get(advisoryRoute).param("limit", "50"))
                .andExpect(status().isOk())
                // Body is the envelope object, not a bare array.
                .andExpect(jsonPath("$").isMap())
                .andExpect(jsonPath("$.advisories").isArray())
                .andExpect(jsonPath("$.advisories[0].advisoryId").value(advisoryId))
                .andExpect(jsonPath("$.bookmark").value("next-bookmark"))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.limit").value(50));

        // The legacy service path must not be touched in paginated mode.
        verify(advisoryService, never()).getAdvisoryInformations(any());
    }

    @Test
    @WithMockUser
    void paginatedModeLastPageHasNullBookmarkAndHasMoreFalse() throws Exception {
        AdvisoryInformationPageResponse page =
                new AdvisoryInformationPageResponse(List.of(), null, false, 50);
        when(advisoryService.getAdvisoryInformationsPage(null, 50, null)).thenReturn(page);

        this.mockMvc.perform(get(advisoryRoute).param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMore").value(false))
                .andExpect(jsonPath("$.bookmark").doesNotExist());
    }

    @Test
    @WithMockUser
    void bookmarkIsForwardedToTheService() throws Exception {
        AdvisoryInformationPageResponse page =
                new AdvisoryInformationPageResponse(List.of(), null, false, 25);
        when(advisoryService.getAdvisoryInformationsPage(null, 25, "the-cursor")).thenReturn(page);

        this.mockMvc.perform(get(advisoryRoute).param("limit", "25").param("bookmark", "the-cursor"))
                .andExpect(status().isOk());

        verify(advisoryService).getAdvisoryInformationsPage(null, 25, "the-cursor");
    }

    @Test
    @WithMockUser
    void limitZeroReturnsBadRequest() throws Exception {
        this.mockMvc.perform(get(advisoryRoute).param("limit", "0"))
                .andExpect(status().isBadRequest());
        verify(advisoryService, never()).getAdvisoryInformationsPage(any(), anyInt(), any());
    }

    @Test
    @WithMockUser
    void limitAboveMaximumReturnsBadRequest() throws Exception {
        this.mockMvc.perform(get(advisoryRoute).param("limit", "1001"))
                .andExpect(status().isBadRequest());
        verify(advisoryService, never()).getAdvisoryInformationsPage(any(), anyInt(), any());
    }

    @Test
    @WithMockUser
    void limitAtMaximumBoundaryIsAccepted() throws Exception {
        AdvisoryInformationPageResponse page =
                new AdvisoryInformationPageResponse(List.of(), null, false, 1000);
        when(advisoryService.getAdvisoryInformationsPage(null, 1000, null)).thenReturn(page);

        this.mockMvc.perform(get(advisoryRoute).param("limit", "1000"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void badBookmarkReturnsBadRequest() throws Exception {
        // The service rejects an undecodable / stale cursor with a 400-mapped CsafException.
        when(advisoryService.getAdvisoryInformationsPage(eq(null), eq(50), eq("bad-cursor")))
                .thenThrow(new CsafException("Invalid pagination cursor",
                        CsafExceptionKey.InvalidFilterExpression, HttpStatus.BAD_REQUEST));

        this.mockMvc.perform(get(advisoryRoute).param("limit", "50").param("bookmark", "bad-cursor"))
                .andExpect(status().isBadRequest());
    }
}
