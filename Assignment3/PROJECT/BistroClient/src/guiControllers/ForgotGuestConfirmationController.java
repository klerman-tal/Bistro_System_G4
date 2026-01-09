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

public class ForgotGuestConfirmationController implements ClientResponseHandler {

    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<LocalTime> timeCombo;
    @FXML private Label lblError;

    private ChatClient chatClient;
    private ClientAPI api;

    public void setClient(ChatClient chatClient) {
        this.chatClient = chatClient;
        if (chatClient != null) {
            this.api = new ClientAPI(chatClient);
            this.chatClient.setResponseHandler(this);
        }
    }

    @FXML
    public void initialize() {
        hideError();

        // Fill half-hour times (00:00..23:30)
        if (timeCombo != null) {
            timeCombo.getItems().clear();
            for (int h = 0; h < 24; h++) {
                timeCombo.getItems().add(LocalTime.of(h, 0));
                timeCombo.getItems().add(LocalTime.of(h, 30));
            }
        }
    }

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

    @FXML
    private void handleCancel(ActionEvent event) {
        closeWindow(event);
    }

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

            // close popup after success
            closeWindowByAnyNode();
        });
    }

    @Override
    public void handleConnectionError(Exception e) {
        Platform.runLater(() -> showError("Server connection lost."));
    }

    @Override
    public void handleConnectionClosed() {}

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    private void closeWindow(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.close();
        } catch (Exception ignored) {}
    }

    private void closeWindowByAnyNode() {
        try {
            if (phoneField != null) {
                Stage stage = (Stage) phoneField.getScene().getWindow();
                stage.close();
            }
        } catch (Exception ignored) {}
    }
}
