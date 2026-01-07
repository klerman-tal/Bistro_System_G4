package guiControllers;

import interfaces.ClientActions;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * קונטרולר למסך כניסת אורח (GuestLogin.fxml)
 * מעוצב לפי עקרונות UI/UX נקיים ואחידות עם מסך המנוי
 */
public class GuestLoginController {

    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private Label lblMessage;

    private ClientActions clientActions;

    /**
     * הזרקת ממשק הפעולות מול השרת
     */
    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    /**
     * מופעל בלחיצה על כפתור CONTINUE
     * מבצע ולידציה בסיסית ושולח לשרת דרך clientActions
     */
    @FXML
    private void handleGuestLogin(ActionEvent event) {
        hideError();

        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();

        // בדיקה ששדות אינם ריקים
        if (phone.isEmpty() || email.isEmpty()) {
            showError("Please fill in both phone and email.");
            return;
        }

        // בדיקת פורמט אימייל בסיסי (UX טוב יותר)
        if (!email.contains("@") || !email.contains(".")) {
            showError("Please enter a valid email address.");
            return;
        }

        // לוגיקת החיבור מול השרת
        if (clientActions != null) {
            boolean success = clientActions.loginGuest(phone, email);
            
            if (!success) {
                showError("Guest login failed. Please check details.");
                return;
            }
        }

        System.out.println("GUEST LOGIN SUCCESS: " + email);
        // כאן ניתן להוסיף מעבר למסך התפריט לאורחים
    }

    /**
     * חזרה למסך בחירת סוג המשתמש (Login_B.fxml)
     */
    @FXML
    private void handleBackButton(ActionEvent event) {
        navigateTo(event, "/gui/Login_B.fxml");
    }

    /* ================= פונקציות עזר (UI Utility) ================= */

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
}