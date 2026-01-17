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
            System.out.println("Loaded controller: " + controller.getClass().getName());

            // Try to call setClient(User, ChatClient)
            try {
                var m = controller.getClass().getDeclaredMethod("setClient", User.class, ChatClient.class);
                m.setAccessible(true);
                m.invoke(controller, user, chatClient);
                System.out.println("setClient(User, ChatClient) invoked successfully");
            } catch (Exception e) {
                System.out.println("Failed to call setClient(User, ChatClient)");
                e.printStackTrace();
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

    private void showMessage(String msg) {
        lblMessage.setText(msg);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }
}