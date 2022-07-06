package de.bsi.secvisogram.csaf_cms_backend.config;

/**
 * All available CSAF Roles
 */
public class CsafRoles {

    // Constants to use Role Names in annotations
        public static final String ROLE_REGISTERED = "ROLE_registered";
    public static final String ROLE_AUTHOR = "ROLE_author";
    public static final String ROLE_REVIEWER = "ROLE_reviewer";
    public static final String ROLE_EDITOR = "ROLE_editor";
    public static final String ROLE_PUBLISHER = "ROLE_publisher";
    public static final String ROLE_MANAGER = "ROLE_manager";

    public static final String ROLE_ADMINISTRATOR = "ROLE_administrator";

    public enum Role {
        REGISTERED(ROLE_REGISTERED),
        AUTHOR(ROLE_AUTHOR),
        REVIEWER(ROLE_REVIEWER),
        EDITOR(ROLE_EDITOR),
        PUBLISHER(ROLE_PUBLISHER),
        MANAGER(ROLE_MANAGER),
        ADMINISTRATOR(ROLE_ADMINISTRATOR);

        private final String roleName;

        Role(String roleName) {
            this.roleName = roleName;
        }

        public String getRoleName() {
            return roleName;
        }
    }
}
