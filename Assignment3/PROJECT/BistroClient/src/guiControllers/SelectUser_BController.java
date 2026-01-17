package guiControllers;

import application.ChatClient;
import entities.User;
import interfaces.ClientActions;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class SelectUser_BController {

    @FXML
    private BorderPane rootPane;

    private ClientActions clientActions;

    // ===== SESSION =====
    private User user;
    private ChatClient chatClient;

    /* ================= SETTERS ================= */

    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    // נקרא מ־RestaurantManagement_BController
    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
    }

    /* ================= BUTTONS ================= */

    @FXML
    private void onSubscribersClicked() {
        openWindow("manageSubscriber.fxml", "Manage Subscribers");
    }

    @FXML
    private void onGuestsClicked() {
        openWindow("manegeGustsGui.fxml", "Manage Guests");
    }

    @FXML
    private void onBackToMenuClicked() {
        openWindow("RestaurantManagement_B.fxml", "Restaurant Management");
    }

    /* ================= NAVIGATION ================= */

    private void openWindow(String fxmlName, String title) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
            Parent root = loader.load();

            Object controller = loader.getController();

            // העברת ClientActions (אם קיים)
            if (controller != null && clientActions != null) {
                try {
                    controller.getClass()
                            .getMethod("setClientActions", ClientActions.class)
                            .invoke(controller, clientActions);
                } catch (Exception ignored) {}
            }

            // העברת session (User + ChatClient)
            if (controller != null && user != null && chatClient != null) {
                try {
                    controller.getClass()
                            .getMethod("setClient", User.class, ChatClient.class)
                            .invoke(controller, user, chatClient);
                } catch (Exception ignored) {}
            }

            switchRoot(root, "Bistro - " + title);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================= SCENE HANDLING ================= */

    private void switchRoot(Parent root, String title) {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        Scene scene = stage.getScene();

        if (scene == null) {
            stage.setScene(new Scene(root));
        } else {
            scene.setRoot(root);
        }

        stage.setTitle(title);
        stage.setMaximized(true);
        stage.show();
    }
}
