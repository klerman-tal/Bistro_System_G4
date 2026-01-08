package guiControllers;

import interfaces.ClientActions;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SelectUser_BController {

    private ClientActions clientActions;

    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
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

    // בדיוק כמו Login_BController — שיטה אחת
    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // העברת clientActions אם יש
            Object controller = loader.getController();
            if (controller != null && clientActions != null) {
                try {
                    controller.getClass()
                              .getMethod("setClientActions", ClientActions.class)
                              .invoke(controller, clientActions);
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
