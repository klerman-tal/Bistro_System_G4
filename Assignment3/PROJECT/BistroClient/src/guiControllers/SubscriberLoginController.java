package guiControllers;

import application.ChatClient;
import application.ClientSession;
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

    private Stage stage;

    public void setClient(ChatClient chatClient) {
        this.chatClient = chatClient;
        if (chatClient != null) {
            this.api = new ClientAPI(chatClient);
            chatClient.setResponseHandler(this);
        }
    }

    @FXML
    public void initialize() {
        if (subscriberIdField != null) {
            subscriberIdField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*")) {
                    subscriberIdField.setText(newVal.replaceAll("[^\\d]", ""));
                }
                if (newVal.length() > 10) {
                    subscriberIdField.setText(oldVal);
                }
            });
        }
    }

    @FXML
    private void handleSubscriberLogin(ActionEvent event) {
        hideMessage();

        this.stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        String idStr = subscriberIdField.getText().trim();
        String user = usernameField.getText().trim();

        if (idStr.isEmpty() || user.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        if (api == null) {
            showError("Internal error: client not initialized.");
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

    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {
            if (response.isSuccess()) {
                if (response.getData() instanceof Subscriber subscriber) {

                    // ✅ Session: logged in = subscriber, acting = subscriber
                    ClientSession.setLoggedInUser(subscriber);
                    ClientSession.setActingUser(subscriber);

                    goToMenu(subscriber);
                }
            } else {
                showError(response.getMessage());
            }
        });
    }

    private void goToMenu(Subscriber loggedInUser) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Menu_B.fxml"));
            Parent root = loader.load();

            Menu_BController menu = loader.getController();
            menu.setClient(loggedInUser, chatClient);

            if (stage == null) {
                if (subscriberIdField != null && subscriberIdField.getScene() != null) {
                    stage = (Stage) subscriberIdField.getScene().getWindow();
                }
            }

            if (stage == null) {
                showError("Internal error: window not found.");
                return;
            }

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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ForgotSubscriberCode.fxml"));
            Parent root = loader.load();

            ForgotCodeController controller = loader.getController();
            controller.setClient(chatClient);
            chatClient.setResponseHandler(controller);

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle("Recover Subscriber Code");
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();

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

            Stage stageLocal = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stageLocal.setScene(new Scene(root));
            stageLocal.centerOnScreen();
            stageLocal.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        if (lblMessage == null) return;
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: #ff0000; -fx-font-weight: bold;");
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    private void showInfo(String msg) {
        if (lblMessage == null) return;
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: black;");
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    private void hideMessage() {
        if (lblMessage == null) return;
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