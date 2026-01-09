package guiControllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import application.ClientUI; // הוספה חדשה לגישה ללקוח הסטטי

public class Login_BController {

    @FXML
    private void handleSubscriberChoice(ActionEvent event) {
        // navigateTo(event, "/gui/Menu_B.fxml"); // הקוד הישן שעבר ישר לתפריט
        
        // הקוד החדש שעובר למסך הזנת פרטי המנוי ומעביר את הלקוח:
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/SubscriberLogin.fxml"));
            Parent root = loader.load();

            SubscriberLoginController nextController = loader.getController();
            // הזרקת הלקוח הסטטי מה-ClientUI למסך הבא כדי שהחיבור לשרת יעבור הלאה
            nextController.setClient(application.ClientUI.client); //

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGuestChoice(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/GuestLogin.fxml"));
            Parent root = loader.load();

            // ✨ הזרקת הלקוח למסך האורח כדי שיוכל לתקשר עם השרת
            GuestLoginController nextController = loader.getController();
            nextController.setClient(application.ClientUI.client); 

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegisterChoice(ActionEvent event) {
        navigateTo(event, "/gui/Register.fxml");
    }

    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
            
            System.out.println("Switched to: " + fxmlPath);
        } catch (IOException e) {
            System.err.println("Error loading FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }
}