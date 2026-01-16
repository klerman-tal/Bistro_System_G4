package network;

import java.util.ArrayList;

import dbControllers.SpecialOpeningHours_DB_Controller;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.SpecialOpeningHours;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for retrieving
 * all special opening hours configurations.
 * <p>
 * This handler fetches special opening hours data from the database
 * and returns it to the client. Special opening hours typically
 * represent exceptions to the regular weekly schedule.
 * </p>
 */
public class GetSpecialOpeningHoursHandler implements RequestHandler {

    private final SpecialOpeningHours_DB_Controller db;

    /**
     * Constructs a handler with the required database controller dependency.
     */
    public GetSpecialOpeningHoursHandler(SpecialOpeningHours_DB_Controller db) {
        this.db = db;
    }

    /**
     * Handles a request to retrieve all special opening hours.
     * <p>
     * The method loads all special opening hours records from the database
     * and sends them back to the client as part of the response.
     * </p>
     */
    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {
        ArrayList<SpecialOpeningHours> list = db.getAllSpecialOpeningHours();
        client.sendToClient(new ResponseDTO(true, "Special opening hours loaded", list));
    }
}
