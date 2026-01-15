package network;

import java.util.ArrayList;

import dbControllers.SpecialOpeningHours_DB_Controller;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.SpecialOpeningHours;
import ocsf.server.ConnectionToClient;

public class GetSpecialOpeningHoursHandler implements RequestHandler {

    private final SpecialOpeningHours_DB_Controller db;

    public GetSpecialOpeningHoursHandler(SpecialOpeningHours_DB_Controller db) {
        this.db = db;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {
        ArrayList<SpecialOpeningHours> list = db.getAllSpecialOpeningHours();
        client.sendToClient(new ResponseDTO(true, "Special opening hours loaded", list));
    }
}
	