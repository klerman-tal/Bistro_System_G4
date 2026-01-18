package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import dto.UpdateSubscriberDetailsDTO;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for updating subscriber personal
 * details.
 * <p>
 * This handler receives updated subscriber information from the client,
 * delegates the update operation to the {@link UserController}, and returns a
 * success or failure response.
 * </p>
 * <p>
 * Typical use cases include profile editing by authorized staff through
 * management interfaces.
 * </p>
 */
public class UpdateSubscriberDetailsHandler implements RequestHandler {

	private final UserController userController;

	/**
	 * Constructs a new UpdateSubscriberDetailsHandler with the required controller.
	 *
	 * @param userController controller responsible for subscriber management
	 */
	public UpdateSubscriberDetailsHandler(UserController userController) {
		this.userController = userController;
	}

	/**
	 * Handles a request to update subscriber details.
	 * <p>
	 * The method extracts updated personal information from the request DTO,
	 * forwards it to the {@link UserController}, and reports whether the update
	 * operation was successful.
	 * </p>
	 */
	@Override
	public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

		// Extract update data from request
		UpdateSubscriberDetailsDTO dto = (UpdateSubscriberDetailsDTO) request.getData();

		// Delegate update logic to the controller
		boolean success = userController.updateSubscriberDetails(dto.getSubscriberId(), dto.getUsername(),
				dto.getFirstName(), dto.getLastName(), dto.getPhone(), dto.getEmail());

		// Send update result to the client
		client.sendToClient(new ResponseDTO(success, success ? "Details updated successfully" : "Update failed", null));
	}
}
