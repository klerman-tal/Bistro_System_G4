package guiControllers;

import application.ChatClient;
import entities.User;
import interfaces.ClientActions;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class ReservationMenu_BController {

    @FXML
    private BorderPane rootPane;

    private User user;
    private ChatClient chatClient;
    private ClientActions clientActions;

    /* =======================
       SETTERS (חשוב!)
       ======================= */

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
    }

    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    /* =======================
       BUTTON ACTIONS
       ======================= */

    @FXML
    private void onCreateReservation() {
        openWindow("TableReservation_B.fxml", "Create Reservation");
    }

    @FXML
    private void onCancelReservation() {
        openWindow("CancelReservation_B.fxml", "Cancel Reservation");
    }

    @FXML
    private void onJoinWaiting() {
        openWindow("JoinWaiting_B.fxml", "Join Waiting List");
    }
    
    @FXML
    private void onCancelWaiting() {
        openWindow("CancelWaiting_B.fxml", "Cancel Waiting List");
    }


    @FXML
    private void onBack() {
        openWindow("Menu_B.fxml", "Main Menu");
    }

    /* =======================
       NAVIGATION (אחיד)
       ======================= */

    private void openWindow(String fxmlName, String title) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
            Parent root = loader.load();

            Object controller = loader.getController();

            // העברת ClientActions
            if (controller != null && clientActions != null) {
                try {
                    controller.getClass()
                            .getMethod("setClientActions", ClientActions.class)
                            .invoke(controller, clientActions);
                } catch (Exception ignored) {}
            }

            // העברת User + ChatClient
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
