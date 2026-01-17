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

    /* =======================
       FXML
       ======================= */
    @FXML private BorderPane rootPane;
    @FXML private TextField txtConfirmationCode;
    @FXML private Label lblMessage;

    /* =======================
       Context
       ======================= */
    private User user;
    private ChatClient chatClient;
    private ClientAPI api;

    /* =======================
       Navigation
       ======================= */
    private String backFxml;

    /* =======================
       Injection
       ======================= */
    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
        this.api = new ClientAPI(chatClient);

        if (chatClient != null) {
            chatClient.setResponseHandler(this);
        }
    }

    public void setClient(User user, ChatClient chatClient, String confirmationCode) {
        setClient(user, chatClient);
        if (confirmationCode != null && txtConfirmationCode != null) {
            txtConfirmationCode.setText(confirmationCode);
        }
    }

    public void setBackFxml(String backFxml) {
        this.backFxml = backFxml;
    }

    /* =======================
       Actions
       ======================= */
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
            // ❗ לא מציגים הודעה כאן – מחכים לשרת
        } catch (IOException e) {
            showError("Failed to send cancel request.");
            e.printStackTrace();
        }
    }

    @FXML
    private void onBackClicked() {
        if (chatClient != null) {
            chatClient.setResponseHandler(null);
        }

        if (backFxml == null) {
            System.err.println("CancelWaiting_BController: backFxml not set");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(backFxml));
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

    /* =======================
       Server Response
       ======================= */
    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {
            if (response == null) return;

            if (response.isSuccess()) {
                showSuccess(
                        response.getMessage() != null
                                ? response.getMessage()
                                : "Waiting cancelled successfully"
                );
            } else {
                showError(
                        response.getMessage() != null
                                ? response.getMessage()
                                : "Cancel failed."
                );
            }
        });
    }

    /* =======================
       UI Helpers
       ======================= */
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

    private void hideMessage() {
        lblMessage.setVisible(false);
    }

    @Override public void handleConnectionError(Exception e) {}
    @Override public void handleConnectionClosed() {}
}
