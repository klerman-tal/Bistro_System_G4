package guiControllers;

import application.ChatClient;
import dto.ResponseDTO;
import entities.Subscriber;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

import java.io.IOException;

public class SubscriberLoginController implements ClientResponseHandler {

    @FXML private TextField subscriberIdField;
    @FXML private TextField usernameField;
    @FXML private Label lblMessage;

    private ChatClient chatClient;
    private ClientAPI api;

    /**
     * חיבור ל־ChatClient + רישום כ־ResponseHandler
     */
    public void setClient(ChatClient chatClient) {
        this.chatClient = chatClient;
        if (chatClient != null) {
            this.api = new ClientAPI(chatClient);
            chatClient.setResponseHandler(this); // ✅ רק למסך הזה
        }
    }

    @FXML
    public void initialize() {
        subscriberIdField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                subscriberIdField.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (newVal.length() > 10) {
                subscriberIdField.setText(oldVal);
            }
        });
    }

    @FXML
    private void handleSubscriberLogin(ActionEvent event) {
        hideMessage();

        String idStr = subscriberIdField.getText().trim();
        String user = usernameField.getText().trim();

        if (idStr.isEmpty() || user.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        try {
            int id = Integer.parseInt(idStr);
            api.loginSubscriber(id, user);
            showInfo("Connecting to server...");
        } catch (Exception e) {
            showError("Subscriber ID must be numeric.");
        }
    }

    /**
     * 🔥 קבלת תשובות מהשרת
     */
    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {

            // ✅ קריטי: מסך זה מטפל רק בתשובת LOGIN
            if (!(response.getData() instanceof Subscriber)) {
                return;
            }

            if (response.isSuccess()) {
                Subscriber subscriber = (Subscriber) response.getData();
                goToMenu(subscriber);
            } else {
                showError(response.getMessage());
            }
        });
    }

    /**
     * מעבר לתפריט הראשי
     */
    private void goToMenu(Subscriber subscriber) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/Menu_B.fxml"));
            Parent root = loader.load();

            Menu_BController menu = loader.getController();
            menu.setClient(subscriber, chatClient); // ✅ הזרקה נכונה

            Stage stage =
                    (Stage) subscriberIdField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to open menu.");
        }
    }

    @FXML
    private void handleForgotCode(ActionEvent event) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/ForgotSubscriberCode.fxml"));
            Parent root = loader.load();

            ForgotCodeController controller = loader.getController();
            controller.setClient(chatClient);
            chatClient.setResponseHandler(controller); // ✅ החלפת handler

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle("Recover Subscriber Code");
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();

            // אחרי סגירת הפופאפ – חוזרים ל־Login כ־handler
            chatClient.setResponseHandler(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackButton(ActionEvent event) {
        navigateTo(event, "/gui/Login_B.fxml");
    }

    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage =
                    (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* ================= UI HELPERS ================= */

    private void showError(String msg) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: red;");
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    private void showInfo(String msg) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: black;");
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    private void hideMessage() {
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
    }

    @Override
    public void handleConnectionError(Exception e) {
        Platform.runLater(() -> showError("Connection error"));
    }

    @Override
    public void handleConnectionClosed() {
        Platform.runLater(() -> showError("Connection closed"));
    }
}
