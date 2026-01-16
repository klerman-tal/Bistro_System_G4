package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Waiting;
import logicControllers.WaitingController;
import ocsf.server.ConnectionToClient;
import java.util.ArrayList;

public class GetWaitingListHandler implements RequestHandler {

    private final WaitingController waitingController;

    public GetWaitingListHandler(WaitingController waitingController) {
        this.waitingController = waitingController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) {
        ResponseDTO response;

        try {
            // שליפת הרשימה מה-Logic Controller
            ArrayList<Waiting> waitingList = waitingController.getActiveWaitingList();

            // יצירת תשובה מוצלחת עם הרשימה בתוך ה-Data
            response = new ResponseDTO(
                    true,
                    "Waiting list retrieved successfully",
                    waitingList
            );
        } catch (Exception e) {
            // טיפול בשגיאה במידה ומשהו השתבש בדרך
            response = new ResponseDTO(
                    false,
                    "Failed to retrieve waiting list: " + e.getMessage(),
                    null
            );
        }

        // שליחת התגובה ללקוח
        try {
            client.sendToClient(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}