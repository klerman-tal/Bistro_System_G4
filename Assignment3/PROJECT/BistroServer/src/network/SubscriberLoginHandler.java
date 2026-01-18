package network;

import dto.SubscriberLoginDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Subscriber;
import logicControllers.OnlineUsersRegistry;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for subscriber login.
 * <p>
 * This handler authenticates a subscriber using subscriber ID and username,
 * initializes a session for the connected client, and optionally registers the
 * subscriber as an online user for real-time notifications.
 * </p>
 */
public class SubscriberLoginHandler implements RequestHandler {

	private final UserController userController;
	private final OnlineUsersRegistry onlineUsers;

	/**
	 * Constructs a new SubscriberLoginHandler with required dependencies.
	 *
	 * @param userController controller responsible for user authentication
	 * @param onlineUsers    registry used to track online users for notifications
	 */
	public SubscriberLoginHandler(UserController userController, OnlineUsersRegistry onlineUsers) {
		this.userController = userController;
		this.onlineUsers = onlineUsers;
	}

	/**
	 * Handles a subscriber login request.
	 * <p>
	 * The method performs the following steps:
	 * <ol>
	 * <li>Validates login request data</li>
	 * <li>Authenticates the subscriber via {@link UserController}</li>
	 * <li>Stores the subscriber in the client session</li>
	 * <li>Registers the subscriber as online (for SMS/notification delivery)</li>
	 * </ol>
	 * </p>
	 */
	@Override
	public void handle(RequestDTO request, ConnectionToClient client) {

		// Step 1: Validate request payload
		Object dataObj = request.getData();
		if (!(dataObj instanceof SubscriberLoginDTO loginData)) {
			send(client, new ResponseDTO(false, "Invalid login data", null));
			return;
		}

		// Step 2: Authenticate subscriber
		Subscriber subscriber = userController.loginSubscriber(loginData.getSubscriberId(), loginData.getUsername());

		if (subscriber == null) {
			send(client, new ResponseDTO(false, "Invalid ID or Username", null));
			return;
		}

		// Step 3: Store subscriber in session
		client.setInfo("user", subscriber);

		// Step 4: Register subscriber as online for real-time notifications
		if (onlineUsers != null) {
			onlineUsers.setOnline(subscriber.getUserId(), client);
		}

		// Step 5: Send successful login response
		send(client, new ResponseDTO(true, "Login successful", subscriber));
	}

	/**
	 * Safely sends a response to the connected client.
	 * <p>
	 * Centralizes network send logic and prevents duplicated try/catch blocks
	 * throughout the handler.
	 * </p>
	 *
	 * @param client the client connection
	 * @param res    the response to send
	 */
	private void send(ConnectionToClient client, ResponseDTO res) {
		try {
			client.sendToClient(res);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
