package util;

import java.util.Arrays;

/**
 * SecurityContext – Centralized server-side authorization service.
 * Enforces role permissions and data-ownership checks before operations execute.
 */
public final class SecurityContext {

    private SecurityContext() {}

    /**
     * Asserts that a user is actively authenticated.
     *
     * @throws AccessDeniedException if no user session exists
     */
    public static void requireAuthenticated() {
        SessionManager session = SessionManager.getInstance();
        if (!session.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required: No active user session.");
        }
    }

    /**
     * Asserts that the authenticated user possesses at least one of the allowed roles.
     *
     * @param allowedRoles roles permitted to perform the action
     * @throws AccessDeniedException if session is invalid or user role is unauthorized
     */
    public static void requireAnyRole(SessionManager.Role... allowedRoles) {
        requireAuthenticated();
        SessionManager.Role currentRole = SessionManager.getInstance().getCurrentRole();
        for (SessionManager.Role role : allowedRoles) {
            if (currentRole == role) {
                return;
            }
        }
        throw new AccessDeniedException(String.format(
            "Access Denied: Role '%s' is not authorized. Required: %s",
            currentRole, Arrays.toString(allowedRoles)
        ));
    }

    public static void requireRole(SessionManager.Role role) {
        requireAnyRole(role);
    }

    /**
     * Asserts that a lecturer can only modify or access courses assigned to them.
     * Administrators and Registrars bypass this ownership check.
     *
     * @param lecturerId primary key ID of the lecturer
     * @throws AccessDeniedException if the current lecturer attempts to access another lecturer's course
     */
    public static void requireLecturerOwnershipOrAdmin(int lecturerId) {
        requireAuthenticated();
        SessionManager session = SessionManager.getInstance();
        SessionManager.Role currentRole = session.getCurrentRole();

        // Admin and Registrars can coordinate across faculty
        if (currentRole == SessionManager.Role.ADMIN || currentRole == SessionManager.Role.REGISTRAR) {
            return;
        }

        if (currentRole == SessionManager.Role.LECTURER) {
            Integer currentLecturerId = session.getLecturerId();
            if (currentLecturerId != null && currentLecturerId.equals(lecturerId)) {
                return;
            }
            throw new AccessDeniedException("Access Denied: You can only access and submit marks for your own assigned courses.");
        }

        throw new AccessDeniedException("Access Denied: Only assigned lecturers or administrators can perform this action.");
    }
}
