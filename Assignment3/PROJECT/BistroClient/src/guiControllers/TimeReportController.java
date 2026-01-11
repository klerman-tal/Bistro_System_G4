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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class TimeReportController implements ClientResponseHandler {

    // ===== Root =====
    @FXML private BorderPane rootPane;

    // ===== Summary – Arrival Status =====
    @FXML private Label lblArrivalSummaryTitle;
    @FXML private Label lblArrivalSummaryText;

    // ===== Summary – Stay Duration =====
    @FXML private Label lblStaySummaryTitle;
    @FXML private Label lblStaySummaryText;

    // ===== Arrival Status =====
    @FXML private PieChart arrivalPieChart;
    @FXML private Label lblOnTime;
    @FXML private Label lblMinorDelay;
    @FXML private Label lblMajorDelay;

    // ===== Stay Duration =====
    @FXML private BarChart<String, Number> timesChart;
    @FXML private Label lblMonthlyAvg;
    @FXML private Label lblMaxDay;
    @FXML private Label lblMinDay;

    private ChatClient chatClient;
    private User user;
    private YearMonth reportMonth;

    // =====================
    // Init
    // =====================
    @FXML
    public void initialize() {
        arrivalPieChart.setLegendVisible(false);
    }

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
        this.chatClient.setResponseHandler(this);

        // תמיד מציגים את החודש שהסתיים
        reportMonth = YearMonth.now().minusMonths(1);

        updateSummary(
                reportMonth,
                lblArrivalSummaryTitle,
                lblArrivalSummaryText,
                "customer arrival status"
        );

        updateSummary(
                reportMonth,
                lblStaySummaryTitle,
                lblStaySummaryText,
                "average customer stay duration"
        );

        requestTimeReport(reportMonth.getYear(), reportMonth.getMonthValue());
    }

    // =====================
    // Requests
    // =====================
    private void requestTimeReport(int year, int month) {
        TimeReportDTO dto = new TimeReportDTO();
        dto.setYear(year);
        dto.setMonth(month);

        try {
            chatClient.sendToServer(
                    new RequestDTO(Commands.GET_TIME_REPORT, dto)
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =====================
    // Server → Client
    // =====================
    @Override
    public void handleResponse(ResponseDTO response) {
        if (response == null || !response.isSuccess()) return;
        if (!(response.getData() instanceof TimeReportDTO)) return;

        TimeReportDTO dto = (TimeReportDTO) response.getData();

        Platform.runLater(() -> {
            drawArrivalStatus(dto);
            drawStayDuration(dto);
        });
    }

    // =====================
    // Shared Summary
    // =====================
    private void updateSummary(
            YearMonth reportMonth,
            Label titleLabel,
            Label textLabel,
            String topic
    ) {
        DateTimeFormatter monthFormatter =
                DateTimeFormatter.ofPattern("MMMM yyyy");
        DateTimeFormatter dateFormatter =
                DateTimeFormatter.ofPattern("dd/MM/yyyy");

        YearMonth nextMonth = reportMonth.plusMonths(1);
        String publishDate = nextMonth.plusMonths(1)
                .atDay(1)
                .format(dateFormatter);

        titleLabel.setText(
                "Summary: " + reportMonth.format(monthFormatter)
        );

        textLabel.setText(
                "This report presents " + topic + " for "
                + reportMonth.format(monthFormatter) + ".\n"
                + "The " + nextMonth.format(monthFormatter)
                + " report will be available starting " + publishDate + "."
        );
    }

    // =====================
    // Arrival Status
    // =====================
    private void drawArrivalStatus(TimeReportDTO dto) {

        int onTime = dto.getOnTimeCount();
        int minor = dto.getMinorDelayCount();
        int major = dto.getSignificantDelayCount();
        int total = onTime + minor + major;

        arrivalPieChart.getData().setAll(
                new PieChart.Data(percent(major, total), major),
                new PieChart.Data(percent(minor, total), minor),
                new PieChart.Data(percent(onTime, total), onTime)
        );

        lblOnTime.setText(
                "● On Time (< 3 min): " + onTime + " (" + percent(onTime, total) + ")"
        );
        lblOnTime.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

        lblMinorDelay.setText(
                "● Minor Delay (3–14 min): " + minor + " (" + percent(minor, total) + ")"
        );
        lblMinorDelay.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");

        lblMajorDelay.setText(
                "● Significant Delay (≥ 15 min): " + major + " (" + percent(major, total) + ")"
        );
        lblMajorDelay.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
    }

    private String percent(int value, int total) {
        if (total == 0) return "0%";
        return String.format("%.1f%%", (value * 100.0) / total);
    }

    // =====================
    // Stay Duration
    // =====================
    private void drawStayDuration(TimeReportDTO dto) {

        timesChart.getData().clear();
        if (dto.getAvgStayMinutesPerDay() == null) return;

        XYChart.Series<String, Number> series =
                new XYChart.Series<>();
        series.setName("Average Stay (minutes)");

        int daysInMonth = reportMonth.lengthOfMonth();

        for (int day = 1; day <= daysInMonth; day++) {
            int value = dto.getAvgStayMinutesPerDay()
                    .getOrDefault(day, 0);

            series.getData().add(
                    new XYChart.Data<>(String.valueOf(day), value)
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
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override public void handleConnectionError(Exception e) {}
    @Override public void handleConnectionClosed() {}
}
