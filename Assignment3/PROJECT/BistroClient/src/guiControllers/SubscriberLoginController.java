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

// הוספתי implements ClientResponseHandler כדי שהקונטרולר יקשיב לשרת
public class SubscriberLoginController implements ClientResponseHandler {

    @FXML private TextField subscriberIdField;
    @FXML private TextField usernameField;
    @FXML private Label lblMessage;

    private ChatClient chatClient;
    private ClientAPI api;

    // מתודה חדשה לקבלת הלקוח מהמסך הקודם
    public void setClient(ChatClient chatClient) {
        this.chatClient = chatClient;
        if (chatClient != null) {
            this.api = new ClientAPI(chatClient);
            this.chatClient.setResponseHandler(this); // רישום הקונטרולר כמקבל תשובות
        }
    }

    @FXML
    public void initialize() {
        subscriberIdField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                subscriberIdField.setText(newValue.replaceAll("[^\\d]", ""));
            }
            if (newValue.length() > 10) {
                subscriberIdField.setText(oldValue);
            }
        });
    }

    @FXML
    private void handleSubscriberLogin(ActionEvent event) {
        String idStr = subscriberIdField.getText().trim();
        String user = usernameField.getText().trim();

        if (idStr.isEmpty() || user.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        try {
            int id = Integer.parseInt(idStr);
            // System.out.println("Attempting login for ID: " + id); // הישן
            api.loginSubscriber(id, user); // שליחה לשרת דרך ה-API
            lblMessage.setText("Connecting to server...");
            lblMessage.setVisible(true);
        } catch (Exception e) {
            showError("ID must be a number.");
        }
    }

    // מימוש קבלת התשובה מהשרת
    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {
            if (response.isSuccess()) {
                Subscriber s = (Subscriber) response.getData();
                goToMenu(s);
            } else {
                showError(response.getMessage());
            }
        });
    }

    private void goToMenu(Subscriber subscriber) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Menu_B.fxml"));
            Parent root = loader.load();

            Menu_BController menu = loader.getController();
            // הזרקת המשתמש והלקוח לתפריט הראשי
            menu.setClient(subscriber, chatClient); 

            Stage stage = (Stage) subscriberIdField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleForgotCode(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ForgotSubscriberCode.fxml"));
            Parent root = loader.load();
            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle("Recover Subscriber Code");
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();
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
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        lblMessage.setText(msg);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    @Override public void handleConnectionError(Exception e) { Platform.runLater(() -> showError("Connection error")); }
    @Override public void handleConnectionClosed() {}
}