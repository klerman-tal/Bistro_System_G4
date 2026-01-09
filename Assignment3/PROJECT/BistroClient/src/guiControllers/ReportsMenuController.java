package guiControllers;

import interfaces.ClientActions;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class ReportsMenuController {

    @FXML private BorderPane rootPane;
    @FXML private Label lblMessage;

    private ClientActions clientActions;

    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    private void openWindow(String fxmlName, String title) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
            Parent root = loader.load();

            Object controller = loader.getController();

            if (controller instanceof TimeReportController) {
                ((TimeReportController) controller).setClientActions(clientActions);

            } else if (controller instanceof SubscribersReportController) {
                ((SubscribersReportController) controller).setClientActions(clientActions);
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle("Bistro - " + title);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Failed to open: " + fxmlName);
        }
    }


    @FXML
    private void onTimeReportClicked() {
    	openWindow("TimeReport.fxml", "Time Report");
    }

    @FXML
    private void onSubscribersReportClicked() {
        openWindow("SubscribersReport.fxml", "Subscribers Report");
    }


    @FXML
    private void onBackClicked() {
        openWindow("RestaurantManagement_B.fxml", "Restaurant Management");
    }

    private void showMessage(String msg) {
        lblMessage.setText(msg);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }
}
