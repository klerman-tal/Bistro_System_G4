package guiControllers;

import application.ChatClient;
import entities.User;
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

    private User user;
    private ChatClient chatClient;

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
    }

    private void openWindow(String fxmlName, String title) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
            Parent root = loader.load();

            Object controller = loader.getController();

            // ✅ Update Tables
            if (controller instanceof UpdateTablesController utc) {
                utc.setClient(user, chatClient); // ✅ היה רק chatClient
            }

            // ✅ Opening Hours
            if (controller instanceof OpeningHoursController ohc) {
                ohc.setClient(user, chatClient);
            }

            // ✅ Select user menu
            if (controller instanceof SelectUser_BController suc) {
                suc.setClient(user, chatClient);
            }

            // ✅ Manage reservations
            if (controller instanceof ManageReservationController mrc) {
                mrc.setClient(user, chatClient);
            }
            
            // ✅ ⭐⭐ THIS WAS MISSING ⭐⭐
            if (controller instanceof ReportsMenuController rmc) {
                rmc.setClient(user, chatClient);
            }
            
            // ✅ Back to Menu
            if (controller instanceof Menu_BController menu) {
                menu.setClient(user, chatClient);
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
    private void onReportsClicked() {
        openWindow("ReportsMenu.fxml", "Reports");
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
