package logicControllers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import entities.User;
import ocsf.server.ConnectionToClient;

/**
 * Thread-safe registry that keeps track of currently online users.
 * <p>
 * The registry maps a user identifier to its active {@link ConnectionToClient}
 * instance and is primarily used by the notification subsystem to deliver
 * real-time popup messages to connected users.
 * </p>
 * <p>
 * This class is safe for concurrent access and can be used by multiple server
 * threads simultaneously.
 * </p>
 */
public class OnlineUsersRegistry {

	/**
	 * Mapping between user IDs and their active client connections.
	 */
	private final Map<Integer, ConnectionToClient> userIdToClient = new ConcurrentHashMap<>();

	/**
	 * Registers a user as online by associating the given user ID with a client
	 * connection.
	 *
	 * @param userId the unique identifier of the user
	 * @param client the active client connection
	 */
	public void setOnline(int userId, ConnectionToClient client) {
		if (client != null) {
			userIdToClient.put(userId, client);
		}
	}

	/**
	 * Convenience method for registering a user as online using a {@link User}
	 * object.
	 * <p>
	 * This method extracts the user ID from the given user and delegates to
	 * {@link #setOnline(int, ConnectionToClient)}.
	 * </p>
	 *
	 * @param user   the user to register as online
	 * @param client the active client connection
	 */
	public void registerUser(User user, ConnectionToClient client) {
		if (user != null && client != null) {
			setOnline(user.getUserId(), client);
		}
	}

	/**
	 * Marks a user as offline by removing their association from the registry.
	 *
	 * @param userId the unique identifier of the user
	 */
	public void setOffline(int userId) {
		userIdToClient.remove(userId);
	}

	/**
	 * Retrieves the client connection associated with a given user ID.
	 *
	 * @param userId the unique identifier of the user
	 * @return the corresponding {@link ConnectionToClient}, or {@code null} if the
	 *         user is not currently online
	 */
	public ConnectionToClient getClient(int userId) {
		return userIdToClient.get(userId);
	}

	/**
	 * Checks whether a given user is currently registered as online.
	 *
	 * @param userId the unique identifier of the user
	 * @return {@code true} if the user is online, {@code false} otherwise
	 */
	public boolean isOnline(int userId) {
		return userIdToClient.containsKey(userId);
	}

	/**
	 * Removes a client connection from the registry regardless of user ID.
	 * <p>
	 * This method is typically invoked when a client disconnects unexpectedly and
	 * the server needs to clean up stale connections.
	 * </p>
	 *
	 * @param client the client connection to remove
	 */
	public void removeClient(ConnectionToClient client) {
		if (client == null)
			return;
		userIdToClient.entrySet().removeIf(e -> e.getValue() == client);
	}
}
