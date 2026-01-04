package guiControllers;

import interfaces.ClientActions;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class SelectUser_BController {

    @FXML private BorderPane rootPane;
    @FXML private Label lblMessage;

    private ClientActions clientActions;

    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    private void openWindow(String fxmlName, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
            Parent root = loader.load();

            Object controller = loader.getController();

            // להעביר clientActions הלאה למסכים הבאים/אחורה
          /*  if (controller instanceof RestaurantManagement_BController) {
                ((RestaurantManagement_BController) controller).setClientActions(clientActions);
            } else if (controller instanceof SubscribersController) {
                ((SubscribersController) controller).setClientActions(clientActions);
            } else if (controller instanceof GuestsController) {
                ((GuestsController) controller).setClientActions(clientActions);
            }*/

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle("Bistro - " + title);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Failed to open: " + fxmlName);
        }
    }

    @FXML
    private void onSubscribersClicked() {
        // תחליפי לשם ה־FXML האמיתי שלך למסך ניהול מנויים
        openWindow("Subscribers.fxml", "Subscribers");
    }

    @FXML
    private void onGuestsClicked() {
        // תחליפי לשם ה־FXML האמיתי שלך למסך ניהול אורחים
        openWindow("Guests.fxml", "Guests");
    }

    @FXML
    private void onBackToMenuClicked() {
        // חזרה למסך הקודם
        openWindow("RestaurantManagement.fxml", "Restaurant Management");
    }

    private void showMessage(String msg) {
        if (lblMessage != null) {
            lblMessage.setText(msg);
            lblMessage.setVisible(true);
            lblMessage.setManaged(true);
        }
    }
}
