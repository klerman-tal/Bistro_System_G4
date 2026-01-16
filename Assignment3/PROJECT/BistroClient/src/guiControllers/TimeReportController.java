package guiControllers;

import application.ChatClient;
import dto.RequestDTO;
import dto.ResponseDTO;
import dto.TimeReportDTO;
import entities.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import network.ClientResponseHandler;
import protocol.Commands;

import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controller for displaying monthly time reports.
 */
public class TimeReportController implements ClientResponseHandler {

    /* ======================= Constants ======================= */
    private static final int MONTHS_BACK = 12;
    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    /* ======================= Root ======================= */
    @FXML
    private BorderPane rootPane;
    @FXML
    private ComboBox<YearMonth> cmbReportMonth;

    /* ======================= Arrival summary ======================= */
    @FXML
    private Label lblSummaryTitle;
    @FXML
    private Text txtSummaryText;

    /* ======================= Stay summary ======================= */
    @FXML
    private Label lblStaySummaryTitle;
    @FXML
    private Text txtStaySummaryText;

    /* ======================= Arrival status ======================= */
    @FXML
    private PieChart arrivalPieChart;
    @FXML
    private Label lblOnTime;
    @FXML
    private Label lblMinorDelay;
    @FXML
    private Label lblMajorDelay;

    /* ======================= Stay duration ======================= */
    @FXML
    private BarChart<String, Number> timesChart;
    @FXML
    private Label lblMonthlyAvg;
    @FXML
    private Label lblMaxDay;
    @FXML
    private Label lblMinDay;

    private ChatClient chatClient;
    private User user;
    private YearMonth reportMonth;

    /* ======================= Initialization ======================= */
    @FXML
    public void initialize() {
        arrivalPieChart.setLegendVisible(false);
        timesChart.setLegendVisible(false);
        initMonthComboBox();
    }

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
        this.chatClient.setResponseHandler(this);

        onMonthSelected(cmbReportMonth.getValue());
    }

    /* ======================= Month Selection ======================= */
    private void initMonthComboBox() {
        YearMonth defaultMonth = YearMonth.now().minusMonths(1);

        List<YearMonth> months = new ArrayList<>();
        for (int i = 0; i < MONTHS_BACK; i++) {
            months.add(defaultMonth.minusMonths(i));
        }

        cmbReportMonth.setItems(FXCollections.observableArrayList(months));
        cmbReportMonth.setValue(defaultMonth);

        cmbReportMonth.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(YearMonth item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(MONTH_FORMATTER));
            }
        });
        cmbReportMonth.setButtonCell(cmbReportMonth.getCellFactory().call(null));

        cmbReportMonth.setOnAction(e -> onMonthSelected(cmbReportMonth.getValue()));
    }

    private void onMonthSelected(YearMonth ym) {
        if (ym == null) return;

        this.reportMonth = ym;
        updateArrivalSummary();
        updateStaySummary();
        requestTimeReport(ym.getYear(), ym.getMonthValue());
    }

    /* ======================= Request ======================= */
    private void requestTimeReport(int year, int month) {
        TimeReportDTO dto = new TimeReportDTO();
        dto.setYear(year);
        dto.setMonth(month);

        try {
            chatClient.sendToServer(new RequestDTO(Commands.GET_TIME_REPORT, dto));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* ======================= Response ======================= */
    @Override
    public void handleResponse(ResponseDTO response) {
        if (response == null || !response.isSuccess()) return;
        if (!(response.getData() instanceof TimeReportDTO dto)) return;

        Platform.runLater(() -> {
            drawArrivalStatus(dto);
            drawStayDuration(dto);
        });
    }

    /* ======================= Summaries ======================= */
    private void updateArrivalSummary() {
        lblSummaryTitle.setText("Arrival Status: " + reportMonth.format(MONTH_FORMATTER));
        txtSummaryText.setText(
                "Displays the distribution of customer arrival times, showing on-time arrivals and delays.");
    }

    private void updateStaySummary() {
        lblStaySummaryTitle.setText("Stay Duration: " + reportMonth.format(MONTH_FORMATTER));
        txtStaySummaryText.setText(
                "Shows the average customer stay duration per day for the selected month.");
    }

    /* ======================= Charts ======================= */
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

        lblOnTime.setText("● On Time (< 3 min): " + onTime + " (" + percent(onTime, total) + ")");
        lblMinorDelay.setText("● Minor Delay (3–10 min): " + minor + " (" + percent(minor, total) + ")");
        lblMajorDelay.setText("● Significant Delay (≥ 10 min): " + major + " (" + percent(major, total) + ")");
    }

    private void drawStayDuration(TimeReportDTO dto) {
        timesChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        int days = reportMonth.lengthOfMonth();

        for (int day = 1; day <= days; day++) {
            int value = dto.getAvgStayMinutesPerDay().getOrDefault(day, 0);
            series.getData().add(new XYChart.Data<>(String.valueOf(day), value));
        }

        timesChart.getData().add(series);

        lblMonthlyAvg.setText("Monthly Average: " + dto.getMonthlyAvgStay() + " min");
        lblMaxDay.setText("Longest Stay: Day " + dto.getMaxAvgDay() +
                " (" + dto.getMaxAvgMinutes() + " min)");
        lblMinDay.setText("Shortest Stay: Day " + dto.getMinAvgDay() +
                " (" + dto.getMinAvgMinutes() + " min)");
    }

    private String percent(int value, int total) {
        return total == 0 ? "0%" : String.format("%.1f%%", (value * 100.0) / total);
    }

    /* ======================= Navigation ======================= */
    @FXML
    private void onBackClicked() {
        try {
            chatClient.setResponseHandler(null);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ReportsMenu.fxml"));
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

    @Override
    public void handleConnectionError(Exception e) {}
    @Override
    public void handleConnectionClosed() {}
}
