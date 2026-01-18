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
 * JavaFX controller for displaying a monthly time report.
 * <p>
 * This screen presents two perspectives for a selected month:
 * <ul>
 *   <li><b>Arrival status</b>: distribution of on-time arrivals and delays (PieChart)</li>
 *   <li><b>Stay duration</b>: average stay duration per day (BarChart) and related KPIs</li>
 * </ul>
 * </p>
 * The controller requests a {@link TimeReportDTO} from the server and updates the UI
 * asynchronously upon receiving a {@link ResponseDTO} via {@link ClientResponseHandler}.
 */
public class TimeReportController implements ClientResponseHandler {

    /* ======================= Constants ======================= */
    private static final int MONTHS_BACK = 12;
    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    /* ======================= Root ======================= */
    @FXML private BorderPane rootPane;
    @FXML private ComboBox<YearMonth> cmbReportMonth;

    /* ======================= Arrival summary ======================= */
    @FXML private Label lblSummaryTitle;
    @FXML private Text txtSummaryText;

    /* ======================= Stay summary ======================= */
    @FXML private Label lblStaySummaryTitle;
    @FXML private Text txtStaySummaryText;

    /* ======================= Arrival status ======================= */
    @FXML private PieChart arrivalPieChart;
    @FXML private Label lblOnTime;
    @FXML private Label lblMinorDelay;
    @FXML private Label lblMajorDelay;

    /* ======================= Stay duration ======================= */
    @FXML private BarChart<String, Number> timesChart;
    @FXML private Label lblMonthlyAvg;
    @FXML private Label lblMaxDay;
    @FXML private Label lblMinDay;

    private ChatClient chatClient;
    private User user;
    private YearMonth reportMonth;

    /* ======================= Initialization ======================= */

    /**
     * JavaFX initialization hook.
     * <p>
     * Configures chart appearance (hides legends) and initializes the report-month selector.
     * </p>
     */
    @FXML
    public void initialize() {
        arrivalPieChart.setLegendVisible(false);
        timesChart.setLegendVisible(false);
        initMonthComboBox();
    }

    /**
     * Injects the current session context and registers this controller as the active
     * {@link ClientResponseHandler}.
     * <p>
     * After injection, the controller triggers a report request for the currently selected month.
     * </p>
     *
     * @param user the current user viewing the reports
     * @param chatClient the active client connection used for server communication
     */
    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;

        if (this.chatClient != null) {
            this.chatClient.setResponseHandler(this);
        }

