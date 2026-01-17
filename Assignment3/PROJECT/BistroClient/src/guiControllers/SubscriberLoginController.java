package guiControllers;

import application.ChatClient;
import application.ClientSession;
import dto.ResponseDTO;
import entities.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

import java.io.IOException;

public class SubscriberLoginController implements ClientResponseHandler {

    // ✅ MUST match the FXML root (StackPane). Also requires fx:id in FXML (see below),
    // but we also fallback to subscriberIdField to avoid crashes.
    @FXML private StackPane rootPane;

    @FXML private TextField subscriberIdField;
    @FXML private TextField usernameField;
    @FXML private Label lblMessage;
    @FXML private Button btnBarcodeSim;

    private ChatClient chatClient;
    private ClientAPI api;

    /* ================= SETTERS ================= */

    public void setClient(ChatClient chatClient) {
        this.chatClient = chatClient;
        if (chatClient != null) {
            this.api = new ClientAPI(chatClient);
            chatClient.setResponseHandler(this);
        }
    }

    /* ================= INIT ================= */

    @FXML
    public void initialize() {
        hideMessage();

        // Subscriber ID – digits only
        if (subscriberIdField != null) {
            subscriberIdField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null) return;
                if (!newVal.matches("\\d*")) {
                    subscriberIdField.setText(newVal.replaceAll("[^\\d]", ""));
                }
                if (newVal.length() > 10) {
                    subscriberIdField.setText(oldVal);
                }
            });
        }
    }

    /* ================= ACTIONS ================= */

    @FXML
    private void handleSubscriberLogin() {
        hideMessage();

        if (api == null) {
            showError("Internal error: connection not initialized.");
            return;
        }

        String idStr = safe(subscriberIdField);
        String username = safe(usernameField);

        if (idStr.isEmpty() || username.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        try {
            api.loginSubscriber(Integer.parseInt(idStr), username);
        } catch (NumberFormatException e) {
            showError("Subscriber ID must be numeric.");
        } catch (Exception e) {
            showError("Failed to send login request.");
        }
    }

    @FXML
    private void onBarcodeSimClicked() {
        if (api == null) {
            showError("Internal error: connection not initialized.");
            return;
        }

        Stage simStage = new Stage();
        simStage.setTitle("Barcode Scanner Simulation");
        simStage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(25));
        root.setStyle("""
            -fx-background-color: white;
            -fx-border-color: #800000;
            -fx-border-width: 3;
            -fx-border-radius: 10;
        """);

        Label title = new Label("Place Card on Scanner (Subscriber ID)");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #800000;");

        TextField txtId = new TextField();
        txtId.setPromptText("Subscriber ID");

        Button btnScan = new Button("SCAN");
        btnScan.setStyle("-fx-background-color: #800000; -fx-text-fill: white;");

        btnScan.setOnAction(e -> {
            String id = txtId.getText().trim();
            if (!id.isEmpty()) {
                try {
                    api.loginByBarcode(id);
                    simStage.close();
                } catch (IOException ex) {
                    showError("Failed to scan.");
                }
            }
        });

        root.getChildren().addAll(title, txtId, btnScan);
        simStage.setScene(new Scene(root, 420, 200));
        simStage.showAndWait();
    }

    @FXML
    private void handleBackButton() {
        openWindow("Login_B.fxml", "Login", null);
    }

    @FXML
    private void handleForgotCode() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/ForgotSubscriberCode.fxml"));
            Parent root = loader.load();

            Stage popup = new Stage();
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setScene(new Scene(root));
            popup.showAndWait();

        } catch (IOException e) {
            showError("Failed to open recovery window.");
        }
    }

    /* ================= SERVER RESPONSE ================= */

    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {
            if (response != null && response.isSuccess() && response.getData() instanceof User user) {

                ClientSession.setLoggedInUser(user);
                ClientSession.setActingUser(user);

                openWindow("Menu_B.fxml", "Main Menu", user);
            } else {
                showError(response != null ? response.getMessage() : "Login failed.");
            }
        });
    }

    @Override public void handleConnectionError(Exception e) {
        Platform.runLater(() -> showError("Connection error."));
    }

    @Override public void handleConnectionClosed() {}

    /* ================= NAVIGATION ================= */

    private void openWindow(String fxmlName, String title, User user) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
            Parent root = loader.load();

            Object controller = loader.getController();

            if (controller != null && user != null && chatClient != null) {
                try {
                    controller.getClass()
                            .getMethod("setClient", User.class, ChatClient.class)
                            .invoke(controller, user, chatClient);
                } catch (Exception ignored) {}
            }

            switchRoot(root, "Bistro - " + title);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Navigation failed.");
        }
    }

    private void switchRoot(Parent root, String title) {
        Stage stage = getStageSafe();
        if (stage == null) {
            showError("Navigation failed (no stage).");
            return;
        }

        Scene scene = stage.getScene();
        if (scene == null) stage.setScene(new Scene(root));
        else scene.setRoot(root);

        stage.setTitle(title);
        stage.setMaximized(true);
        stage.show();
    }

    private Stage getStageSafe() {
        if (rootPane != null && rootPane.getScene() != null) {
            return (Stage) rootPane.getScene().getWindow();
        }
        if (subscriberIdField != null && subscriberIdField.getScene() != null) {
            return (Stage) subscriberIdField.getScene().getWindow();
        }
        return null;
    }

    /* ================= UI HELPERS ================= */

    private String safe(TextField tf) {
        return (tf == null || tf.getText() == null) ? "" : tf.getText().trim();
    }

    private void showError(String msg) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    private void hideMessage() {
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
    }
}
