package network;

import dto.RegisterSubscriberDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Subscriber;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for registering new subscribers.
 * <p>
 * This handler validates registration data, checks the permissions of the
 * performing user, and delegates the actual creation logic to the
 * {@link UserController}. It is typically used by restaurant agents or managers
 * through management interfaces.
 * </p>
 */
public class RegisterSubscriberHandler implements RequestHandler {

	private final UserController userController;

	/**
	 * Constructs a new RegisterSubscriberHandler with the required controller.
	 *
	 * @param userController controller responsible for user-related operations
	 */
	public RegisterSubscriberHandler(UserController userController) {
		this.userController = userController;
	}

	/**
	 * Handles a subscriber registration request.
	 * <p>
	 * The method performs the following steps:
	 * <ol>
	 * <li>Validates that the request data is of type
	 * {@link RegisterSubscriberDTO}</li>
	 * <li>Verifies that the current session user is authorized to register
	 * subscribers</li>
	 * <li>Delegates subscriber creation to the {@link UserController}</li>
	 * <li>Returns the newly created subscriber ID upon success</li>
	 * </ol>
	 * </p>
	 */
	@Override
	public void handle(RequestDTO request, ConnectionToClient client) {

		// Step 1: Validate request payload
		Object dataObj = request.getData();
		if (!(dataObj instanceof RegisterSubscriberDTO dto)) {
			send(client, new ResponseDTO(false, "Invalid register data", null));
			return;
		}

		// Step 2: Validate performing user and permissions
		Object performerObj = client.getInfo("user");
		if (!(performerObj instanceof Subscriber performedBy)) {
			send(client, new ResponseDTO(false, "Unauthorized", null));
			return;
		}

		// Step 3: Delegate subscriber creation to logic controller
		int createdId = userController.registerSubscriber(dto.getUsername(), dto.getFirstName(), dto.getLastName(),
				dto.getPhone(), dto.getEmail(), dto.getRole(), performedBy);

		// Step 4: Handle creation failure
		if (createdId == -1) {
			send(client, new ResponseDTO(false, "Subscriber was not created: permission denied or invalid data", null));
			return;
		}

		// Step 5: Return only the created subscriber ID (used by management UI)
		send(client, new ResponseDTO(true, "Subscriber created successfully", createdId));
	}

	/**
	 * Safely sends a response back to the client.
	 * <p>
	 * This helper method centralizes error handling for network transmission and
	 * prevents duplicated try/catch blocks inside the handler logic.
	 * </p>
	 */
	private void send(ConnectionToClient client, ResponseDTO res) {
		try {
			client.sendToClient(res);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
