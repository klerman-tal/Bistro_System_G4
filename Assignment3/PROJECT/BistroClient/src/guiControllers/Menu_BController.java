package guiControllers;

import java.io.IOException;

import interfaces.ClientActions;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Menu_BController {

    @FXML private BorderPane rootPane;

    @FXML private Button btnReservation;
    @FXML private Button btnPayment;
    @FXML private Button btnPersonalDetails;
    @FXML private Button btnGetTable;
    @FXML private Button btnRestaurantManagement;
    @FXML private Button btnLogout;

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
            // אם למסך הבא יש setClientActions – מעבירים
            if (controller instanceof RestaurantManagement_BController) {
                ((RestaurantManagement_BController) controller).setClientActions(clientActions);
            }
            // בהמשך תעשי אותו דבר למסכים אחרים שצריכים שרת

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle("Bistro - " + title);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            if (lblMessage != null) {
                lblMessage.setText("Failed to open: " + fxmlName);
                lblMessage.setVisible(true);
                lblMessage.setManaged(true);
            }
        }
    }

    @FXML
    private void onSelectReservationClicked() { openWindow("TableReservation_B.fxml", "Table Reservation"); }

    @FXML
    private void onSelectPaymentClicked() { openWindow("Payment_B.fxml", "Payment"); }

    @FXML
    private void onSelectPersonalDetailsClicked() { openWindow("ClientDetails_B.fxml", "Client Details"); }

    @FXML
    private void onSelectGetTableClicked() { openWindow("GetTable_B.fxml", "Get Table"); }

    @FXML
    private void onSelectRestaurantManagementClicked() {
        openWindow("RestaurantManagement_B.fxml", "Restaurant Management");
    }

    @FXML
    private void onLogoutClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Login_B.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle("Bistro - Login");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            if (lblMessage != null) {
                lblMessage.setText("Failed to open login.");
                lblMessage.setVisible(true);
                lblMessage.setManaged(true);
            }
        }
    }
}
