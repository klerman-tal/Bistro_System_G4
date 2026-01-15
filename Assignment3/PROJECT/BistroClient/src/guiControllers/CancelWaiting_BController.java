package guiControllers;

import java.io.IOException;
import application.ChatClient;
import dto.ResponseDTO;
import entities.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

public class CancelWaiting_BController implements ClientResponseHandler {

    @FXML private BorderPane rootPane;
    @FXML private TextField txtConfirmationCode;
    @FXML private Label lblMessage;

    private User user;
    private ChatClient chatClient;
    private ClientAPI api;

    /* ================= INJECTION ================= */

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
        this.api = new ClientAPI(chatClient);
        if (chatClient != null) {
            chatClient.setResponseHandler(this);
        }
    }
    
    // המתודה שפותרת את השגיאה ב-ManageWaitingListController
    public void setClient(User user, ChatClient chatClient, String confirmationCode) {
        setClient(user, chatClient); 
        if (confirmationCode != null && txtConfirmationCode != null) {
            txtConfirmationCode.setText(confirmationCode);
        }
    }

    /* ================= ACTIONS ================= */

    @FXML
    private void onCancelWaitingClicked() {
        hideMessage();
        String code = txtConfirmationCode.getText();
        if (code == null || code.isBlank()) {
            showError("Confirmation code is required.");
            return;
        }
        try {
            api.cancelWaiting(code.trim());
            showInfo("Cancel request sent.");
        } catch (IOException e) {
            showError("Failed to send cancel request.");
            e.printStackTrace();
        }
    }

    @FXML
    private void onBackClicked() {
        try {
            // מעודכן: חוזר למסך ניהול הרשימה (הטבלה)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ManageWaitingList.fxml"));
            Parent root = loader.load();
            ManageWaitingListController controller = loader.getController();
            controller.setClient(user, chatClient);
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    /* ================= SERVER RESPONSE ================= */

    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {
            if (response == null) return;
            if (response.isSuccess()) {
                showSuccess(response.getMessage() != null ? response.getMessage() : "Cancelled successfully.");
            } else {
                showError(response.getMessage() != null ? response.getMessage() : "Cancel failed.");
            }
        });
    }

    private void showError(String msg) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: red;");
        lblMessage.setVisible(true);
    }

    private void showSuccess(String msg) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: green;");
        lblMessage.setVisible(true);
    }

    private void showInfo(String msg) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: #2e7d32;");
        lblMessage.setVisible(true);
    }

    private void hideMessage() { lblMessage.setVisible(false); }
    @Override public void handleConnectionError(Exception e) {}
    @Override public void handleConnectionClosed() {}
}