        onMonthSelected(cmbReportMonth.getValue());
    }

    /* ======================= Month Selection ======================= */

    /**
     * Initializes the month ComboBox with the last {@link #MONTHS_BACK} months,
     * defaulting to the most recent complete month.
     * <p>
     * Also applies a custom cell renderer to display months using {@link #MONTH_FORMATTER}
     * and registers the selection handler.
     * </p>
     */
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

    /**
     * Handles a month selection change.
     * <p>
     * Updates UI summaries and requests the report data from the server.
     * </p>
     *
     * @param ym the selected month
     */
    private void onMonthSelected(YearMonth ym) {
        if (ym == null) return;

        this.reportMonth = ym;
        updateArrivalSummary();
        updateStaySummary();
        requestTimeReport(ym.getYear(), ym.getMonthValue());
    }

    /* ======================= Request ======================= */

    /**
     * Sends a time report request to the server for the specified year and month.
     *
     * @param year the report year
     * @param month the report month (1-12)
     */
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

    /**
     * Receives and processes asynchronous server responses.
     * <p>
     * On a successful response containing {@link TimeReportDTO}, the controller updates both charts.
     * UI updates are performed on the JavaFX Application Thread using {@link Platform#runLater(Runnable)}.
     * </p>
     *
     * @param response the server response wrapper
     */
    @Override
    public void handleResponse(ResponseDTO response) {
        if (response == null || !response.isSuccess()) return;
        if (!(response.getData() instanceof TimeReportDTO dto)) return;

        Platform.runLater(() -> {
            drawArrivalStatus(dto);
            drawStayDuration(dto);
        });
    }
    
    private String formatDelta(Integer delta) {
        if (delta == null) return "";
        if (delta > 0) return " ▲ +" + delta;
        if (delta < 0) return " ▼ " + delta;
        return " —";
    }


    /* ======================= Summaries ======================= */

    /**
     * Updates the arrival summary title and description text for the selected month.
     */
    private void updateArrivalSummary() {
        lblSummaryTitle.setText("Arrival Status: " + reportMonth.format(MONTH_FORMATTER));
        txtSummaryText.setText(
                "Displays the distribution of customer arrival times, showing on-time arrivals and delays.");
    }

    /**
     * Updates the stay-duration summary title and description text for the selected month.
     */
    private void updateStaySummary() {
        lblStaySummaryTitle.setText("Stay Duration: " + reportMonth.format(MONTH_FORMATTER));
        txtStaySummaryText.setText(
                "Shows the average customer stay duration per day for the selected month.");
    }

    /* ======================= Charts ======================= */

    /**
     * Draws the arrival status PieChart and updates the related KPI labels.
     *
     * @param dto the report data transfer object containing arrival status counts
     */
    private void drawArrivalStatus(TimeReportDTO dto) {
        int onTime = dto.getOnTimeCount();
        int minor = dto.getMinorDelayCount();
        int major = dto.getSignificantDelayCount();
        int total = onTime + minor + major;

        arrivalPieChart.getData().setAll(
                new PieChart.Data(percent(onTime, total), onTime),
                new PieChart.Data(percent(major, total), major),
                new PieChart.Data(percent(minor, total), minor)
        );

        lblOnTime.setText(
        	    "● On Time (< 3 min): " + onTime +
        	    " (" + percent(onTime, total) + ")" +
        	    formatDelta(dto.getOnTimeDelta())
        	);

        lblMinorDelay.setText(
        	    "● Minor Delay (3–10 min): " + minor +
        	    " (" + percent(minor, total) + ")" +
        	    formatDelta(dto.getMinorDelayDelta())
        	);

        lblMajorDelay.setText(
        	    "● Significant Delay (≥ 10 min): " + major +
        	    " (" + percent(major, total) + ")" +
        	    formatDelta(dto.getSignificantDelayDelta())
        	);

    }

    /**
     * Draws the stay-duration BarChart for the selected month and updates summary labels
     * (monthly average, longest day, shortest day).
     *
     * @param dto the report data transfer object containing stay duration metrics
     */
    private void drawStayDuration(TimeReportDTO dto) {
        timesChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        int days = reportMonth.lengthOfMonth();

        for (int day = 1; day <= days; day++) {
            int value = dto.getAvgStayMinutesPerDay().getOrDefault(day, 0);
            series.getData().add(new XYChart.Data<>(String.valueOf(day), value));
        }

        timesChart.getData().add(series);

        lblMonthlyAvg.setText(
        	    "Monthly Average: " + dto.getMonthlyAvgStay() + " min" +
        	    formatDelta(dto.getMonthlyAvgDelta())
        	);

        lblMaxDay.setText("Longest Stay: Day " + dto.getMaxAvgDay() +
                " (" + dto.getMaxAvgMinutes() + " min)");
        lblMinDay.setText("Shortest Stay: Day " + dto.getMinAvgDay() +
                " (" + dto.getMinAvgMinutes() + " min)");
    }

    /**
     * Formats a percentage string for the given value out of a total.
     *
     * @param value the part value
     * @param total the total value
     * @return a formatted percentage string (e.g., "12.5%")
     */
    private String percent(int value, int total) {
        return total == 0 ? "0%" : String.format("%.1f%%", (value * 100.0) / total);
    }

    /* ======================= Navigation ======================= */

    /**
     * Navigates back to the reports menu screen.
     * <p>
     * Clears this controller as the active response handler and reuses the existing {@link Scene}
     * by replacing its root node.
     * </p>
     */
    @FXML
    private void onBackClicked() {
        try {
            if (chatClient != null) {
                chatClient.setResponseHandler(null);
            }

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/ReportsMenu.fxml"));
            Parent root = loader.load();

            ReportsMenuController controller = loader.getController();
            controller.setClient(user, chatClient);

            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = stage.getScene();

            scene.setRoot(root);
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when a connection error occurs while this controller is active.
     *
     * @param e the connection exception
     */
    @Override public void handleConnectionError(Exception e) {}

    /**
     * Called when the server connection is closed while this controller is active.
     */
    @Override public void handleConnectionClosed() {}
}
