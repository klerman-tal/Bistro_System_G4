package guiControllers;

import application.ChatClient;
import dto.ResponseDTO;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * JavaFX controller for recovering a guest reservation confirmation code.
 *
 * <p>This controller collects guest contact details (phone and/or email) along with the reservation
 * date and time, sends a recovery request to the server via {@link ClientAPI}, and displays the
 * recovered confirmation code in an information dialog.</p>
 *
 * <p>The controller implements {@link ClientResponseHandler} to handle asynchronous responses and
 * connection events from the server.</p>
 */
public class ForgotGuestConfirmationController implements ClientResponseHandler {

    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<LocalTime> timeCombo;
    @FXML private Label lblError;

    private ChatClient chatClient;
    private ClientAPI api;

    /**
     * Injects the {@link ChatClient} into this controller, initializes {@link ClientAPI},
     * and registers this controller as the active response handler.
     *
     * @param chatClient the network client used to send requests
     */
    public void setClient(ChatClient chatClient) {
        this.chatClient = chatClient;
        if (chatClient != null) {
            this.api = new ClientAPI(chatClient);
            this.chatClient.setResponseHandler(this);
        }
    }

    /**
     * Initializes the controller after FXML injection.
     *
     * <p>Clears the error label and populates the time combo-box with half-hour increments
     * for the full day (00:00..23:30).</p>
     */
    @FXML
    public void initialize() {
        hideError();

        if (timeCombo != null) {
            timeCombo.getItems().clear();
            for (int h = 0; h < 24; h++) {
                timeCombo.getItems().add(LocalTime.of(h, 0));
                timeCombo.getItems().add(LocalTime.of(h, 30));
            }
        }
    }

    /**
     * Handles the Recover action.
     *
     * <p>Validates the input fields and sends a guest confirmation-code recovery request to the server.</p>
     *
     * @param event the JavaFX action event
     */
    @FXML
    private void handleRecover(ActionEvent event) {
        hideError();

        String phone = phoneField.getText() != null ? phoneField.getText().trim() : "";
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        LocalDate date = datePicker.getValue();
        LocalTime time = timeCombo.getValue();

        if (phone.isEmpty() && email.isEmpty()) {
            showError("Please enter a phone number or an email.");
            return;
        }

        if (!email.isEmpty() && (!email.contains("@") || !email.contains("."))) {
            showError("Please enter a valid email address.");
            return;
        }

        if (date == null) {
            showError("Please select reservation date.");
            return;
        }

        if (time == null) {
            showError("Please select reservation time.");
            return;
        }

        if (api == null) {
            showError("Client is not initialized.");
            return;
        }

        LocalDateTime reservationDateTime = LocalDateTime.of(date, time);

        try {
            api.recoverGuestConfirmationCode(phone, email, reservationDateTime);
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (IOException e) {
            showError("Connection error.");
        }
    }

    /**
     * Handles the Cancel action by closing the current window.
     *
     * @param event the JavaFX action event
     */
    @FXML
    private void handleCancel(ActionEvent event) {
        closeWindow(event);
    }

    /**
     * Handles a server response for the guest confirmation-code recovery request.
     *
     * <p>On success, displays the recovered confirmation code and closes the window.
     * On failure, displays an error message.</p>
     *
     * @param response the response received from the server
     */
    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {
            if (response == null) {
                showError("Server returned empty response.");
                return;
            }

            if (!response.isSuccess()) {
                showError(response.getMessage() != null ? response.getMessage() : "Recovery failed.");
                return;
            }

            Object data = response.getData();
            String code = (data == null) ? "" : String.valueOf(data);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Confirmation Code Recovered");
            alert.setHeaderText("Your confirmation code:");
            alert.setContentText(code);
            alert.showAndWait();

            closeWindowByAnyNode();
        });
    }

    /**
     * Handles connection errors by showing an error message on the UI thread.
     *
     * @param e the connection exception
     */
    @Override
    public void handleConnectionError(Exception e) {
        Platform.runLater(() -> showError("Server connection lost."));
    }

    /**
     * Handles connection closure events (no UI action defined in this controller).
     */
    @Override
    public void handleConnectionClosed() {}

    /**
     * Displays an error message in the UI.
     *
     * @param msg the message to display
     */
    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    /**
     * Hides the error label area and clears its text.
     */
    private void hideError() {
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    /**
     * Closes the current window using the given action event source node.
     *
     * @param event the JavaFX action event
     */
    private void closeWindow(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.close();
        } catch (Exception ignored) {}
    }

    /**
     * Closes the current window using an available injected node (phoneField) as an anchor.
     */
    private void closeWindowByAnyNode() {
        try {
            if (phoneField != null) {
                Stage stage = (Stage) phoneField.getScene().getWindow();
                stage.close();
            }
        } catch (Exception ignored) {}
    }
}
