package guiControllers;

import application.ChatClient;
import dto.RequestDTO;
import dto.ResponseDTO;
import dto.SubscribersReportDTO;
import entities.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import network.ClientResponseHandler;
import protocol.Commands;

import java.time.YearMonth;
import java.util.Map;

/**
 * SubscribersReportController
 *
 * Displays monthly subscribers-related reports:
 * 1. Active vs Inactive subscribers
 * 2. Waiting list activity per day
 * 3. Subscribers reservations trend per day
 */
public class SubscribersReportController implements ClientResponseHandler {

    // ===== Root =====
    @FXML
    private BorderPane rootPane;

    // ===== TAB 1: Active Subscribers =====
    @FXML
    private PieChart activeSubscribersPie;
    @FXML
    private Label lblActiveSubscribers;
    @FXML
    private Label lblInactiveSubscribers;

    // ===== TAB 2: Waiting List Activity =====
    @FXML
    private BarChart<String, Number> waitingListChart;

    // ===== TAB 3: Reservation Trend =====
    @FXML
    private LineChart<String, Number> reservationsTrendChart;

    // ===== Client context =====
    private User user;
    private ChatClient chatClient;
    private YearMonth reportMonth;

    // =====================
    // Init
    // =====================
    @FXML
    public void initialize() {
        activeSubscribersPie.setLegendVisible(false);
    }

    /**
     * Injected from previous screen
     */
    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;

        chatClient.setResponseHandler(this);

        // Always display the last full month
        reportMonth = YearMonth.now().minusMonths(1);

        requestSubscribersReport();
    }

    // =====================
    // Request
    // =====================
    private void requestSubscribersReport() {

        SubscribersReportDTO dto = new SubscribersReportDTO();
        dto.setYear(reportMonth.getYear());
        dto.setMonth(reportMonth.getMonthValue());

        try {
            chatClient.sendToServer(
                    new RequestDTO(Commands.GET_SUBSCRIBERS_REPORT, dto)
            );
            System.out.println("📤 Sent SubscribersReport request");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =====================
    // Response
    // =====================
    @Override
    public void handleResponse(ResponseDTO response) {

        if (response == null || !response.isSuccess()) return;
        if (!(response.getData() instanceof SubscribersReportDTO)) return;

        SubscribersReportDTO dto =
                (SubscribersReportDTO) response.getData();

        Platform.runLater(() -> {
            drawSubscribersStatus(dto);
            drawWaitingList(dto);
            drawReservationsTrend(dto);
        });
    }

    // =====================
    // TAB 1 – Active vs Inactive
    // =====================
    private void drawSubscribersStatus(SubscribersReportDTO dto) {

        int active = dto.getActiveSubscribersCount();
        int inactive = dto.getInactiveSubscribersCount();

        activeSubscribersPie.getData().setAll(
                new PieChart.Data("Active", active),
                new PieChart.Data("Inactive", inactive)
        );

        lblActiveSubscribers.setText(
                "Active Subscribers: " + active);
        lblInactiveSubscribers.setText(
                "Inactive Subscribers: " + inactive);
    }

    // =====================
    // TAB 2 – Waiting List per Day
    // =====================
    private void drawWaitingList(SubscribersReportDTO dto) {

        waitingListChart.getData().clear();

        XYChart.Series<String, Number> series =
                new XYChart.Series<>();
        series.setName("Waiting List Activity");

        int daysInMonth = reportMonth.lengthOfMonth();
        Map<Integer, Integer> data =
                dto.getWaitingListPerDay();

        for (int day = 1; day <= daysInMonth; day++) {
            int value = data.getOrDefault(day, 0);
            series.getData().add(
                    new XYChart.Data<>(
                            String.valueOf(day),
                            value
                    )
            );
        }

        waitingListChart.getData().add(series);
    }

    // =====================
    // TAB 3 – Reservations Trend
    // =====================
    private void drawReservationsTrend(SubscribersReportDTO dto) {

        reservationsTrendChart.getData().clear();

        XYChart.Series<String, Number> series =
                new XYChart.Series<>();
        series.setName("Subscribers Reservations");

        int daysInMonth = reportMonth.lengthOfMonth();
        Map<Integer, Integer> data =
                dto.getReservationsPerDay();

        for (int day = 1; day <= daysInMonth; day++) {
            int value = data.getOrDefault(day, 0);
            series.getData().add(
                    new XYChart.Data<>(
                            String.valueOf(day),
                            value
                    )
            );
        }

        reservationsTrendChart.getData().add(series);
    }

    // =====================
    // Navigation
    // =====================
    @FXML
    private void onBackClicked() {
        try {
            chatClient.setResponseHandler(null);

            FXMLLoader loader =
                    new FXMLLoader(getClass()
                            .getResource("/gui/ReportsMenu.fxml"));
            Parent root = loader.load();

            ReportsMenuController controller =
                    loader.getController();
            controller.setClient(user, chatClient);

            Stage stage =
                    (Stage) rootPane.getScene().getWindow();
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
