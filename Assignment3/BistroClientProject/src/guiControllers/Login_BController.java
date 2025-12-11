package guiControllers;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Login_BController {

    @FXML private BorderPane rootPane;

    @FXML private TextField txtUserName;
    @FXML private TextField txtSubscriberNumber;
    @FXML private TextField txtPhoneNumber;
    @FXML private TextField txtEmail;

    @FXML private Button btnLogin;
    @FXML private Button btnCancel;

    @FXML private Label lblMessage;

    @FXML
    private void onLoginClicked() {
        // כרגע: תמיד לעבור ל-MENU בלי לוגיקה
        openMenu();
    }

    @FXML
    private void onCancelClicked() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.close();
    }

    private void openMenu() {
        try {
            // שימי לב לנתיב /gui/ כי ה-FXMLים יושבים בחבילה gui
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Menu_B.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle("Bistro - Main Menu");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            if (lblMessage != null) {
                lblMessage.setText("Failed to open menu.");
                lblMessage.setVisible(true);
                lblMessage.setManaged(true);
            }
        }
    }
}
