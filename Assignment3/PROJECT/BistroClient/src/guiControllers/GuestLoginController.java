package guiControllers;

import application.ChatClient;
import application.ClientSession;
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
import javafx.scene.control.TextFormatter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

import java.io.IOException;
import java.util.function.UnaryOperator;

public class GuestLoginController implements ClientResponseHandler {

    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private Label lblMessage;

    private ChatClient chatClient;
    private ClientAPI api;

    private final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    @FXML
    public void initialize() {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getControlNewText();
            if (text.matches("\\d*") && text.length() <= 10) return change;
            return null;
        };
        phoneField.setTextFormatter(new TextFormatter<>(filter));
    }

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

        if (phone.isEmpty() || email.isEmpty()) {
            showError("Please enter both phone and email.");
            return;
        }

        if (!phone.startsWith("05") || phone.length() != 10) {
            showError("Phone must start with '05' and be exactly 10 digits.");
            return;
        }

        if (!email.matches(EMAIL_REGEX)) {
            showError("Invalid email address (check for special characters or typos).");
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

                ClientSession.setLoggedInUser(guestUser);
                ClientSession.setActingUser(guestUser);

                goToMenu(guestUser);
            } else {
                showError(response.getMessage());
            }
        });
    }

    // =========================
    // MAIN SCREEN navigation
    // =========================
    private void goToMenu(User loggedInUser) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Menu_B.fxml"));
            Parent root = loader.load();
            Menu_BController menu = loader.getController();
            menu.setClient(loggedInUser, chatClient);

            Stage stage = (Stage) phoneField.getScene().getWindow();

            // ✅ תצוגה בלבד – בלי Scene חדשה
            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }

            stage.setMaximized(true);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to open menu.");
        }
    }

    // =========================
    // POPUP – לא נוגעים
    // =========================
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
            if (chatClient != null) {
                chatClient.setResponseHandler(this);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to open recovery window.");
        }
    }

    // =========================
    // MAIN SCREEN navigation
    // =========================
    @FXML
    private void handleBackButton(ActionEvent event) {
        navigateTo(event, "/gui/Login_B.fxml");
    }

    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // ✅ תצוגה בלבד – בלי Scene חדשה
            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }

            stage.setMaximized(true);
            stage.show();

        } catch (IOException e) {
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
