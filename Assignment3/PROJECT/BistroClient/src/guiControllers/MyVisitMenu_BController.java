package guiControllers;

import java.io.IOException;

import application.ChatClient;
import entities.User;
import interfaces.ClientActions;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MyVisitMenu_BController {

    private User user;
    private ChatClient chatClient;
    private ClientActions clientActions;

    @FXML private BorderPane rootPane;

    // כמו כל שאר החלונות אצלך
    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
    }

    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    @FXML
    private void onGetTable() {
        // ✅ במקום Checkin_B.fxml (שלא קיים) -> למסך הבחירה החדש
        openWindow("GetTableChoice_B.fxml", "Get Table");
    }

    @FXML
    private void onPayment() {
        openWindow("Payment_B.fxml", "Payment");
    }

    @FXML
    private void onBack() {
        openWindow("Menu_B.fxml", "Menu");
    }

    // ================= NAVIGATION =================

    private void openWindow(String fxmlName, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
            Parent root = loader.load();

            Object controller = loader.getController();

            // pass clientActions if exists
            if (controller != null && clientActions != null) {
                try {
                    controller.getClass()
                            .getMethod("setClientActions", ClientActions.class)
                            .invoke(controller, clientActions);
                } catch (Exception ignored) {}
            }

            // ✅ Preserve logged-in user + chatClient/session
            if (controller != null && user != null && chatClient != null) {
                try {
                    controller.getClass()
                            .getMethod("setClient", User.class, ChatClient.class)
                            .invoke(controller, user, chatClient);
                } catch (Exception ignored) {}
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle("Bistro - " + title);
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
