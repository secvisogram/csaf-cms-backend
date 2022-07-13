package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles.Role.AUTHOR;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafJsonCategoryTitle;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import de.bsi.secvisogram.csaf_cms_backend.CouchDBExtension;
import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.DatabaseException;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test for the workflow in the Advisory service. The required CouchDB container is started in the CouchDBExtension.
 */
@SpringBootTest
@ExtendWith(CouchDBExtension.class)
@DirtiesContext
@ContextConfiguration
@SuppressFBWarnings(value = "VA_FORMAT_STRING_USES_NEWLINE", justification = "False positives on multiline format strings")

public class AdvisoryWorkflowTest {

    private static final String EMPTY_PASSWD = "";

    @Autowired
    private AdvisoryService advisoryService;

    @Test
    @WithMockUser(username = "editor1", authorities = {CsafRoles.ROLE_AUTHOR})
    public void addAdvisoryTest() throws IOException, DatabaseException {

        final String csafJson = csafJsonCategoryTitle("Category1", "Title1");
        IdAndRevision idRev = advisoryService.addAdvisory(csafJson);
        AdvisoryResponse advisory = advisoryService.getAdvisory(idRev.getId());

        String dateNowMinutes =  DateTimeFormatter.ISO_INSTANT.format(Instant.now()).substring(0, 16);
        assertThat(advisory.getCsaf().at("/document/tracking/version").asText(), equalTo("0.0.1"));
        assertThat(advisory.getCsaf().at("/document/tracking/status").asText(), equalTo("draft"));
        assertThat(advisory.getCsaf().at("/document/tracking/current_release_date").asText(), startsWith(dateNowMinutes));
    }

    @Test
    @WithMockUser(username = "reviewer1", authorities = { CsafRoles.ROLE_REVIEWER})
    public void addAdvisoryTest_InvalidRole() throws IOException, DatabaseException {

        final String csafJson = csafJsonCategoryTitle("Category1", "Title1");
        Assertions.assertThrows(AccessDeniedException.class, () -> advisoryService.addAdvisory(csafJson));
     }

    @Test
    @WithMockUser(username = "editor1", authorities = {CsafRoles.ROLE_AUTHOR})
    public void readAdvisoryTest_AuthorOwn() throws IOException, DatabaseException {

        final String userName = "editor1";
        final String csafJson = csafJsonCategoryTitle("Category1", "Title1");
        IdAndRevision idRev = advisoryService.addAdvisoryForCredentials(csafJson, createAuthentication(userName, AUTHOR.getRoleName()));
        AdvisoryResponse advisory = advisoryService.getAdvisory(idRev.getId());
        assertThat(advisory.isChangeable(), is(true));
        assertThat(advisory.isDeletable(), is(true));
     }

    @Test
    @WithMockUser(username = "editor1", authorities = {CsafRoles.ROLE_AUTHOR})
    public void readAdvisoryTest_AuthorNotOwn() throws IOException, DatabaseException {

        final String advisoryUser = "John";
        final String csafJson = csafJsonCategoryTitle("Category1", "Title1");
        IdAndRevision idRev = advisoryService.addAdvisoryForCredentials(csafJson, createAuthentication(advisoryUser, AUTHOR.getRoleName()));
        AdvisoryResponse advisory = advisoryService.getAdvisory(idRev.getId());
        assertThat(advisory.isChangeable(), is(false));
        assertThat(advisory.isDeletable(), is(false));
    }

    private Authentication createAuthentication(String userName, String ... roles) {

        var authority = new SimpleGrantedAuthority(roles[0]);
        var principal =  new User(userName, EMPTY_PASSWD, singletonList(authority));
        return new TestingAuthenticationToken(principal, null, roles);
    }
}
