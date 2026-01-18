package network;

import dto.BarcodeScanDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.User;
import entities.Subscriber;
import logicControllers.OnlineUsersRegistry;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for processing external barcode
 * scanner simulations.
 * <p>
 * This handler authenticates a user based solely on their Subscriber ID,
 * bypassing traditional username/password login to simulate a physical card
 * swipe or barcode scan.
 * </p>
 */
public class BarcodeLoginHandler implements RequestHandler {

	private final UserController userController;
	private final OnlineUsersRegistry onlineUsersRegistry;

	/**
	 * Constructs the handler with necessary controllers.
	 */
	public BarcodeLoginHandler(UserController userController, OnlineUsersRegistry onlineUsersRegistry) {
		this.userController = userController;
		this.onlineUsersRegistry = onlineUsersRegistry;
	}

	/**
	 * Processes the barcode scan request.
	 * <p>
	 * It extracts the Subscriber ID from the DTO, attempts to log the user in via
	 * the UserController, and registers the connection in the OnlineUsersRegistry
	 * upon success.
	 * </p>
	 */
	@Override
	public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

		// 1. Extract the scan data from the request
		BarcodeScanDTO scanData = (BarcodeScanDTO) request.getData();

		try {
			// 2. Convert ID to integer (matching your getSubscriberById signature)
			int id = Integer.parseInt(scanData.getSubscriberId());

			// 3. Attempt login logic through the controller
			// Note: Use the loginByBarcode method we discussed to fetch the Subscriber
			Subscriber subscriber = userController.loginByBarcode(id);

			if (subscriber != null) {
				// 4. Register the user as "Online" in the server's registry
				// Since Subscriber inherits from User, this is polymorphic and valid.
				onlineUsersRegistry.registerUser(subscriber, client);

				// 5. Send success response back to the client
				ResponseDTO response = new ResponseDTO(true, "Barcode Login Successful", subscriber);
				client.sendToClient(response);

			} else {
				// 6. Return failure if subscriber not found or unauthorized
				ResponseDTO response = new ResponseDTO(false, "Invalid Subscriber ID or User not authorized.", null);
				client.sendToClient(response);
			}

		} catch (NumberFormatException e) {
			// Handle cases where the scanned ID is not a valid number
			client.sendToClient(new ResponseDTO(false, "Invalid barcode format. ID must be numeric.", null));
		} catch (Exception e) {
			// General error handling for server stability
			client.sendToClient(new ResponseDTO(false, "Internal server error during barcode login.", null));
			e.printStackTrace();
		}
	}
}