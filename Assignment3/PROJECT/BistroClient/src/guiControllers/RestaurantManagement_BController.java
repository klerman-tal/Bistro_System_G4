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

    /* ================= SETTER ================= */

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
    }

    /* ================= NAVIGATION ================= */

    private void openWindow(String fxmlName, String title) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
            Parent root = loader.load();

            Object controller = loader.getController();

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
            showMessage("Failed to open: " + fxmlName);
        }
    }

    /* ================= BUTTONS ================= */

    @FXML
    private void onUpdateTablesClicked() {
        openWindow("UpdateTables.fxml", "Update Tables");
    }

    @FXML
    private void onWaitingListClicked() {
        openWindow("ManageWaitingList.fxml", "Waiting List");
    }

    @FXML
    private void onUpdateOpeningHoursClicked() {
        openWindow("opening.fxml", "Opening Hours");
    }

    @FXML
    private void onManageUsersClicked() {
        openWindow("manageSubscriber.fxml", "Manage Subscribers");
    }

    @FXML
    private void onManageReservationClicked() {
        openWindow("ManageReservation.fxml", "Manage Reservations");
    }

    @FXML
    private void onManageCurrentDinersClicked() {
        openWindow("ManageCurrentDiners.fxml", "Current Diners");
    }

    @FXML
    private void onReportsClicked() {
        openWindow("ReportsMenu.fxml", "Reports");
    }

    @FXML
    private void onBackToMenuClicked() {
        openWindow("Menu_B.fxml", "Main Menu");
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

    /* ================= UI ================= */

    private void showMessage(String msg) {
        lblMessage.setText(msg);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }
}
