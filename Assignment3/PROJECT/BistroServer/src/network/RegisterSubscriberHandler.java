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

        // 1️⃣ בדיקת DTO נכון
        Object dataObj = request.getData();
        if (!(dataObj instanceof RegisterSubscriberDTO dto)) {
            send(client, new ResponseDTO(false, "Invalid register data", null));
            return;
        }

        // 2️⃣ מי מבצע את הפעולה (Agent / Manager)
        Object performerObj = client.getInfo("user");
        if (!(performerObj instanceof Subscriber performedBy)) {
            send(client, new ResponseDTO(false, "Unauthorized", null));
            return;
        }

        // 3️⃣ קריאה נכונה ל־UserController
        Subscriber created =
                userController.registerSubscriber(
                        dto.getUsername(),
                        dto.getFirstName(),
                        dto.getLastName(),
                        dto.getPhone(),
                        dto.getEmail(),
                        dto.getRole(),
                        performedBy
                );

        if (created == null) {
            send(client, new ResponseDTO(
                    false,
                    "Permission denied or invalid data",
                    null
            ));
            return;
        }

        // 4️⃣ הצלחה
        send(client, new ResponseDTO(
                true,
                "User created successfully",
                created
        ));
    }

    private void send(ConnectionToClient client, ResponseDTO res) {
        try {
            client.sendToClient(res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
