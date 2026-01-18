package application;

import application.ChatClient;
import entities.User;

/**
 * Client-side session holder.
 * <p>
 * This class maintains global session state for the client application.
 * It stores references to:
 * <ul>
 *   <li>The user who is currently logged in</li>
 *   <li>The user on whose behalf actions are currently performed (acting user)</li>
 *   <li>The active {@link ChatClient} instance used for server communication</li>
 * </ul>
 * </p>
 *
 * <p>
 * The distinction between <b>logged-in user</b> and <b>acting user</b> allows
 * managers or agents to perform actions on behalf of another user while
 * still preserving the original authenticated identity.
 * </p>
 *
 * <p>
 * This class is implemented as a static utility holder and cannot be instantiated.
 * </p>
 */
public class ClientSession {

    /** The user that successfully authenticated into the system. */
    private static User loggedInUser;

    /**
     * The user currently acting in the system.
     * Defaults to {@link #loggedInUser} but may change temporarily
     * (e.g., manager acting on behalf of a subscriber).
     */
    private static User actingUser;

    /** Shared ChatClient instance for the current client session. */
    private static ChatClient chatClient;

    /**
     * Private constructor to prevent instantiation.
     */
    private ClientSession() {}

    /* ================= Logged-in user ================= */

    /**
     * Sets the logged-in user for the session.
     * <p>
     * IMPORTANT RULE:
     * - loggedInUser must NOT change until logout.
     * - actingUser should be set to loggedInUser ONLY on a real login (or when user changes),
     *   NOT when returning to Menu/back navigation.
     * </p>
     *
     * @param user the authenticated user
     */
    public static void setLoggedInUser(User user) {

        // logout case
        if (user == null) {
            loggedInUser = null;
            actingUser = null;
            return;
        }

        boolean isNewLogin =
                (loggedInUser == null) ||
                (loggedInUser.getUserId() != user.getUserId());

        loggedInUser = user;

        // Only on real login (or user change) -> reset acting user to logged-in user
        if (isNewLogin) {
            actingUser = user;
            return;
        }

        // If same logged-in user and acting user is missing -> restore to logged-in user
        if (actingUser == null) {
            actingUser = user;
        }
    }

    /**
     * Returns the currently logged-in user.
     *
     * @return the authenticated {@link User}, or {@code null} if not logged in
     */
    public static User getLoggedInUser() {
        return loggedInUser;
    }

    /* ================= Acting user ================= */

    /**
     * Sets the acting user for the session.
     * <p>
     * If {@code null} is provided, the acting user is reset to the logged-in user.
     * </p>
     *
     * @param user the user to act as, or {@code null} to revert to logged-in user
     */
    public static void setActingUser(User user) {
        actingUser = (user != null) ? user : loggedInUser;
    }

    /**
     * Returns the user currently acting in the system.
     *
     * @return the acting {@link User}
     */
    public static User getActingUser() {
        return actingUser;
    }

    /**
     * Resets the acting user back to the logged-in user.
     */
    public static void resetActingUser() {
        actingUser = loggedInUser;
    }

    /* ================= ChatClient ================= */

    /**
     * Sets the {@link ChatClient} instance associated with this session.
     *
     * @param client the active ChatClient
     */
    public static void setChatClient(ChatClient client) {
        chatClient = client;
    }

    /**
     * Returns the {@link ChatClient} associated with this session.
     *
     * @return the active ChatClient instance
     */
    public static ChatClient getChatClient() {
        return chatClient;
    }
}
