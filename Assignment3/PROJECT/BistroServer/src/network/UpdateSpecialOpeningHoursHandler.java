package network;

import dbControllers.SpecialOpeningHours_DB_Controller;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.SpecialOpeningHours;
import ocsf.server.ConnectionToClient;

public class UpdateSpecialOpeningHoursHandler implements RequestHandler {
    private SpecialOpeningHours_DB_Controller db;

    public UpdateSpecialOpeningHoursHandler(SpecialOpeningHours_DB_Controller db) {
        this.db = db;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) {
        try {
            SpecialOpeningHours special = (SpecialOpeningHours) request.getData();
            boolean success = db.updateSpecialHours(special);
            
            if (success) {
                client.sendToClient(new ResponseDTO(true, "Special hours updated!", null));
            } else {
                client.sendToClient(new ResponseDTO(false, "DB error updating special hours.", null));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}