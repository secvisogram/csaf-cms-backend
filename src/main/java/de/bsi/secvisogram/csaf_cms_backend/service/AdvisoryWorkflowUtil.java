package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles.Role.*;

import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryWrapper;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Helper Methods for workflow and permissions
 */
public class AdvisoryWorkflowUtil {


    /**
     * Check whether the given advisory info can be deleted with the given credentials
     * @param response the advisory info to check
     * @param credentials the credentials for the check
     * @return true - info can be deleted
     */
    public static boolean canDeleteAdvisory(AdvisoryInformationResponse response, Authentication credentials) {

        return canDeleteAdvisory(response.getOwner(), response.getWorkflowState(), credentials);
    }

    /**
     * Check whether the given advisory can be deleted with the given credentials
     * @param advisory the advisory to check
     * @param credentials the credentials for the check
     * @return true - info can be deleted
     */
    public static boolean canDeleteAdvisory(AdvisoryWrapper advisory, Authentication credentials) {

        return canDeleteAdvisory(advisory.getOwner(), advisory.getWorkflowState(), credentials);
    }

    /**
     * Check whether a advisory with the geiven user and state can be deleted with the given credentials
     * @param userToCheck the advisory user to checck
     * @param stateToCheck the advisory workflow state to check
     * @param credentials the credentials for the check
     * @return true - info can be deleted
     */
    public static boolean canDeleteAdvisory(String userToCheck, WorkflowState stateToCheck, Authentication credentials) {

        boolean canBeDeleted = false;
        if (hasRole(AUTHOR, credentials)) {
            canBeDeleted = isOwnAdvisory(userToCheck, credentials) && isInStateDraft(stateToCheck);
        }
        if (hasRole(EDITOR, credentials)) {
            canBeDeleted = isInStateDraft(stateToCheck);
        }
        if (hasRole(MANAGER, credentials)) {
            canBeDeleted = true;
        }
        return canBeDeleted;
    }

    public static boolean hasRole(CsafRoles.Role csafRole, Authentication credentials) {

        return credentials.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals(csafRole.getRoleName()));
    }

    public static boolean isOwnAdvisory(String userToCheck, Authentication credentials) {
        return userToCheck.equals(credentials.getName());
    }

    public static boolean isInStateDraft(WorkflowState stateToCheck) {
        return stateToCheck == WorkflowState.Draft;
    }
}
