package guiControllers;

import java.io.IOException;

import application.ChatClient;
import entities.User;
import interfaces.ClientActions;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SelectUser_BController {

    private ClientActions clientActions;

    // ✅ session
    private User user;
    private ChatClient chatClient;

    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    // ✅ נקרא מ-RestaurantManagement_BController
    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
    }

    @FXML
    private void onSubscribersClicked(ActionEvent event) {
        navigateTo(event, "/gui/manageSubscriber.fxml");
    }

    @FXML
    private void onGuestsClicked(ActionEvent event) {
        navigateTo(event, "/gui/manegeGustsGui.fxml");
    }

    @FXML
    private void onBackToMenuClicked(ActionEvent event) {
        navigateTo(event, "/gui/RestaurantManagement_B.fxml");
    }

    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();

            // להעביר clientActions (כמו שהיה לך)
            if (controller != null && clientActions != null) {
                try {
                    controller.getClass()
                            .getMethod("setClientActions", ClientActions.class)
                            .invoke(controller, clientActions);
                } catch (Exception ignored) {}
            }

            // ✅ להעביר user + chatClient למסך הבא (ManageSubscriber צריך את זה)
            if (controller != null && user != null && chatClient != null) {
                try {
                    controller.getClass()
                            .getMethod("setClient", User.class, ChatClient.class)
                            .invoke(controller, user, chatClient);
                } catch (Exception ignored) {}
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();

            System.out.println("Switched to: " + fxmlPath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}