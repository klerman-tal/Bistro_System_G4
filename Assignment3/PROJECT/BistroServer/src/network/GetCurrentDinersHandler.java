package network;

import java.util.ArrayList;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Reservation;
import logicControllers.ReservationController;
import ocsf.server.ConnectionToClient;

public class GetCurrentDinersHandler implements RequestHandler {

    private final ReservationController reservationController;

    public GetCurrentDinersHandler(ReservationController reservationController) {
        this.reservationController = reservationController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {
        
        // כאן אין לנו צורך ב-Data מהקליינט כי אנחנו פשוט מבקשים את כל הרשימה הקיימת
        
        try {
            // קריאה למתודה בלוגיקה (שתומכת בשאילתה שדיברנו עליה)
            ArrayList<Reservation> diners = reservationController.getCurrentDiners();

            if (diners != null) {
                // החזרת הצלחה עם רשימת הסועדים
                client.sendToClient(new ResponseDTO(true, "Current diners list retrieved successfully", diners));
            } else {
                client.sendToClient(new ResponseDTO(false, "Failed to retrieve current diners list", null));
            }
            
        } catch (Exception e) {
            // טיפול בשגיאות בלתי צפויות
            client.sendToClient(new ResponseDTO(false, "Server error: " + e.getMessage(), null));
        }
    }
}