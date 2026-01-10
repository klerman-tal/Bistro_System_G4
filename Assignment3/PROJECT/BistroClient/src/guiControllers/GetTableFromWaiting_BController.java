package guiControllers;

import application.ChatClient;
import dto.ResponseDTO;
import interfaces.ClientActions;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;
import entities.User;

public class GetTableFromWaiting_BController implements ClientResponseHandler {

    @FXML private BorderPane rootPane;
    @FXML private TextField txtCode;
    @FXML private Button btnConfirm;
    @FXML private Label lblInfo;
    @FXML private Label lblError;

    private User user;
    private ChatClient chatClient;
    private ClientActions clientActions;
    private ClientAPI api;

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
        this.api = new ClientAPI(chatClient);

        // this screen handles responses
        this.chatClient.setResponseHandler(this);
    }

    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    @FXML
    private void onConfirmClicked() {
        hideMessages();

        if (chatClient == null || api == null) {
            showError("Session error. Please login again.");
            return;
        }

        String code = txtCode.getText() == null ? "" : txtCode.getText().trim();

        if (!code.matches("\\d{6}")) {
            showError("Please enter a valid 6-digit confirmation code.");
            return;
        }

        try {
            btnConfirm.setDisable(true);
            api.confirmWaitingArrival(code);
            showInfo("Request sent...");
        } catch (Exception e) {
            btnConfirm.setDisable(false);
            showError("Failed to send request to server.");
            e.printStackTrace();
        }
    }

    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {
            if (response == null) return;

            btnConfirm.setDisable(false);

            if (response.isSuccess()) {
                // server confirms: reservation created + seated
                showSuccessAlert("Success", "You are seated! A reservation was created.");
                showInfo("You are seated.");
                return;
            }

            String msg = response.getMessage() != null ? response.getMessage()
                    : "Arrival confirm failed (not ready / expired / not found).";
            showError(msg);
        });
    }

    @Override
    public void handleConnectionError(Exception e) {
        Platform.runLater(() -> showError("Connection lost."));
    }

    @Override
    public void handleConnectionClosed() { }

    // =========================
    // Back navigation (preserve user + chatClient)
    // =========================

    @FXML
    private void onBackClicked() {
        if (chatClient != null) chatClient.setResponseHandler(null);
        openWindow("GetTableChoice_B.fxml", "Get Table");
    }

    private void openWindow(String fxmlName, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
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

    // =========================
    // UI helpers
    // =========================

    private void hideMessages() {
        lblError.setVisible(false);
        lblError.setManaged(false);
        lblInfo.setVisible(false);
        lblInfo.setManaged(false);
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void showInfo(String msg) {
        lblInfo.setText(msg);
        lblInfo.setVisible(true);
        lblInfo.setManaged(true);
    }

    private void showSuccessAlert(String title, String content) {
        javafx.scene.control.Alert alert =
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
