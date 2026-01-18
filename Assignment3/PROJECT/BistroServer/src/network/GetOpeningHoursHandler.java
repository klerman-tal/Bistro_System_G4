package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import entities.OpeningHouers;
import logicControllers.RestaurantController;
import ocsf.server.ConnectionToClient;

import java.util.ArrayList;

/**
 * Server-side request handler responsible for retrieving the restaurant opening
 * hours.
 * <p>
 * This handler fetches the configured opening hours from the
 * {@link RestaurantController} and returns them to the client.
 * </p>
 */
public class GetOpeningHoursHandler implements RequestHandler {

	private final RestaurantController restaurantController;

	/**
	 * Constructs a handler with the required restaurant controller dependency.
	 */
	public GetOpeningHoursHandler(RestaurantController restaurantController) {
		this.restaurantController = restaurantController;
	}

	/**
	 * Handles a request to retrieve the restaurant opening hours.
	 * <p>
	 * The method loads the opening hours data and sends it back to the client as
	 * part of the response.
	 * </p>
	 */
	@Override
	public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

		try {
			ArrayList<OpeningHouers> openingHours = restaurantController.getOpeningHours();

			client.sendToClient(new ResponseDTO(true, "Opening hours loaded", openingHours));

		} catch (Exception e) {
			e.printStackTrace();
			client.sendToClient(new ResponseDTO(false, "Failed to load opening hours", null));
		}
	}
}
