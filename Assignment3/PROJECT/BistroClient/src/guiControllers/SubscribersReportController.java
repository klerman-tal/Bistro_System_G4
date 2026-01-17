package guiControllers;

import application.ChatClient;
import dto.RequestDTO;
import dto.ResponseDTO;
import dto.SubscribersReportDTO;
import entities.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
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

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SubscribersReportController implements ClientResponseHandler {

    /* ======================= Constants ======================= */
    private static final int MONTHS_BACK = 12;
    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    /* ======================= Root ======================= */
    @FXML private BorderPane rootPane;
    @FXML private ComboBox<YearMonth> cmbReportMonth;

    /* ======================= Subscribers summary ======================= */
    @FXML private Label lblSubscribersSummaryTitle;
    @FXML private Text txtSubscribersSummaryText;
    @FXML private Label lblWaitingTotal;
    @FXML private Label lblWaitingAvg;
    @FXML private Label lblWaitingPeak;

    /* ======================= Waiting list summary ======================= */
    @FXML private Label lblWaitingSummaryTitle;
    @FXML private Text txtWaitingSummaryText;

    /* ======================= Reservations summary ======================= */
    @FXML private Label lblReservationsSummaryTitle;
    @FXML private Text txtReservationsSummaryText;
    @FXML private Label lblReservationsTotal;
    @FXML private Label lblReservationsAvg;
    @FXML private Label lblReservationsPeak;

    /* ======================= Charts ======================= */
    @FXML private PieChart activeSubscribersPie;
    @FXML private Label lblActiveSubscribers;
    @FXML private Label lblInactiveSubscribers;

    @FXML private BarChart<String, Number> waitingListChart;
    @FXML private LineChart<String, Number> reservationsTrendChart;

    private User user;
    private ChatClient chatClient;
    private YearMonth reportMonth;

    /* ======================= INIT ======================= */

    /**
     * JavaFX controller for the Subscribers Report screen.
     * <p>
     * Responsibilities:
     * <ul>
     *   <li>Allows selecting a report month (default: last full month).</li>
     *   <li>Requests the subscribers report data from the server.</li>
     *   <li>Displays summary text and renders charts:
     *       active vs. inactive subscribers, waiting list activity, and reservations trend.</li>
     * </ul>
     * </p>
     */
    @FXML
    public void initialize() {
        activeSubscribersPie.setLegendVisible(false);
        waitingListChart.setLegendVisible(false);
        reservationsTrendChart.setLegendVisible(false);
        initMonthComboBox();
    }

    /**
     * Injects the current user and ChatClient session into this controller and registers
     * this controller as the active response handler.
     * <p>
     * After setup, the controller triggers loading the report for the currently selected month.
     * </p>
     *
     * @param user the currently logged-in user (used for session context)
     * @param chatClient the active client connection to the server
     */
    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
        this.chatClient.setResponseHandler(this);

        onMonthSelected(cmbReportMonth.getValue());
    }

    /* ======================= MONTH SELECTION ======================= */

    /**
     * Initializes the month ComboBox with the last {@link #MONTHS_BACK} months.
     * <p>
     * The default selected month is the last full month (current month minus one).
     * The ComboBox displays months using {@link #MONTH_FORMATTER}.
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

        cmbReportMonth.setOnAction(e ->
                onMonthSelected(cmbReportMonth.getValue()));
    }

    /**
     * Handles month selection changes.
     * <p>
     * Updates UI summary headers/text and sends a request to the server for the selected month.
     * </p>
     *
     * @param ym the selected report month
     */
    private void onMonthSelected(YearMonth ym) {
        if (ym == null) return;

        this.reportMonth = ym;
        updateSubscribersSummary();
        updateWaitingSummary();
        updateReservationsSummary();
        requestSubscribersReport();
    }

    /* ======================= SERVER REQUEST ======================= */

    /**
     * Sends a {@link Commands#GET_SUBSCRIBERS_REPORT} request to the server for the selected month.
     * The request payload includes year and month values in {@link SubscribersReportDTO}.
     */
    private void requestSubscribersReport() {
        SubscribersReportDTO dto = new SubscribersReportDTO();
        dto.setYear(reportMonth.getYear());
        dto.setMonth(reportMonth.getMonthValue());

        try {
            chatClient.sendToServer(
                    new RequestDTO(Commands.GET_SUBSCRIBERS_REPORT, dto));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ======================= SERVER RESPONSE ======================= */

    /**
     * Receives server responses for this screen.
     * <p>
     * On a successful response containing {@link SubscribersReportDTO},
     * updates charts on the JavaFX Application Thread.
     * </p>
     *
     * @param response server response wrapper
     */
    @Override
    public void handleResponse(ResponseDTO response) {
        if (response == null || !response.isSuccess()) return;
        if (!(response.getData() instanceof SubscribersReportDTO dto)) return;

        Platform.runLater(() -> {
            drawSubscribersStatus(dto);
            drawWaitingList(dto);
            drawReservationsTrend(dto);
        });
    }

    /* ======================= SUMMARIES ======================= */

    /**
     * Updates the subscribers status summary header and description text for the selected month.
     */
    private void updateSubscribersSummary() {
        lblSubscribersSummaryTitle.setText(
                "Subscribers Status: " + reportMonth.format(MONTH_FORMATTER));
        txtSubscribersSummaryText.setText(
                "Shows the number of active versus inactive subscribers during the selected month.");
    }

    /**
     * Updates the waiting list activity summary header and description text for the selected month.
     */
    private void updateWaitingSummary() {
        lblWaitingSummaryTitle.setText(
                "Waiting List Activity: " + reportMonth.format(MONTH_FORMATTER));
        txtWaitingSummaryText.setText(
                "Displays daily waiting list entries joined by subscribers throughout the selected month.");
    }

    /**
     * Updates the reservations trend summary header and description text for the selected month.
     */
    private void updateReservationsSummary() {
        lblReservationsSummaryTitle.setText(
                "Reservations Trend: " + reportMonth.format(MONTH_FORMATTER));
        txtReservationsSummaryText.setText(
                "Presents the daily number of reservations made by subscribers.");
    }

    /* ======================= CHARTS ======================= */

    /**
     * Renders a pie chart showing active vs. inactive subscribers for the selected month,
     * and updates the legend labels with counts and percentages.
     *
     * @param dto report data returned from the server
     */
    private void drawSubscribersStatus(SubscribersReportDTO dto) {
        int active = dto.getActiveSubscribersCount();
        int inactive = dto.getInactiveSubscribersCount();
        int total = active + inactive;

        activeSubscribersPie.getData().setAll(
                new PieChart.Data(percent(active, total), active),
                new PieChart.Data(percent(inactive, total), inactive)
        );

        lblActiveSubscribers.setText("● Active Subscribers: " + active +
                " (" + percent(active, total) + ")");
        lblInactiveSubscribers.setText("● Inactive Subscribers: " + inactive +
                " (" + percent(inactive, total) + ")");
    }

    /**
     * Renders a bar chart of waiting list entries per day for the selected month,
     * and computes summary metrics (total, average, and peak day).
     *
     * @param dto report data returned from the server
     */
    private void drawWaitingList(SubscribersReportDTO dto) {
        waitingListChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<Integer, Integer> data = dto.getWaitingListPerDay();
        int days = reportMonth.lengthOfMonth();

        int total = 0, maxDay = -1, maxCount = 0;

        for (int day = 1; day <= days; day++) {
            int value = data.getOrDefault(day, 0);
            total += value;

            if (value > maxCount) {
                maxCount = value;
                maxDay = day;
            }
            series.getData().add(new XYChart.Data<>(String.valueOf(day), value));
        }

        waitingListChart.getData().add(series);

        lblWaitingTotal.setText("Total Waiting Entries: " + total);
        lblWaitingAvg.setText(String.format("Daily Average: %.1f", (double) total / days));
        lblWaitingPeak.setText(
                maxDay == -1 ? "Peak Day: N/A"
                        : "Peak Day: Day " + maxDay + " (" + maxCount + ")");
    }

    /**
     * Renders a line chart of reservations per day for the selected month,
     * and computes summary metrics (total, average, and peak day).
     *
     * @param dto report data returned from the server
     */
    private void drawReservationsTrend(SubscribersReportDTO dto) {
        reservationsTrendChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<Integer, Integer> data = dto.getReservationsPerDay();
        int days = reportMonth.lengthOfMonth();

        int total = 0, maxDay = -1, maxCount = 0;

        for (int day = 1; day <= days; day++) {
            int value = data.getOrDefault(day, 0);
            total += value;

            if (value > maxCount) {
                maxCount = value;
                maxDay = day;
            }
            series.getData().add(new XYChart.Data<>(String.valueOf(day), value));
        }

        reservationsTrendChart.getData().add(series);

        lblReservationsTotal.setText("Total Reservations: " + total);
        lblReservationsAvg.setText(String.format("Daily Average: %.1f", (double) total / days));
        lblReservationsPeak.setText(
                maxDay == -1 ? "Peak Day: N/A"
                        : "Peak Day: Day " + maxDay + " (" + maxCount + ")");
    }

    /**
     * Calculates a percentage string (one decimal place) for the given value out of total.
     *
     * @param value numerator
     * @param total denominator
     * @return formatted percentage string (e.g., "42.5%")
     */
    private String percent(int value, int total) {
        return total == 0 ? "0%" : String.format("%.1f%%", (value * 100.0) / total);
    }

    /* ======================= NAVIGATION ======================= */

    /**
     * Navigates back to the Reports menu screen (ReportsMenu.fxml).
     * Also clears the current response handler from the ChatClient.
     */
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
            Scene scene = stage.getScene();

            scene.setRoot(root);
            stage.setTitle("Bistro - Reports");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when a connection error occurs.
     */
    @Override public void handleConnectionError(Exception e) {}

    /**
     * Called when the connection is closed.
     */
    @Override public void handleConnectionClosed() {}
}
