package de.bsi.secvisogram.csaf_cms_backend.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryPageResponse;
import de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdvisoryV2Controller.class)
public class AdvisoryV2ControllerTest {

    private static final String ROUTE = "/api/v2/advisories";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdvisoryService advisoryService;

    @Test
    @WithMockUser
    void defaultsAndResponseShape() throws Exception {
        AdvisoryInformationResponse info = new AdvisoryInformationResponse("id-1", WorkflowState.Draft);
        when(advisoryService.getAdvisoryPage(isNull(), eq(0), eq(50), eq(List.of())))
                .thenReturn(new AdvisoryPageResponse(List.of(info), 0, 50, 7));

        mockMvc.perform(get(ROUTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].advisoryId").value("id-1"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.totalElements").value(7));
    }

    @Test
    @WithMockUser
    void bindsParameters() throws Exception {
        when(advisoryService.getAdvisoryPage(eq("{}"), eq(2), eq(20), eq(List.of("title,asc", "tlp,desc"))))
                .thenReturn(new AdvisoryPageResponse(List.of(), 2, 20, 0));

        mockMvc.perform(get(ROUTE).param("expression", "{}").param("page", "2").param("size", "20")
                        .param("sort", "title,asc").param("sort", "tlp,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2));
    }

    @Test
    @WithMockUser
    void singleSortWithDirectionIsNotSplit() throws Exception {
        when(advisoryService.getAdvisoryPage(isNull(), eq(0), eq(50), eq(List.of("title,desc"))))
                .thenReturn(new AdvisoryPageResponse(List.of(), 0, 50, 0));

        mockMvc.perform(get(ROUTE).param("sort", "title,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    @WithMockUser
    void blankSortValuesAreIgnored() throws Exception {
        when(advisoryService.getAdvisoryPage(isNull(), eq(0), eq(50), eq(List.of("tlp"))))
                .thenReturn(new AdvisoryPageResponse(List.of(), 0, 50, 0));

        mockMvc.perform(get(ROUTE).param("sort", "").param("sort", "tlp").param("sort", " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    @WithMockUser
    void csafExceptionGivesBadRequestWithMessage() throws Exception {
        when(advisoryService.getAdvisoryPage(any(), anyInt(), anyInt(), any()))
                .thenThrow(new CsafException("Invalid sort 'x'", CsafExceptionKey.InvalidListParameter,
                        HttpStatus.BAD_REQUEST));

        mockMvc.perform(get(ROUTE).param("sort", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid sort 'x'"));
    }
}
