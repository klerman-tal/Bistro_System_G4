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
import dto.TimeReportDTO;
import dto.RequestDTO;
import protocol.Commands;



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
        // בדיקה זמנית – ינואר 2025
        requestTimeReport(2025, 1);
    }
    
    private void requestTimeReport(int year, int month) {

        if (clientActions == null) {
            System.out.println("❌ clientActions is NULL");
            return;
        }

        TimeReportDTO dto = new TimeReportDTO();
        dto.setYear(year);
        dto.setMonth(month);

        RequestDTO request =
                new RequestDTO(Commands.GET_TIME_REPORT, dto);

        clientActions.sendToServer(request);

        System.out.println("📤 TimeReport request sent: " + year + "-" + month);
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
