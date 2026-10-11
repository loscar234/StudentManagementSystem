package util;

/**
 * SessionManager – Thread-safe singleton managing the logged-in user context.
 */
public final class SessionManager {

    public enum Role {
        ADMIN,
        LECTURER,
        STUDENT,
        LIBRARIAN
    }

    private static final SessionManager INSTANCE = new SessionManager();

    private Integer userId;
    private String username;
    private String displayName;
    private Role currentRole;

    private SessionManager() {
        // Defaults to Admin for now until Login UI is integrated in Phase 4
        this.userId = 1;
        this.username = "admin";
        this.displayName = "System Administrator";
        this.currentRole = Role.ADMIN;
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public synchronized void login(int userId, String username, String displayName, Role role) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.currentRole = role;
    }

    public synchronized void logout() {
        this.userId = null;
        this.username = null;
        this.displayName = null;
        this.currentRole = null;
    }

    public synchronized boolean isAuthenticated() {
        return userId != null;
    }

    public synchronized Integer getUserId() {
        return userId;
    }

    public synchronized Integer getCurrentUserId() {
        return userId;
    }

    public synchronized String getUsername() {
        return username;
    }

    public synchronized String getDisplayName() {
        return displayName;
    }

    public synchronized Role getCurrentRole() {
        return currentRole;
    }

    public synchronized boolean hasRole(Role role) {
        return this.currentRole == role;
    }
}
