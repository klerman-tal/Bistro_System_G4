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
    @FXML private Button btnCancelWaiting;
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

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
        this.api = new ClientAPI(chatClient);

        // This screen handles server responses
        this.chatClient.setResponseHandler(this);
    }

    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

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
    private void onCancelWaitingClicked() {
        hideMessages();

        if (confirmationCode == null || confirmationCode.isBlank()) {
            showError("No active waiting to cancel.");
            return;
        }

        try {
            api.cancelWaiting(confirmationCode);
            showInfo("Cancel request sent.");
        } catch (IOException e) {
            showError("Failed to send cancel request.");
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

    // =========================
    // Polling (every 10 seconds) - SILENT
    // =========================

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
        } catch (IOException ignored) {
            // Silent: do not show "checking..." to user
        }
    }

    private void stopPolling() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
            pollingTimeline = null;
        }
    }

    // =========================
    // Server responses
    // =========================

    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {
            if (response == null) return;

            if (response.isSuccess()) {

                Object data = response.getData();

                // Server returns Waiting entity (JOIN / STATUS)
                if (data instanceof Waiting w) {

                    // Save code once
                    if (w.getConfirmationCode() != null && !w.getConfirmationCode().isBlank()) {
                        this.confirmationCode = w.getConfirmationCode();
                    }

                    // After successful join, start polling (silent)
                    if (this.confirmationCode != null && !this.confirmationCode.isBlank()) {
                        startPollingEvery10Seconds();
                    }

                    // ✅ Show popup ONCE after join (not on every status response)
                    if (!didShowJoinPopup && this.confirmationCode != null && !this.confirmationCode.isBlank()) {
                        didShowJoinPopup = true;
                        showSuccessAlert("Joined Waiting List",
                                "Your confirmation code is: " + this.confirmationCode);
                    }

                    updateUIFromWaiting(w);
                    btnJoin.setDisable(false);
                    return;
                }

                // Fallback: server returns String code
                if (data instanceof String code) {
                    this.confirmationCode = code;
                    btnJoin.setDisable(false);

                    if (!didShowJoinPopup) {
                        didShowJoinPopup = true;
                        showSuccessAlert("Joined Waiting List",
                                "Your confirmation code is: " + code);
                    }

                    startPollingEvery10Seconds();
                    showActionButtons(true);
                    showInfo("You are in the waiting list.");
                    return;
                }

                btnJoin.setDisable(false);
                showInfo(response.getMessage() != null ? response.getMessage() : "Success.");
                return;
            }

            // Failure
            btnJoin.setDisable(false);
            String msg = response.getMessage() != null ? response.getMessage() : "Request failed.";
            showError(msg);
        });
    }

    private void updateUIFromWaiting(Waiting w) {
        if (w == null) return;

        WaitingStatus status = w.getWaitingStatus();
        Integer tableNum = w.getTableNumber();

        // Seated
        if (status == WaitingStatus.Seated) {
            stopPolling();
            showActionButtons(false);

            if (tableNum != null) {
                showSuccessAlert("Table Assigned",
                        "You are seated at table " + tableNum +
                        ".\nConfirmation Code: " + w.getConfirmationCode());
            } else {
                showSuccessAlert("Table Assigned",
                        "You are seated.\nConfirmation Code: " + w.getConfirmationCode());
            }
            showInfo("You are seated.");
            return;
        }

        // Cancelled / expired
        if (status == WaitingStatus.Cancelled) {
            stopPolling();
            showActionButtons(false);
            showError("Waiting was cancelled (or expired).");
            return;
        }

        // Waiting + table is ready (15 minutes)
        if (status == WaitingStatus.Waiting && tableNum != null && w.getTableFreedTime() != null) {
            showActionButtons(true);
            showInfo("A table is ready! Table " + tableNum +
                     ". Please arrive within 15 minutes and press “I’m here”.");
            return;
        }

        // Normal waiting
        showActionButtons(true);
        showInfo("You are in the waiting list.");
    }

    private void showActionButtons(boolean show) {
        btnCancelWaiting.setVisible(show);
        btnCancelWaiting.setManaged(show);

        btnImHere.setVisible(show);
        btnImHere.setManaged(show);
    }

    @Override
    public void handleConnectionError(Exception e) {
        Platform.runLater(() -> showError("Connection lost."));
        stopPolling();
    }

    @Override
    public void handleConnectionClosed() {
        stopPolling();
    }

    // =========================
    // Back navigation (preserve user + chatClient)
    // =========================

    @FXML
    private void onBackClicked() {
        stopPolling();
        if (chatClient != null) chatClient.setResponseHandler(null);
        openMenu();
    }

    private void openMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Menu_B.fxml"));
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
            stage.setTitle("Bistro - Main Menu");
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
