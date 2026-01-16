package guiControllers;

import application.ChatClient;
import application.ClientSession;
import dto.ResponseDTO;
import entities.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

import java.io.IOException;

public class SubscriberLoginController implements ClientResponseHandler {

    @FXML private TextField subscriberIdField;
    @FXML private TextField usernameField;
    @FXML private Label lblMessage;
    @FXML private Button btnBarcodeSim;

    private ChatClient chatClient;
    private ClientAPI api;

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
        String idStr = subscriberIdField.getText().trim();
        String user = usernameField.getText().trim();

        if (idStr.isEmpty() || user.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        try {
            api.loginSubscriber(Integer.parseInt(idStr), user);
        } catch (Exception e) {
            showError("Subscriber ID must be numeric.");
        }
    }

    @FXML
    private void onBarcodeSimClicked(ActionEvent event) {
        Stage simStage = new Stage();
        simStage.setTitle("External Barcode Scanner Simulation");

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: white; -fx-border-color: #800000; -fx-border-width: 3; -fx-border-radius: 10;");

        Label title = new Label("Place Card on Scanner (Enter Subscriber ID)");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #800000; -fx-font-size: 14px;");
        title.setWrapText(true);
        title.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        TextField txtId = new TextField();
        txtId.setPromptText("e.g. 12345");

        Button btnScan = new Button("SCAN");
        btnScan.setStyle("-fx-background-color: #800000; -fx-text-fill: white; -fx-font-weight: bold;");
        
        btnScan.setOnAction(e -> {
            String id = txtId.getText().trim();
            if (!id.isEmpty()) {
                try {
                    api.loginByBarcode(id);
                    simStage.close();
                } catch (IOException ex) { ex.printStackTrace(); }
            }
        });

        root.getChildren().addAll(title, txtId, btnScan);
        simStage.setScene(new Scene(root, 450, 200));
        simStage.initModality(Modality.APPLICATION_MODAL);
        simStage.show();
    }

    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {
            if (response.isSuccess() && response.getData() instanceof User user) {
                ClientSession.setLoggedInUser(user);
                ClientSession.setActingUser(user);
                // שימוש ב-navigate עבור כניסה לתפריט
                navigateTo(null, "/gui/Menu_B.fxml", user);
            } else {
                showError(response.getMessage());
            }
        });
    }

    @FXML
    private void handleBackButton(ActionEvent event) {
        navigateTo(event, "/gui/Login_B.fxml", null);
    }

    @FXML
    private void handleForgotCode(ActionEvent event) {
        // פתיחת פופ-אפ כפי שהיה במקור שלך
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ForgotSubscriberCode.fxml"));
            Parent root = loader.load();
            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();
        } catch (IOException e) { showError("Error opening recovery."); }
    }

    /**
     * פונקציית הניווט המרכזית כפי שהייתה לך
     */
    private void navigateTo(ActionEvent event, String fxmlPath, User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // אם עוברים לתפריט ומעבירים משתמש
            if (user != null) {
                Object controller = loader.getController();
                try {
                    controller.getClass()
                        .getMethod("setClient", User.class, ChatClient.class)
                        .invoke(controller, user, chatClient);
                } catch (Exception e) { System.out.println("No setClient method found."); }
            }

            // השגת ה-Stage
            Stage currentStage;
            if (event != null) {
                currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            } else {
                currentStage = (Stage) subscriberIdField.getScene().getWindow();
            }

            currentStage.setScene(new Scene(root));
            currentStage.centerOnScreen();
            currentStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Navigation failed.");
        }
    }

    private void showError(String msg) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: #ff0000; -fx-font-weight: bold;");
        lblMessage.setVisible(true);
    }

    private void hideMessage() { lblMessage.setVisible(false); }

    @Override public void handleConnectionError(Exception e) { Platform.runLater(() -> showError("Conn Error")); }
    @Override public void handleConnectionClosed() {}
}