package guiControllers;

import application.ChatClient;
import entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MyVisitMenu_BController {

    @FXML private BorderPane rootPane;

    private User user;
    private ChatClient chatClient;

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
    }

    @FXML
    private void onGetTable() {
        openWindow("GetTableChoice_B.fxml", "Get Table");
    }

    @FXML
    private void onPayment() {
        openWindow("Payment_B.fxml", "Payment");
    }

    @FXML
    private void onBack() {
        openWindow("Menu_B.fxml", "Main Menu");
    }

    private void openWindow(String fxml, String title) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/" + fxml));
            Parent root = loader.load();

            Object controller = loader.getController();
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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
