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

/**
 * JavaFX controller for joining the waiting list and tracking waiting status.
 *
 * <p>This screen allows a user to join the waiting list with a specified number of guests.
 * After joining, the controller can periodically poll the server for waiting status updates
 * and update the UI accordingly. It also supports navigation back to the previous screen
 * while preserving the current session context.</p>
 *
 * <p>The controller communicates with the server through {@link ClientAPI} and processes
 * asynchronous responses via {@link ClientResponseHandler}.</p>
 */
public class JoinWaiting_BController implements ClientResponseHandler {

    @FXML private BorderPane rootPane;
    @FXML private TextField txtGuests;
    @FXML private Button btnJoin;
    @FXML private Label lblInfo;
    @FXML private Label lblError;

    private User user;
    private ChatClient chatClient;
    private ClientActions clientActions;
    private ClientAPI api;

    private String backFxml;

    private String confirmationCode;
    private Timeline pollingTimeline;
    private boolean didShowJoinPopup = false;

    /**
     * Injects the current session context, initializes {@link ClientAPI},
     * and registers this controller as the active response handler.
     *
     * @param user       the current logged-in user
     * @param chatClient the network client used to communicate with the server
     */
    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
        this.api = new ClientAPI(chatClient);
        this.chatClient.setResponseHandler(this);
    }

    /**
     * Injects a {@link ClientActions} implementation for controllers that rely on GUI-to-client actions.
     *
     * @param clientActions the client actions bridge used by downstream controllers
     */
    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    /**
     * Sets the FXML path to navigate back to when the Back button is pressed.
     *
     * @param backFxml the classpath resource path of the previous screen FXML
     */
    public void setBackFxml(String backFxml) {
        this.backFxml = backFxml;
    }

    /**
     * Handles the Join button click.
     *
     * <p>Validates the guest count and sends a join-waiting-list request to the server.</p>
     */
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
        }
    }

    /**
     * Handles the Back button click.
     *
     * <p>Stops polling, clears the response handler, and navigates back to the configured screen
     * while preserving {@link User} and {@link ChatClient} context.</p>
     */
    @FXML
    private void onBackClicked() {
        stopPolling();

        if (chatClient != null) {
            chatClient.setResponseHandler(null);
        }

        if (backFxml == null) {
            System.err.println("JoinWaiting_BController: backFxml not set");
            return;
        }

        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource(backFxml));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller != null) {
                controller.getClass()
                        .getMethod("setClient", User.class, ChatClient.class)
                        .invoke(controller, user, chatClient);
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = stage.getScene();

            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }

            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts polling the server every 10 seconds to refresh the waiting status.
     */
    private void startPollingEvery10Seconds() {
        stopPolling();
        pollingTimeline = new Timeline(
                new KeyFrame(Duration.seconds(10), e -> pollWaitingStatus())
        );
        pollingTimeline.setCycleCount(Timeline.INDEFINITE);
        pollingTimeline.play();
    }

    /**
     * Sends a waiting-status request for the current confirmation code.
     */
    private void pollWaitingStatus() {
        if (api == null || confirmationCode == null || confirmationCode.isBlank())
            return;

        try {
            api.getWaitingStatus(confirmationCode);
        } catch (IOException ignored) {}
    }

    /**
     * Stops polling if an active polling timeline exists.
     */
    private void stopPolling() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
            pollingTimeline = null;
        }
    }

    /**
     * Handles server responses for join and waiting-status operations.
     *
     * <p>On success, updates the waiting state, shows a one-time confirmation popup,
     * optionally starts polling, and updates the UI based on the current waiting status.</p>
     *
     * @param response the response received from the server
     */
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

                confirmationCode = w.getConfirmationCode();

                if (confirmationCode != null && !confirmationCode.isBlank()) {
                    startPollingEvery10Seconds();
                }

                if (!didShowJoinPopup) {
                    didShowJoinPopup = true;
                    showSuccessAlert(
                            "Joined Waiting List",
                            "Your confirmation code is: " + confirmationCode
                    );
                }

                updateUIFromWaiting(w);
                btnJoin.setDisable(false);
            }
        });
    }

    /**
     * Updates the UI according to the provided {@link Waiting} object.
     *
     * <p>If the status is {@link WaitingStatus#Seated}, polling is stopped and a success popup is shown.
     * If a table is ready, an informational message is displayed. Otherwise, the user is informed
     * they are still waiting.</p>
     *
     * @param w the waiting record returned from the server
     */
    private void updateUIFromWaiting(Waiting w) {
        if (w == null) return;

        WaitingStatus status = w.getWaitingStatus();
        Integer tableNum = w.getTableNumber();

        if (status == WaitingStatus.Seated) {
            stopPolling();
            showSuccessAlert(
                    "Table Assigned",
                    "You are seated at table " +
                            (tableNum != null ? tableNum : "") +
                            "\nConfirmation Code: " + w.getConfirmationCode()
            );
            showInfo("You are seated.");
            return;
        }

        if (status == WaitingStatus.Waiting &&
                tableNum != null &&
                w.getTableFreedTime() != null) {
            showInfo("A table is ready! Please arrive within 15 minutes.");
            return;
        }

        showInfo("You are in the waiting list.");
    }

    /**
     * Hides both information and error messages.
     */
    private void hideMessages() {
        lblError.setVisible(false);
        lblError.setManaged(false);
        lblInfo.setVisible(false);
        lblInfo.setManaged(false);
    }

    /**
     * Displays an error message.
     *
     * @param msg the message to display
     */
    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    /**
     * Displays an informational message.
     *
     * @param msg the message to display
     */
    private void showInfo(String msg) {
        lblInfo.setText(msg);
        lblInfo.setVisible(true);
        lblInfo.setManaged(true);
    }

    /**
     * Shows a success information alert dialog.
     *
     * @param title   the alert title
     * @param content the alert content text
     */
    private void showSuccessAlert(String title, String content) {
        javafx.scene.control.Alert alert =
                new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Handles connection errors by stopping polling and displaying an error message.
     *
     * @param e the connection exception
     */
    @Override
    public void handleConnectionError(Exception e) {
        stopPolling();
        Platform.runLater(() -> showError("Connection lost."));
    }

    /**
     * Handles connection closure events by stopping polling.
     */
    @Override
    public void handleConnectionClosed() {
        stopPolling();
    }
}
