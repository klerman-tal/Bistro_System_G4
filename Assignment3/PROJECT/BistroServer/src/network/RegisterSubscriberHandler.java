package network;

import dto.RegisterSubscriberDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Subscriber;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

public class RegisterSubscriberHandler implements RequestHandler {

    private final UserController userController;

    public RegisterSubscriberHandler(UserController userController) {
        this.userController = userController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) {

        Object dataObj = request.getData();
        if (!(dataObj instanceof RegisterSubscriberDTO dto)) {
            send(client, new ResponseDTO(false, "Invalid register data", null));
            return;
        }

        Object performerObj = client.getInfo("user");
        if (!(performerObj instanceof Subscriber performedBy)) {
            send(client, new ResponseDTO(false, "Unauthorized", null));
            return;
        }

        int createdId =
                userController.registerSubscriber(
                        dto.getUsername(),
                        dto.getFirstName(),
                        dto.getLastName(),
                        dto.getPhone(),
                        dto.getEmail(),
                        dto.getRole(),
                        performedBy
                );

        if (createdId == -1) {
            send(client, new ResponseDTO(false, "Subscriber was not created: permission denied or invalid data", null));
            return;
        }

        // ✅ מחזירים רק את ה-ID (Integer) כדי שמסך Manage יוכל להציג הודעה
        send(client, new ResponseDTO(true, "Subscriber created successfully", createdId));
    }

    private void send(ConnectionToClient client, ResponseDTO res) {
        try {
            client.sendToClient(res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
