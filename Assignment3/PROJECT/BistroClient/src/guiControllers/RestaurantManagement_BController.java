package guiControllers;

import application.ChatClient;
import entities.User;
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

    // ✅ session
    private User user;
    private ChatClient chatClient;

    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    // ✅ נקרא מה-Menu_BController
    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
    }

    private void openWindow(String fxmlName, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
            Parent root = loader.load();

            Object controller = loader.getController();

            // pass clientActions where needed (כמו שהיה לך)
            if (controller instanceof UpdateTablesController) {
                ((UpdateTablesController) controller).setClientActions(clientActions);

            } else if (controller instanceof OpeningHoursController) {
                ((OpeningHoursController) controller).setClientActions(clientActions);

            } else if (controller instanceof SelectUser_BController) {
                ((SelectUser_BController) controller).setClientActions(clientActions);

            } else if (controller instanceof ManageReservationController) {
                ((ManageReservationController) controller).setClientActions(clientActions);

            } else if (controller instanceof ReportsMenuController) {
                ((ReportsMenuController) controller).setClientActions(clientActions);
            }

            // ✅ להעביר גם user+chatClient למי שצריך (במיוחד SelectUser)
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
