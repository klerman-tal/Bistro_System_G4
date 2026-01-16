package guiControllers;

import java.io.IOException;
import application.ChatClient;
import dto.ResponseDTO;
import entities.Subscriber;
import entities.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

/**
 * Controller for the Subscriber Login screen.
 * Handles standard login and simulated barcode scanning.
 */
public class SubscriberLoginController implements ClientResponseHandler {

    @FXML private TextField subscriberIdField;
    @FXML private TextField usernameField;
    @FXML private Label lblMessage;
    @FXML private Button btnBarcodeSim;

    private ChatClient chatClient;
    private ClientAPI clientAPI;
    private User loggedInUser;

    /**
     * Initializes the controller with the ChatClient and sets up the API.
     */
    public void setClient(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.clientAPI = new ClientAPI(chatClient);
        this.chatClient.setResponseHandler(this);
    }

    /**
     * Handles the standard manual login button click.
     */
    @FXML
    private void handleSubscriberLogin(ActionEvent event) {
        String idStr = subscriberIdField.getText().trim();
        String username = usernameField.getText().trim();

        if (idStr.isEmpty() || username.isEmpty()) {
            showError("Please enter both Subscriber ID and Username.");
            return;
        }

        try {
            int subscriberId = Integer.parseInt(idStr);
            clientAPI.loginSubscriber(subscriberId, username);
        } catch (NumberFormatException e) {
            showError("Subscriber ID must be a number.");
        } catch (IOException e) {
            showError("Server connection error.");
        }
    }

    /**
     * Opens the External Barcode Scanner Simulation window.
     */
    @FXML
    private void onBarcodeSimClicked(ActionEvent event) {
        Stage simStage = new Stage();
        simStage.setTitle("External Barcode Scanner Simulation");

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: white; -fx-border-color: #800000; -fx-border-width: 3; -fx-border-radius: 10;");

        Label title = new Label("Place Card on Scanner");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #800000; -fx-font-size: 14px;");

        TextField txtId = new TextField();
        txtId.setPromptText("Subscriber ID...");

        Button btnScan = new Button("SCAN");
        btnScan.setStyle("-fx-background-color: #800000; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        
        btnScan.setOnAction(e -> {
            String id = txtId.getText().trim();
            if (id.isEmpty()) return;
            try {
                // קריאה ל-API שהוספנו
                clientAPI.loginByBarcode(id);
                simStage.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        root.getChildren().addAll(title, txtId, btnScan);
        simStage.setScene(new Scene(root, 320, 180));
        simStage.initModality(Modality.APPLICATION_MODAL);
        simStage.show();
    }

    /**
     * Handles the response from the server.
     */
    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {
        	System.out.println("Response received: Success=" + response.isSuccess() + ", Data=" + response.getData());
            if (response.isSuccess() && response.getData() instanceof User) {
                this.loggedInUser = (User) response.getData();
                navigateToMainMenu();
            } else {
                showError(response.getMessage());
            }
        });
    }

    /**
     * Navigates to the subscriber main menu after successful login.
     */
    /**
     * Navigates to the main menu after successful login.
     */
    private void navigateToMainMenu() {
        try {
            // שינוי קריטי: שם הקובץ חייב להיות Menu_B.fxml כפי שמופיע בתיקיית ה-gui שלך
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Menu_B.fxml"));
            Parent root = loader.load();
            
            // השגת הקונטרולר של המסך הבא
            Object controller = loader.getController();
            
            // העברת הנתונים (המשתמש והקליינט) לקונטרולר הבא
            try {
                controller.getClass()
                    .getMethod("setClient", User.class, ChatClient.class)
                    .invoke(controller, loggedInUser, chatClient);
            } catch (Exception e) {
                System.out.println("Note: Could not call setClient, check method signature on next controller.");
            }

            // החלפת הסצנה בחלון הקיים
            Stage stage = (Stage) btnBarcodeSim.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Bistro - Main Menu");
            stage.centerOnScreen(); // מומלץ כדי שהתפריט יפתח במרכז
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            showError("FXML Load Error: Could not find /gui/Menu_B.fxml");
        }
    }

    @FXML
    private void handleBackButton(ActionEvent event) {
        // לוגיקה לחזרה למסך הפתיחה
    }

    @FXML
    private void handleForgotCode(ActionEvent event) {
        showError("Contact support to recover your code.");
    }

    private void showError(String msg) {
        lblMessage.setText(msg);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    @Override public void handleConnectionError(Exception e) { showError("Connection lost."); }
    @Override public void handleConnectionClosed() {}
}