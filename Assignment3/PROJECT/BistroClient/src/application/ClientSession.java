package application;

import application.ChatClient;
import entities.User;

/**
 * Client-side session holder.
 * Keeps logged-in user and acting user (for manager/agent actions).
 */
public class ClientSession {

    private static User loggedInUser;
    private static User actingUser;
    private static ChatClient chatClient;

    private ClientSession() {}

    // ===== Logged-in user =====
    public static void setLoggedInUser(User user) {
        loggedInUser = user;
        // default acting user is the logged-in user
        actingUser = user;
    }

    public static User getLoggedInUser() {
        return loggedInUser;
    }

    // ===== Acting user =====
    public static void setActingUser(User user) {
        actingUser = (user != null) ? user : loggedInUser;
    }

    public static User getActingUser() {
        return actingUser;
    }

    public static void resetActingUser() {
        actingUser = loggedInUser;
    }

    // ===== ChatClient =====
    public static void setChatClient(ChatClient client) {
        chatClient = client;
    }

    public static ChatClient getChatClient() {
        return chatClient;
    }
}
