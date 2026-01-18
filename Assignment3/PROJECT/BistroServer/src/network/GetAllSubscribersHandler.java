package network;

import java.io.IOException;
import java.util.List;

import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Subscriber;
import entities.User;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;
import protocol.Commands;

/**
 * Server-side request handler responsible for retrieving all registered
 * subscribers.
 * <p>
 * This handler verifies that the requesting user is an authorized subscriber
 * and delegates the retrieval logic to the {@link UserController}.
 * </p>
 */
public class GetAllSubscribersHandler implements RequestHandler {

	private final UserController userController;

	/**
	 * Constructs a handler with the required user controller dependency.
	 */
	public GetAllSubscribersHandler(UserController userController) {
		this.userController = userController;
	}

	/**
	 * Handles a request to retrieve all subscribers.
	 * <p>
	 * The method validates the session user, fetches the list of all subscribers
	 * from the system, and sends the result back to the client.
	 * </p>
	 */
	@Override
	public void handle(RequestDTO request, ConnectionToClient client) {

		try {
			User performedBy = (User) client.getInfo("user");

			if (!(performedBy instanceof Subscriber)) {
				client.sendToClient(new ResponseDTO(false, "Unauthorized", null));
				return;
			}

			List<Subscriber> subscribers = userController.getAllSubscribers((Subscriber) performedBy);

			ResponseDTO response = new ResponseDTO(true, "Subscribers loaded", subscribers);

			client.sendToClient(response);

		} catch (IOException e) {
			e.printStackTrace();
			try {
				client.sendToClient(new ResponseDTO(false, "Server error", null));
			} catch (IOException ignore) {
			}
		}
	}
}
