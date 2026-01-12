package logicControllers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ocsf.server.ConnectionToClient;

/**
 * Keeps track of online users (userId -> ConnectionToClient).
 * Used by the notification dispatcher to send popups to the right user.
 */
public class OnlineUsersRegistry {

    private final Map<Integer, ConnectionToClient> userIdToClient = new ConcurrentHashMap<>();

    public void setOnline(int userId, ConnectionToClient client) {
        if (client != null) {
            userIdToClient.put(userId, client);
        }
    }

    public void setOffline(int userId) {
        userIdToClient.remove(userId);
    }

    public ConnectionToClient getClient(int userId) {
        return userIdToClient.get(userId);
    }

    public boolean isOnline(int userId) {
        return userIdToClient.containsKey(userId);
    }
}
