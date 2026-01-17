package guiControllers;

import java.io.IOException;

import application.ChatClient;
import dto.CancelReservationDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import network.ClientResponseHandler;
import protocol.Commands;

/**
 * JavaFX controller for the "Cancel Reservation" screen.
 *
 * <p>This controller collects a reservation confirmation code and a cancellation reason,
 * builds a {@link CancelReservationDTO}, and sends a {@link Commands#CANCEL_RESERVATION}
 * request to the server via {@link ChatClient}. It also implements {@link ClientResponseHandler}
 * to display server responses on the UI thread.</p>
 */
public class CancelReservation_BController implements ClientResponseHandler {

    @FXML private BorderPane rootPane;
    @FXML private TextField txtConfirmationCode;
    @FXML private ComboBox<String> cmbCancelReason;
    @FXML private TextArea txtOtherReason;
    @FXML private Label lblMessage;

    private ChatClient chatClient;
    private User user;

    private String backFxml;

    /**
     * Initializes the controller after FXML injection.
     *
     * <p>Populates the cancellation reason combo-box with predefined options.</p>
     */
    @FXML
    public void initialize() {
        cmbCancelReason.getItems().addAll(
                "Changed my plans",
                "Found another restaurant",
                "Running late",
                "No longer needed",
                "Other"
        );
    }

    /**
     * Prefills the confirmation code input field.
     *
     * @param code the confirmation code to display
     */
    public void setConfirmationCode(String code) {
        txtConfirmationCode.setText(code);
    }

    /**
     * Sets the current user and client instance for this screen and registers this controller
     * as the active response handler.
     *
     * @param user       the current logged-in user
     * @param chatClient the network client used to send requests
     */
    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
        if (chatClient != null) {
            chatClient.setResponseHandler(this);
        }
    }

    /**
     * Sets the current user and client instance and optionally pre-fills the confirmation code
     * (e.g., when navigating from a table selection).
     *
     * @param user             the current logged-in user
     * @param chatClient       the network client used to send requests
     * @param confirmationCode the confirmation code to prefill in the UI
     */
    public void setClient(User user, ChatClient chatClient, String confirmationCode) {
        setClient(user, chatClient);

        if (confirmationCode != null && txtConfirmationCode != null) {
            txtConfirmationCode.setText(confirmationCode);
        }
    }

    /**
     * Defines which FXML should be loaded when the user clicks the Back button.
     *
     * @param backFxml the FXML resource path to navigate back to
     */
    public void setBackFxml(String backFxml) {
        this.backFxml = backFxml;
    }

    /**
     * Handles the Cancel Reservation button click.
     *
     * <p>Validates input, builds a cancellation DTO, and sends the request to the server.</p>
     */
    @FXML
    private void onCancelReservationClicked() {

        hideMessage();

        if (chatClient == null) {
            showError("Session error. Please reopen the screen.");
            return;
        }

        String code = txtConfirmationCode.getText();
        if (code == null || code.isBlank()) {
            showError("Confirmation code is required");
            return;
        }

        String reason = cmbCancelReason.getValue();
        String other = txtOtherReason.getText();

        String finalReason =
                (reason != null ? reason : "") +
                ((other != null && !other.isBlank()) ? " - " + other : "");

        CancelReservationDTO data =
                new CancelReservationDTO(code.trim(), finalReason);

        RequestDTO request =
                new RequestDTO(Commands.CANCEL_RESERVATION, data);

        try {
            chatClient.sendToServer(request);
        } catch (IOException e) {
            showError("Failed to send request");
        }
    }

    /**
     * Handles the Back button click by loading the configured FXML and replacing the current scene root.
     *
     * <p>If the target controller has a {@code setClient(User, ChatClient)} method, it will be invoked
     * reflectively to preserve the session context.</p>
     */
    @FXML
    private void onBackClicked() {
        if (backFxml == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(backFxml));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller != null && user != null && chatClient != null) {
                try {
                    controller.getClass()
                            .getMethod("setClient", User.class, ChatClient.class)
                            .invoke(controller, user, chatClient);
                } catch (Exception ignored) {}
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
     * Handles a server response and updates the UI accordingly.
     *
     * @param response the response received from the server
     */
    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {
            if (response.isSuccess()) {
                showSuccess(response.getMessage());
            } else {
                showError(response.getMessage());
            }
        });
    }

    /**
     * Displays an error message in the UI.
     *
     * @param msg the message to display
     */
    private void showError(String msg) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: red;");
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    /**
     * Displays a success message in the UI.
     *
     * @param msg the message to display
     */
    private void showSuccess(String msg) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: green;");
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    /**
     * Hides the message label area.
     */
    private void hideMessage() {
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
    }

    /**
     * Handles connection errors (no UI action defined in this controller).
     *
     * @param e the connection exception
     */
    @Override
    public void handleConnectionError(Exception e) {}

    /**
     * Handles connection closure events (no UI action defined in this controller).
     */
    @Override
    public void handleConnectionClosed() {}
}
