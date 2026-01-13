package guiControllers;

import java.io.IOException;

import application.ChatClient;
import dto.ResponseDTO;
import entities.User;
import entities.Waiting;
import entities.Enums.WaitingStatus;
import interfaces.ClientActions;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.util.Duration;
import network.ClientAPI;
import network.ClientResponseHandler;

public class JoinWaiting_BController implements ClientResponseHandler {

    @FXML private BorderPane rootPane;
    @FXML private TextField txtGuests;
    @FXML private Button btnJoin;
    @FXML private Button btnImHere;
    @FXML private Label lblInfo;
    @FXML private Label lblError;

    private User user;
    private ChatClient chatClient;
    private ClientActions clientActions;
    private ClientAPI api;

    private String confirmationCode;
    private Timeline pollingTimeline;

    // UI state helpers
    private boolean didShowJoinPopup = false;

    /* ================= INJECTION ================= */

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
        this.api = new ClientAPI(chatClient);
        this.chatClient.setResponseHandler(this);
    }

    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    /* ================= ACTIONS ================= */

    @FXML
    private void onJoinClicked() {
        hideMessages();

        if (user == null || chatClient == null || api == null) {
            showError("Session error. Please login again.");
            return;
        }

        int guests;
        try {
            guests = Integer.parseInt(txtGuests.getText().trim());
        } catch (Exception e) {
            showError("Please enter a valid number of guests.");
            return;
        }

        if (guests <= 0) {
            showError("Guests must be at least 1.");
            return;
        }

        try {
            api.joinWaitingList(guests, user);
            btnJoin.setDisable(true);
            showInfo("Request sent.");

        } catch (IOException e) {
            btnJoin.setDisable(false);
            showError("Failed to send request to server.");
            e.printStackTrace();
        }
    }

    @FXML
    private void onImHereClicked() {
        hideMessages();

        if (confirmationCode == null || confirmationCode.isBlank()) {
            showError("Missing confirmation code.");
            return;
        }

        try {
            api.confirmWaitingArrival(confirmationCode);
            showInfo("Arrival confirmation sent.");
        } catch (IOException e) {
            showError("Failed to confirm arrival.");
            e.printStackTrace();
        }
    }

    /* ================= POLLING ================= */

    private void startPollingEvery10Seconds() {
        stopPolling();
        pollingTimeline = new Timeline(
                new KeyFrame(Duration.seconds(10), e -> pollWaitingStatus())
        );
        pollingTimeline.setCycleCount(Timeline.INDEFINITE);
        pollingTimeline.play();
    }

    private void pollWaitingStatus() {
        if (api == null || confirmationCode == null || confirmationCode.isBlank())
            return;

        try {
            api.getWaitingStatus(confirmationCode);
        } catch (IOException ignored) {}
    }

    private void stopPolling() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
            pollingTimeline = null;
        }
    }

    /* ================= SERVER RESPONSES ================= */

    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {
            if (response == null) return;

            if (!response.isSuccess()) {
                btnJoin.setDisable(false);
                showError(response.getMessage() != null
                        ? response.getMessage()
                        : "Request failed.");
                return;
            }

            Object data = response.getData();

            if (data instanceof Waiting w) {

                if (w.getConfirmationCode() != null && !w.getConfirmationCode().isBlank()) {
                    this.confirmationCode = w.getConfirmationCode();
                }

                if (this.confirmationCode != null && !this.confirmationCode.isBlank()) {
                    startPollingEvery10Seconds();
                }

                if (!didShowJoinPopup) {
                    didShowJoinPopup = true;
                    showSuccessAlert("Joined Waiting List",
                            "Your confirmation code is: " + this.confirmationCode);
                }

                updateUIFromWaiting(w);
                btnJoin.setDisable(false);
                return;
            }

            if (data instanceof String code) {
                this.confirmationCode = code;
                btnJoin.setDisable(false);

                if (!didShowJoinPopup) {
                    didShowJoinPopup = true;
                    showSuccessAlert("Joined Waiting List",
                            "Your confirmation code is: " + code);
                }

                startPollingEvery10Seconds();
                showInfo("You are in the waiting list.");
            }
        });
    }

    private void updateUIFromWaiting(Waiting w) {
        if (w == null) return;

        WaitingStatus status = w.getWaitingStatus();
        Integer tableNum = w.getTableNumber();

        if (status == WaitingStatus.Seated) {
            stopPolling();
            showSuccessAlert("Table Assigned",
                    "You are seated at table " +
                    (tableNum != null ? tableNum : "") +
                    ".\nConfirmation Code: " + w.getConfirmationCode());
            showInfo("You are seated.");
            return;
        }

        if (status == WaitingStatus.Waiting && tableNum != null && w.getTableFreedTime() != null) {
            showInfo("A table is ready! Please arrive within 15 minutes and press “I’m here”.");
            return;
        }

        showInfo("You are in the waiting list.");
    }

    /* ================= BACK ================= */

    @FXML
    private void onBackClicked() {
        stopPolling();
        if (chatClient != null) {
            chatClient.setResponseHandler(null);
        }

        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/ReservationMenu_B.fxml"));
            Parent root = loader.load();

            ReservationMenu_BController controller =
                    loader.getController();

            if (controller != null) {
                controller.setClient(user, chatClient);
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================= UI HELPERS ================= */

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

    @Override public void handleConnectionError(Exception e) {
        stopPolling();
        Platform.runLater(() -> showError("Connection lost."));
    }

    @Override public void handleConnectionClosed() {
        stopPolling();
    }
}
