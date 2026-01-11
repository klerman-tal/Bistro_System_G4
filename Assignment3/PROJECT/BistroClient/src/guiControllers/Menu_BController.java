package guiControllers;

import java.io.IOException;

import application.ChatClient;
import entities.User;
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
    @FXML private Button btnCancelReservation; // ✅ NEW
    @FXML private Button btnPayment;
    @FXML private Button btnPersonalDetails;
    @FXML private Button btnGetTable;
    @FXML private Button btnJoinWaiting;
    @FXML private Button btnRestaurantManagement;
    @FXML private Button btnLogout;

    @FXML private Label lblMessage;

    private User user;
    private ChatClient chatClient;
    private ClientActions clientActions;

    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
    }

    @FXML
    private void onSelectReservationClicked() {
        openWindow("TableReservation_B.fxml", "Table Reservation");
    }

    // ✅ NEW
    @FXML
    private void onSelectCancelReservationClicked() {
        openWindow("CancelReservation_B.fxml", "Cancel Reservation");
    }

    @FXML
    private void onSelectPaymentClicked() {
        openWindow("Payment_B.fxml", "Payment");
    }

    @FXML
    private void onSelectPersonalDetailsClicked() {
        openWindow("ClientDetails_B.fxml", "Client Details");
    }

    @FXML
    private void onSelectGetTableClicked() {
        openWindow("GetTableChoice_B.fxml", "Get Table");
    }

    @FXML
    private void onSelectJoinWaitingClicked() {
        openWindow("JoinWaiting_B.fxml", "Join Waiting List");
    }

    @FXML
    private void onSelectRestaurantManagementClicked() {
        openWindow("RestaurantManagement_B.fxml", "Restaurant Management");
    }

    @FXML
    private void onLogoutClicked() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/Login_B.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle("Bistro - Login");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openWindow(String fxmlName, String title) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
            Parent root = loader.load();

            Object controller = loader.getController();

            if (controller != null && clientActions != null) {
                try {
                    controller.getClass()
                            .getMethod("setClientActions", ClientActions.class)
                            .invoke(controller, clientActions);
                } catch (Exception ignored) {}
            }

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
        }
    }
}
