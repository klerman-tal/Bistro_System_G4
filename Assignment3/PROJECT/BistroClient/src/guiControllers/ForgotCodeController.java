package guiControllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ForgotCodeController {

    @FXML private TextField usernameField, phoneField, emailField;
    @FXML private Label lblError;

    @FXML
    private void handleRecover(ActionEvent event) {
        if (usernameField.getText().isEmpty() || phoneField.getText().isEmpty() || emailField.getText().isEmpty()) {
            lblError.setText("Please fill all fields!");
            lblError.setVisible(true);
            lblError.setManaged(true);
            return;
        }

        // כאן תבוא הלוגיקה מול השרת לשליחת הקוד
        System.out.println("Recovery request for: " + usernameField.getText());
        
        // סגירת החלון אחרי הצלחה
        handleCancel(event);
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}