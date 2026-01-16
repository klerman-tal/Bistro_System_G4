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

    // ⬅️ FXML לחזרה
    private String backFxml;

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

    public void setConfirmationCode(String code) {
        txtConfirmationCode.setText(code);
    }

    /* ================= CONTEXT ================= */

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
        if (chatClient != null) {
            chatClient.setResponseHandler(this);
        }
    }
    
    /* גרסה נוספת שמאפשרת לקבל קוד אישור באופן אוטומטי מהטבלה */
    public void setClient(User user, ChatClient chatClient, String confirmationCode) {
        // קריאה למתודה המקורית שכבר כתבת כדי לשמור על הלוגיקה הקיימת
        setClient(user, chatClient); 
        
        // מילוי אוטומטי של שדה הטקסט בקוד שהגיע מהטבלה
        if (confirmationCode != null && txtConfirmationCode != null) {
            txtConfirmationCode.setText(confirmationCode);
        }
    }

    public void setBackFxml(String backFxml) {
        this.backFxml = backFxml;
    }

    /* ================= ACTIONS ================= */

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

    /* ================= BACK ================= */

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
            stage.setScene(new Scene(root));
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
