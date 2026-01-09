package guiControllers;

import application.ChatClient;
import application.ClientUI;
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
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;
import java.io.IOException;

/**
 * קונטרולר למסך כניסת אורח (GuestLogin.fxml)
 * מעודכן לתמיכה בתקשורת אסינכרונית והזרקת משתמש "תפוס"
 */
public class GuestLoginController implements ClientResponseHandler { // ✨ הוספת הממשק

    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private Label lblMessage;

    // // private ClientActions clientActions; // ❌ הוחלף בגישה האחידה
    private ChatClient chatClient;
    private ClientAPI api;

    /**
     * הזרקת הלקוח והכנת ה-API
     */
    public void setClient(ChatClient chatClient) { // ✨ מתודה חדשה להזרקה
        this.chatClient = chatClient;
        if (chatClient != null) {
            this.api = new ClientAPI(chatClient);
            this.chatClient.setResponseHandler(this); // רישום לקבלת תשובות מהשרת
        }
    }

    /**
     * מופעל בלחיצה על כפתור CONTINUE
     */
    @FXML
    private void handleGuestLogin(ActionEvent event) {
        hideError();

        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();

        // בדיקה ששניהם לא ריקים בו זמנית
        if (phone.isEmpty() && email.isEmpty()) {
            showError("Please enter at least a phone number or an email.");
            return;
        }

        // ולידציה לאימייל רק אם המשתמש הזין אחד
        if (!email.isEmpty() && (!email.contains("@") || !email.contains("."))) {
            showError("Please enter a valid email address.");
            return;
        }

        if (api != null) {
            try {
                api.loginGuest(phone, email); // שולח את מה שיש (גם אם אחד ריק)
                lblMessage.setText("Connecting...");
                lblMessage.setVisible(true);
            } catch (IOException e) {
                showError("Connection error.");
            }
        }
    }

    /**
     * טיפול בתשובה מהשרת - יצירת האורח הצליחה/נכשלה
     */
    @Override
    public void handleResponse(ResponseDTO response) { // ✨ מימוש קבלת תשובה
        Platform.runLater(() -> {
            if (response.isSuccess()) {
                // השרת מחזיר אובייקט User (אורח) עם ה-ID שנוצר ב-DB
                User guestUser = (User) response.getData();
                System.out.println("GUEST LOGIN SUCCESS: " + guestUser.getEmail());
                goToMenu(guestUser);
            } else {
                showError(response.getMessage());
            }
        });
    }

    /**
     * מעבר לתפריט עם הזרקת האורח ה"תפוס"
     */
    private void goToMenu(User guestUser) { // ✨ מתודה חדשה למעבר
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Menu_B.fxml"));
            Parent root = loader.load();

            Menu_BController menu = loader.getController();
            menu.setClient(guestUser, chatClient); // הזרקת האורח והחיבור לתפריט

            Stage stage = (Stage) phoneField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackButton(ActionEvent event) {
        navigateTo(event, "/gui/Login_B.fxml");
    }

    /* ================= פונקציות עזר (UI Utility) ================= */

    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            // אם חוזרים אחורה, כדאי להזריק שוב את הלקוח אם צריך
            if (loader.getController() instanceof Login_BController) {
                // לוגיקה נוספת במידת הצורך
            }

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

    // מימושים ריקים עבור הממשק
    @Override public void handleConnectionError(Exception e) { Platform.runLater(() -> showError("Server connection lost.")); }
    @Override public void handleConnectionClosed() {}
}