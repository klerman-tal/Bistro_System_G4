package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Table;
import logicControllers.RestaurantController;
import ocsf.server.ConnectionToClient;

import java.util.List;

/**
 * Server-side request handler responsible for retrieving all restaurant tables.
 * <p>
 * This handler loads the tables data from the database via the
 * {@link RestaurantController} and returns the full list of tables to the
 * client.
 * </p>
 */
public class GetTablesHandler implements RequestHandler {

	private final RestaurantController restaurantController;

	/**
	 * Constructs a handler with the required restaurant controller dependency.
	 */
	public GetTablesHandler(RestaurantController restaurantController) {
		this.restaurantController = restaurantController;
	}

	/**
	 * Handles a request to retrieve all restaurant tables.
	 * <p>
	 * The method ensures the tables are loaded from the database, retrieves the
	 * updated table list, and sends it back to the client.
	 * </p>
	 */
	@Override
	public void handle(RequestDTO request, ConnectionToClient client) {
		try {
			restaurantController.loadTablesFromDb();
			List<Table> tables = restaurantController.getAllTables();

			client.sendToClient(new ResponseDTO(true, "Tables loaded", tables));

		} catch (Exception e) {
			try {
				client.sendToClient(new ResponseDTO(false, e.getMessage(), null));
			} catch (Exception ignored) {
			}
		}
	}
}
