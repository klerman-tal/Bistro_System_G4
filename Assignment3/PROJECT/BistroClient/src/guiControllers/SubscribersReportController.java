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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Displays monthly subscribers-related reports.
 */
public class SubscribersReportController implements ClientResponseHandler {

    /* ======================= Constants ======================= */
    private static final int MONTHS_BACK = 12;
    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    /* ======================= Root ======================= */
    @FXML
    private BorderPane rootPane;
    @FXML
    private ComboBox<YearMonth> cmbReportMonth;

    /* ======================= Subscribers summary ======================= */
    @FXML
    private Label lblSubscribersSummaryTitle;
    @FXML
    private Text txtSubscribersSummaryText;
    @FXML
    private Label lblWaitingTotal;
    @FXML
    private Label lblWaitingAvg;
    @FXML
    private Label lblWaitingPeak;

    /* ======================= Waiting list summary ======================= */
    @FXML
    private Label lblWaitingSummaryTitle;
    @FXML
    private Text txtWaitingSummaryText;

    /* ======================= Reservations summary ======================= */
    @FXML
    private Label lblReservationsSummaryTitle;
    @FXML
    private Text txtReservationsSummaryText;
    @FXML
    private Label lblReservationsTotal;
    @FXML
    private Label lblReservationsAvg;
    @FXML
    private Label lblReservationsPeak;

    /* ======================= Charts ======================= */
    @FXML
    private PieChart activeSubscribersPie;
    @FXML
    private Label lblActiveSubscribers;
    @FXML
    private Label lblInactiveSubscribers;

    @FXML
    private BarChart<String, Number> waitingListChart;
    @FXML
    private LineChart<String, Number> reservationsTrendChart;

    private User user;
    private ChatClient chatClient;
    private YearMonth reportMonth;

    /* ======================= Initialization ======================= */
    @FXML
    public void initialize() {
        activeSubscribersPie.setLegendVisible(false);
        waitingListChart.setLegendVisible(false);
        reservationsTrendChart.setLegendVisible(false);
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
        updateSubscribersSummary();
        updateWaitingSummary();
        updateReservationsSummary();
        requestSubscribersReport();
    }

    /* ======================= Request ======================= */
    private void requestSubscribersReport() {
        SubscribersReportDTO dto = new SubscribersReportDTO();
        dto.setYear(reportMonth.getYear());
        dto.setMonth(reportMonth.getMonthValue());

        try {
            chatClient.sendToServer(new RequestDTO(Commands.GET_SUBSCRIBERS_REPORT, dto));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ======================= Response ======================= */
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

    /* ======================= Summaries ======================= */
    private void updateSubscribersSummary() {
        lblSubscribersSummaryTitle.setText(
                "Subscribers Status: " + reportMonth.format(MONTH_FORMATTER));
        txtSubscribersSummaryText.setText(
                "Shows the number of active versus inactive subscribers during the selected month.");
    }

    private void updateWaitingSummary() {
        lblWaitingSummaryTitle.setText(
                "Waiting List Activity: " + reportMonth.format(MONTH_FORMATTER));
        txtWaitingSummaryText.setText(
                "Displays daily waiting list entries joined by subscribers throughout the selected month.");
    }

    private void updateReservationsSummary() {
        lblReservationsSummaryTitle.setText(
                "Reservations Trend: " + reportMonth.format(MONTH_FORMATTER));
        txtReservationsSummaryText.setText(
                "Presents the daily number of reservations made by subscribers.");
    }

    /* ======================= Charts ======================= */
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
        lblWaitingPeak.setText(maxDay == -1 ? "Peak Day: N/A" :
                "Peak Day: Day " + maxDay + " (" + maxCount + ")");
    }

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
        lblReservationsPeak.setText(maxDay == -1 ? "Peak Day: N/A" :
                "Peak Day: Day " + maxDay + " (" + maxCount + ")");
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
