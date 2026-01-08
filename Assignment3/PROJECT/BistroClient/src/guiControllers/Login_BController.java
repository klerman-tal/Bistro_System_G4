package guiControllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class Login_BController {

    @FXML
    private void handleSubscriberChoice(ActionEvent event) {
        navigateTo(event, "/gui/Menu_B.fxml");
    }

    @FXML
    private void handleGuestChoice(ActionEvent event) {
        navigateTo(event, "/gui/GuestLogin.fxml");
    }

    /**
     * פונקציה חדשה המעבירה למסך ההרשמה
     */
    @FXML
    private void handleRegisterChoice(ActionEvent event) {
        navigateTo(event, "/gui/Register.fxml");
    }

    /**
     * פונקציית עזר לניווט בין מסכים
     */
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