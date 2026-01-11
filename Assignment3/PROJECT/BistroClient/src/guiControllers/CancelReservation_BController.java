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

public class CancelReservation_BController implements ClientResponseHandler {

    @FXML private BorderPane rootPane;
    @FXML private TextField txtConfirmationCode;
    @FXML private ComboBox<String> cmbCancelReason;
    @FXML private TextArea txtOtherReason;
    @FXML private Label lblMessage;

    private ChatClient chatClient;
    private User user;

    /* ================= INIT ================= */

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

    /* ================= INJECTION ================= */

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;

        if (chatClient != null) {
            chatClient.setResponseHandler(this); // ✅ המסך הפעיל
        }
    }

    /* ================= ACTIONS ================= */

    @FXML
    private void onCancelReservationClicked() {

        hideMessage();

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    /* ================= BACK ================= */

    @FXML
    private void onBackClicked() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/Menu_B.fxml"));
            Parent root = loader.load();

            Menu_BController menu = loader.getController();
            menu.setClient(user, chatClient); // ✅ שומר את המשתמש

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================= SERVER RESPONSE ================= */

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

    /* ================= UI HELPERS ================= */

    private void showError(String msg) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: red;");
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    private void showSuccess(String msg) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: green;");
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    private void hideMessage() {
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
    }

    @Override public void handleConnectionError(Exception e) {}
    @Override public void handleConnectionClosed() {}
}
