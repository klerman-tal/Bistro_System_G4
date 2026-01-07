package guiControllers;

import interfaces.ClientActions;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class RestaurantManagement_BController {

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

            // מעבירים clientActions רק למי שצריך
            if (controller instanceof UpdateTablesController) {
                ((UpdateTablesController) controller).setClientActions(clientActions);

            } else if (controller instanceof OpeningHoursController) {
                ((OpeningHoursController) controller).setClientActions(clientActions);

            } else if (controller instanceof SelectUser_BController) {
                ((SelectUser_BController) controller).setClientActions(clientActions);

            } else if (controller instanceof ManageReservationController) {
                ((ManageReservationController) controller).setClientActions(clientActions);
            }

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
    private void onUpdateTablesClicked() {
        openWindow("UpdateTables.fxml", "Update Tables");
    }

    @FXML
    private void onUpdateOpeningHoursClicked() {
        openWindow("opening.fxml", "Opening Hours");
    }

    @FXML
    private void onManageUsersClicked() {
        openWindow("selectUser.fxml", "Select User Menu");
    }

    @FXML
    private void onManageReservationClicked() {
        openWindow("ManageReservation.fxml", "Manage Reservations");
    }

    @FXML
    private void onBackToMenuClicked() {
        openWindow("Menu_B.fxml", "Main Menu");
    }

    private void showMessage(String msg) {
        lblMessage.setText(msg);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }
}
