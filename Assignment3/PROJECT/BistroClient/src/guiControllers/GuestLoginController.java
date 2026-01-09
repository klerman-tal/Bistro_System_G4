package guiControllers;

import application.ChatClient;
import dto.ResponseDTO;
import entities.User;
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

public class GuestLoginController implements ClientResponseHandler {

    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private Label lblMessage;

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
    private void handleGuestLogin(ActionEvent event) {
        hideError();

        String phone = phoneField.getText() != null ? phoneField.getText().trim() : "";
        String email = emailField.getText() != null ? emailField.getText().trim() : "";

        if (phone.isEmpty() && email.isEmpty()) {
            showError("Please enter at least a phone number or an email.");
            return;
        }

        if (!email.isEmpty() && (!email.contains("@") || !email.contains("."))) {
            showError("Please enter a valid email address.");
            return;
        }

        if (api != null) {
            try {
                api.loginGuest(phone, email);
                lblMessage.setText("Connecting...");
                lblMessage.setVisible(true);
                lblMessage.setManaged(true);
            } catch (IOException e) {
                showError("Connection error.");
            }
        } else {
            showError("Client not initialized.");
        }
    }

    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {
            if (response.isSuccess()) {
                User guestUser = (User) response.getData();
                goToMenu(guestUser);
            } else {
                showError(response.getMessage());
            }
        });
    }

    private void goToMenu(User guestUser) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Menu_B.fxml"));
            Parent root = loader.load();

            Menu_BController menu = loader.getController();
            menu.setClient(guestUser, chatClient);

            Stage stage = (Stage) phoneField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to open menu.");
        }
    }

    // ✅ NEW: Open ForgotGuestConfirmation popup
    @FXML
    private void handleForgotConfirmationCode(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ForgotGuestConfirmation.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof ForgotGuestConfirmationController) {
                ((ForgotGuestConfirmationController) controller).setClient(chatClient);
            }

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle("Recover Confirmation Code");
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();

            // ✅ IMPORTANT: restore handler back to THIS screen
            if (chatClient != null) {
                chatClient.setResponseHandler(this);
            }

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to open recovery window.");
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
            System.err.println("Error navigating to: " + fxmlPath);
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        if (lblMessage != null) {
            lblMessage.setText(msg);
            lblMessage.setVisible(true);
            lblMessage.setManaged(true);
        }
    }

    private void hideError() {
        if (lblMessage != null) {
            lblMessage.setVisible(false);
            lblMessage.setManaged(false);
        }
    }

    @Override
    public void handleConnectionError(Exception e) {
        Platform.runLater(() -> showError("Server connection lost."));
    }

    @Override
    public void handleConnectionClosed() {}
}
