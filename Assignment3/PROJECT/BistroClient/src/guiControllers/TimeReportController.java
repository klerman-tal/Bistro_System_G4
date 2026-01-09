package guiControllers;

import interfaces.ClientActions;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class TimeReportController {

    // ===== Root =====
    @FXML
    private BorderPane rootPane;

    // ===== TAB 1: Arrival Status =====
    @FXML
    private PieChart arrivalPieChart;

    @FXML
    private Label lblOnTime;
    @FXML
    private Label lblMinorDelay;
    @FXML
    private Label lblMajorDelay;

    // ===== TAB 2: Stay Duration =====
    @FXML
    private BarChart<String, Number> timesChart;

    @FXML
    private Label lblMonthlyAvg;
    @FXML
    private Label lblMaxDay;
    @FXML
    private Label lblMinDay;

    private ClientActions clientActions;

    // Injected from previous screen
    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    @FXML
    public void initialize() {
        // Intentionally empty
        // Data will be loaded later via DTO / DB
    }

    // ===== BACK BUTTON =====
    @FXML
    private void onBackClicked() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/ReportsMenu.fxml"));
            Parent root = loader.load();

            ReportsMenuController controller = loader.getController();
            controller.setClientActions(clientActions);

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle("Bistro - Reports");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
