package util;

import model.User;
import model.UserDAO;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * AuthService – Authentication and security service.
 * Handles BCrypt password hashing, verification, lockout throttling,
 * and first-run administrator provisioning without hardcoded credentials.
 */
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final UserDAO userDAO = new UserDAO();

    public boolean isSystemProvisioned() throws SQLException {
        return userDAO.countUsers() > 0;
    }

    /**
     * Provisions the initial administrator account.
     * Enforces that this can only be executed once when the system has no existing users.
     */
    public synchronized void provisionInitialAdmin(String username, String rawPassword) throws SQLException {
        if (isSystemProvisioned()) {
            throw new IllegalStateException("System is already provisioned with administrative users.");
        }
        if (username == null || username.trim().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters.");
        }
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }

        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
        User admin = new User(username.trim(), hash, SessionManager.Role.ADMIN);
        userDAO.insert(admin);
    }

    /**
     * Authenticates a user against BCrypt hash, enforcing rate-limiting lockouts.
     */
    public boolean login(String username, String rawPassword) throws SQLException {
        if (username == null || rawPassword == null) return false;

        User user = userDAO.findByUsername(username.trim());
        if (user == null) {
            return false;
        }

        if (!user.isActive()) {
            throw new AccessDeniedException("This account is inactive. Please contact an administrator.");
        }

        // Check if locked
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new AccessDeniedException("Account is temporarily locked due to consecutive failed logins. Please try again later.");
        }

        // Verify hash
        boolean passwordMatches = false;
        try {
            passwordMatches = BCrypt.checkpw(rawPassword, user.getPasswordHash());
        } catch (Exception ignored) {}

        if (!passwordMatches) {
            int newFailedCount = user.getFailedAttempts() + 1;
            LocalDateTime lockedUntil = null;
            if (newFailedCount >= MAX_FAILED_ATTEMPTS) {
                lockedUntil = LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES);
            }
            userDAO.recordFailedAttempt(user.getId(), newFailedCount, lockedUntil);
            return false;
        }

        // Success
        userDAO.recordSuccessfulLogin(user.getId());
        String displayName = user.getUsername();
        SessionManager.getInstance().login(
            user.getId(),
            user.getUsername(),
            displayName,
            user.getRole(),
            user.getPersonId(),
            user.getLecturerId()
        );

        return true;
    }
}
