package guiControllers;

import application.ChatClient;
import dto.RequestDTO;
import dto.ResponseDTO;
import dto.TimeReportDTO;
import entities.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import network.ClientResponseHandler;
import protocol.Commands;

import java.io.IOException;
import java.util.Map;

public class TimeReportController implements ClientResponseHandler {

    // ===== Root =====
    @FXML private BorderPane rootPane;

    // ===== TAB 1: Arrival Status =====
    @FXML private PieChart arrivalPieChart;
    @FXML private Label lblOnTime;
    @FXML private Label lblMinorDelay;
    @FXML private Label lblMajorDelay;

    // ===== TAB 2: Stay Duration =====
    @FXML private BarChart<String, Number> timesChart;
    @FXML private Label lblMonthlyAvg;
    @FXML private Label lblMaxDay;
    @FXML private Label lblMinDay;

    // ===== Client =====
    private ChatClient chatClient;
    private User user;

    /**
     * Called from ReportsMenuController
     */
    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;

        // register for responses
        this.chatClient.setResponseHandler(this);
        System.out.println("📤 Sending GET_TIME_REPORT request");

        requestTimeReport(2025, 1); // demo
    }

    @FXML
    public void initialize() {
        // ❗ No server calls here
    }

    // =====================
    // Requests
    // =====================
    private void requestTimeReport(int year, int month) {
        TimeReportDTO dto = new TimeReportDTO();
        dto.setYear(year);
        dto.setMonth(month);

        try {
            RequestDTO request =
                    new RequestDTO(Commands.GET_TIME_REPORT, dto);
            		chatClient.sendToServer(request);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // =====================
    // Server → Client
    // =====================
    @Override
    public void handleResponse(ResponseDTO response) {
    	System.out.println("🟢 handleResponse called in TimeReportController");

        if (response == null || !response.isSuccess()) return;
        if (!(response.getData() instanceof TimeReportDTO)) {
        	System.out.println("📥 TimeReportDTO received in client");
        	return;
        }
        	
        	

        TimeReportDTO dto = (TimeReportDTO) response.getData();

        Platform.runLater(() -> {
            drawArrivalStatus(dto);
            drawStayDuration(dto);   // כבר מוכן, גם אם ה-DTO ריק
        });
    }

    // =====================
    // TAB 1 – Pie Chart
    // =====================
    private void drawArrivalStatus(TimeReportDTO dto) {

        int onTime = dto.getOnTimeCount();
        int minor = dto.getMinorDelayCount();
        int major = dto.getSignificantDelayCount();

        arrivalPieChart.getData().setAll(
                new PieChart.Data("On Time", onTime),
                new PieChart.Data("Minor Delay", minor),
                new PieChart.Data("Significant Delay", major)
        );

        lblOnTime.setText("On Time: " + onTime);
        lblMinorDelay.setText("Minor Delay: " + minor);
        lblMajorDelay.setText("Significant Delay: " + major);
    }

    // =====================
    // TAB 2 – Bar Chart
    // =====================
    private void drawStayDuration(TimeReportDTO dto) {

        timesChart.getData().clear();

        if (dto.getAvgStayMinutesPerDay() == null) return;

        XYChart.Series<String, Number> series =
                new XYChart.Series<>();
        series.setName("Average Stay (minutes)");

        for (Map.Entry<Integer, Integer> e :
                dto.getAvgStayMinutesPerDay().entrySet()) {

            series.getData().add(
                    new XYChart.Data<>(
                            String.valueOf(e.getKey()),
                            e.getValue()
                    )
            );
        }

        timesChart.getData().add(series);

        lblMonthlyAvg.setText(
                "Monthly Average: " + dto.getMonthlyAvgStay() + " min");
        lblMaxDay.setText(
                "Longest Stay: Day " + dto.getMaxAvgDay()
                        + " (" + dto.getMaxAvgMinutes() + " min)");
        lblMinDay.setText(
                "Shortest Stay: Day " + dto.getMinAvgDay()
                        + " (" + dto.getMinAvgMinutes() + " min)");
    }

    // =====================
    // Navigation
    // =====================
    @FXML
    private void onBackClicked() {
        try {
            chatClient.setResponseHandler(null);

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/ReportsMenu.fxml"));
            Parent root = loader.load();

            ReportsMenuController controller = loader.getController();
            controller.setClient(user, chatClient);

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle("Bistro - Reports");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override public void handleConnectionError(Exception e) {}
    @Override public void handleConnectionClosed() {}
}
