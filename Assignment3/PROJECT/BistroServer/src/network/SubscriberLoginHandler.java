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
 * initializes a session for the connected client, and prevents duplicate
 * concurrent logins for the same user.
 * </p>
 */
public class SubscriberLoginHandler implements RequestHandler {

	private final UserController userController;
	private final OnlineUsersRegistry onlineUsers;

	/**
	 * Constructs a new SubscriberLoginHandler with required dependencies.
	 *
	 * @param userController controller responsible for user authentication
	 * @param onlineUsers    registry used to track online users
	 */
	public SubscriberLoginHandler(UserController userController, OnlineUsersRegistry onlineUsers) {
		this.userController = userController;
		this.onlineUsers = onlineUsers;
	}

	@Override
	public void handle(RequestDTO request, ConnectionToClient client) {

		// Step 1: Validate request payload
		Object dataObj = request.getData();
		if (!(dataObj instanceof SubscriberLoginDTO)) {
			send(client, new ResponseDTO(false, "Invalid login data", null));
			return;
		}

		SubscriberLoginDTO loginData = (SubscriberLoginDTO) dataObj;

		// Step 2: Authenticate subscriber
		Subscriber subscriber = userController.loginSubscriber(
				loginData.getSubscriberId(),
				loginData.getUsername()
		);

		if (subscriber == null) {
			send(client, new ResponseDTO(false, "Invalid ID or Username", null));
			return;
		}

		// Step 3: Block duplicate login
		if (onlineUsers != null && onlineUsers.isOnline(subscriber.getUserId())) {
			send(client, new ResponseDTO(
					false,
					"User is already logged in from another session",
					null
			));
			return;
		}

		// Step 4: Store subscriber in session
		client.setInfo("user", subscriber);

		// Step 5: Register subscriber as online
		if (onlineUsers != null) {
			onlineUsers.setOnline(subscriber.getUserId(), client);
		}

		// Step 6: Send successful login response
		send(client, new ResponseDTO(true, "Login successful", subscriber));
	}

	private void send(ConnectionToClient client, ResponseDTO res) {
		try {
			client.sendToClient(res);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
