package guiControllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node; // הוספנו את זה!
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Random;

public class RegisterController {

    @FXML private TextField firstNameField, lastNameField, usernameField, phoneField, emailField;
    @FXML private Label lblMessage;

    @FXML
    public void initialize() {
        phoneField.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*")) phoneField.setText(newV.replaceAll("[^\\d]", ""));
            if (newV.length() > 10) phoneField.setText(oldV);
        });
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        if (isFormEmpty()) {
            lblMessage.setText("Please fill in all fields");
            lblMessage.setVisible(true);
            lblMessage.setManaged(true);
            return;
        }

        int subscriberID = 100000 + new Random().nextInt(899999);
        
        // מציגים את ההודעה
        showRegistrationSuccess(subscriberID);
        
        // חוזרים למסך הראשי אחרי הלחיצה על OK
        navigateTo(event, "/gui/Login_B.fxml");
    }

    private void showRegistrationSuccess(int id) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Account Created");
        alert.setHeaderText("Welcome to the Bistro Family!");
        alert.setContentText("This is your new Subscriber Number: " + id + "\n\n" +
                             "Recommendation: Save it in a safe and private place!!");
        
        alert.showAndWait(); // הקוד נעצר כאן עד שהמשתמש לוחץ OK
    }

    private boolean isFormEmpty() {
        return firstNameField.getText().isEmpty() || lastNameField.getText().isEmpty() || 
               usernameField.getText().isEmpty() || phoneField.getText().isEmpty() || emailField.getText().isEmpty();
    }

    @FXML
    private void handleBackButton(ActionEvent event) {
        navigateTo(event, "/gui/Login_B.fxml");
    }

    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            // עכשיו זה יעבוד כי הוספנו Import ל-Node
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
            
            System.out.println("Navigated to: " + fxmlPath);
        } catch (IOException e) {
            System.err.println("Navigation failed for: " + fxmlPath);
            e.printStackTrace();
        }
    }
}