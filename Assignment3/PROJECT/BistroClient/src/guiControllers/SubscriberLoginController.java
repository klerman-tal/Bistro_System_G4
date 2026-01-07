package guiControllers;

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
import java.io.IOException;

public class SubscriberLoginController {

    @FXML private TextField subscriberIdField;
    @FXML private TextField usernameField;
    @FXML private Label lblMessage;

    @FXML
    public void initialize() {
        // הגנה בזמן אמת: רק מספרים בשדה ה-ID ומקסימום 10 ספרות
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
        String id = subscriberIdField.getText().trim();
        String user = usernameField.getText().trim();

        if (id.isEmpty() || user.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        // כאן תבוא הלוגיקה מול השרת
        System.out.println("Attempting login for ID: " + id);
    }

    /**
     * פתיחת חלון קטן (Popup) לשחזור קוד מנוי
     */
    @FXML
    private void handleForgotCode(ActionEvent event) {
        try {
            // טעינת ה-FXML של החלון הקטן
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ForgotSubscriberCode.fxml"));
            Parent root = loader.load();

            // יצירת במה (Stage) חדשה לחלון הקופץ
            Stage popupStage = new Stage();
            
            // הגדרה שהחלון מודאלי - נועל את המסך הראשי
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle("Recover Subscriber Code");
            
            // הגדרת הסצנה וקיבוע גודל החלון
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            
            System.out.println("Opening Forgot Code popup...");
            popupStage.showAndWait(); // מציג ומחכה עד שהמשתמש יסגור

        } catch (IOException e) {
            System.err.println("Error: Could not find /gui/ForgotSubscriberCode.fxml");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackButton(ActionEvent event) {
        navigateTo(event, "/gui/Login_B.fxml");
    }

    /**
     * מתודה מרכזית לניווט בין מסכים מלאים
     */
    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
            
            System.out.println("Navigated to: " + fxmlPath);
        } catch (IOException e) {
            System.err.println("Navigation failed for: " + fxmlPath);
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        lblMessage.setText(msg);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }
}