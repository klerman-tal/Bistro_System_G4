package network;

import dto.SubscriberLoginDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Subscriber;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

public class SubscriberLoginHandler implements RequestHandler {
    private final UserController userController;

    public SubscriberLoginHandler(UserController userController) {
        this.userController = userController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {
        SubscriberLoginDTO loginData = (SubscriberLoginDTO) request.getData();
        
        // קריאה ללוגיקה הקיימת שלך ב-UserController
        Subscriber subscriber = userController.loginSubscriber(
            loginData.getSubscriberId(), 
            loginData.getUsername()
        );

        if (subscriber != null) {
            client.sendToClient(new ResponseDTO(true, "Login successful", subscriber));
        } else {
            client.sendToClient(new ResponseDTO(false, "Invalid ID or Username", null));
        }
    }
}