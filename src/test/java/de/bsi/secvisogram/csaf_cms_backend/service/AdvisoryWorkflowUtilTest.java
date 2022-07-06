package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles.Role.*;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafJsonTitle;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryWrapper;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class AdvisoryWorkflowUtilTest {

    private static final String EMPTY_PASSWD = "";

    @Test
    public void canDeleteAdvisoryTest_registeredDeleteOwn() throws IOException {

        var userName = "John";
        var wrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitle("The Title"), userName);
        Authentication credentials = createAuthentication(userName, REGISTERED);
        assertThat(AdvisoryWorkflowUtil.canDeleteAdvisory(wrapper, credentials), is(false));
    }

    @Test
    public void canDeleteAdvisoryTest_authorDeleteOwn() throws IOException {

        var userName = "John";
        var wrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitle("The Title"), userName);
        Authentication credentials = createAuthentication(userName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canDeleteAdvisory(wrapper, credentials), is(true));
    }

    @Test
    public void canDeleteAdvisoryTest_authorDeleteNotOwn() throws IOException {

        var userName = "John";
        var otherName = "Jack";
        var wrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitle("The Title"), userName);
        Authentication credentials = createAuthentication(otherName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canDeleteAdvisory(wrapper, credentials), is(false));
    }

    @Test
    public void canDeleteAdvisoryTest_authorDeleteOwnNotDraft() throws IOException {

        var userName = "John";
        var wrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitle("The Title"), userName);
        wrapper.setWorkflowState(WorkflowState.Approved);
        Authentication credentials = createAuthentication(userName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canDeleteAdvisory(wrapper, credentials), is(false));
    }

    @Test
    public void canDeleteAdvisoryTest_editorDeleteNotOwn() throws IOException {

        var userName = "John";
        var otherName = "Jack";
        var wrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitle("The Title"), userName);
        Authentication credentials = createAuthentication(otherName, EDITOR);
        assertThat(AdvisoryWorkflowUtil.canDeleteAdvisory(wrapper, credentials), is(true));
    }

    @Test
    public void canDeleteAdvisoryTest_editorDeleteOwnNotDraft() throws IOException {

        var userName = "John";
        var wrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitle("The Title"), userName);
        wrapper.setWorkflowState(WorkflowState.Approved);
        Authentication credentials = createAuthentication(userName, EDITOR);
        assertThat(AdvisoryWorkflowUtil.canDeleteAdvisory(wrapper, credentials), is(false));
    }

    @Test
    public void canDeleteAdvisoryTest_managerDeleteNotOwnNotDraft() throws IOException {

        var userName = "John";
        var otherName = "Jack";
        var wrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitle("The Title"), userName);
        wrapper.setWorkflowState(WorkflowState.Approved);
        Authentication credentials = createAuthentication(otherName, MANAGER);
        assertThat(AdvisoryWorkflowUtil.canDeleteAdvisory(wrapper, credentials), is(true));
    }

    /**
     * Create authentication
     * @param userName the Name of the user
     * @param role the role of the user
     * @return the created authentication
     */
    private Authentication createAuthentication(String userName, CsafRoles.Role role) {

        var authority = new SimpleGrantedAuthority(role.getRoleName());
        var principal =  new User(userName, EMPTY_PASSWD, singletonList(authority));
        return new TestingAuthenticationToken(principal, null, role.getRoleName());
    }
}
