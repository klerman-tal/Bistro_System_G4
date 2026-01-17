package guiControllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import application.ClientUI;

public class Login_BController {

    @FXML
    private void handleSubscriberChoice(ActionEvent event) {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/SubscriberLogin.fxml"));
            Parent root = loader.load();

            SubscriberLoginController nextController = loader.getController();
            nextController.setClient(application.ClientUI.client);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // ✅ תצוגה בלבד – בלי Scene חדשה
            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }

            stage.setMaximized(true);
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

            GuestLoginController nextController = loader.getController();
            nextController.setClient(application.ClientUI.client);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // ✅ תצוגה בלבד – בלי Scene חדשה
            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }

            stage.setMaximized(true);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
