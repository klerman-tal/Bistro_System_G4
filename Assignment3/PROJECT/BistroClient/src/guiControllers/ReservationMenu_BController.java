package guiControllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ReservationMenu_BController {

    @FXML
    private void onCreateReservation(ActionEvent event) {
        navigateTo(event, "/gui/TableReservation_B.fxml");
    }

    @FXML
    private void onCancelReservation(ActionEvent event) {
        // ✅ שם נכון – תואם ל־Menu_BController
        navigateTo(event, "/gui/CancelReservation_B.fxml");
    }

    @FXML
    private void onBack(ActionEvent event) {
        navigateTo(event, "/gui/Menu_B.fxml");
    }

    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            var url = getClass().getResource(fxmlPath);
            if (url == null) {
                throw new RuntimeException("FXML not found: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            Stage stage =
                    (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
