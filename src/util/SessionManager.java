package util;

/**
 * SessionManager – Thread-safe singleton managing the logged-in user context.
 */
public final class SessionManager {

    public enum Role {
        ADMIN,
        REGISTRAR,
        LECTURER,
        LIBRARIAN,
        APPROVER
    }

    private static final SessionManager INSTANCE = new SessionManager();

    private Integer userId;
    private Integer personId;
    private Integer lecturerId;
    private String username;
    private String displayName;
    private Role currentRole;

    private SessionManager() {
        // Starts unauthenticated; user must log in via LoginDialog
        this.userId = null;
        this.personId = null;
        this.lecturerId = null;
        this.username = null;
        this.displayName = null;
        this.currentRole = null;
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public synchronized void login(int userId, String username, String displayName, Role role, Integer personId, Integer lecturerId) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.currentRole = role;
        this.personId = personId;
        this.lecturerId = lecturerId;
    }

    public synchronized void logout() {
        this.userId = null;
        this.personId = null;
        this.lecturerId = null;
        this.username = null;
        this.displayName = null;
        this.currentRole = null;
    }

    public synchronized boolean isAuthenticated() {
        return userId != null && currentRole != null;
    }

    public synchronized Integer getUserId() {
        return userId;
    }

    public synchronized Integer getCurrentUserId() {
        return userId;
    }

    public synchronized Integer getPersonId() {
        return personId;
    }

    public synchronized Integer getLecturerId() {
        return lecturerId;
    }

    public synchronized String getUsername() {
        return username;
    }

    public synchronized String getCurrentUsername() {
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
