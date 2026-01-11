package guiControllers;

import application.ChatClient;
import entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Subscribers Report Controller
 *
 * Responsible for displaying monthly subscribers-related reports.
 *
 * Data will be provided later via DTOs from the server.
 */
public class SubscribersReportController {

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

    /**
     * Injected from previous screen
     */
    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
    }

    /**
     * Called automatically after FXML load
     */
    @FXML
    public void initialize() {

        /*
         * ===== FUTURE FLOW =====
         *
         * 1. Send RequestDTO to server:
         *      - GET_SUBSCRIBERS_REPORT
         *
         * 2. Server responds with SubscribersReportDTO:
         *      - activeSubscribersCount
         *      - inactiveSubscribersCount
         *      - waitingListPerDay
         *      - reservationsPerDay
         *
         * 3. Populate:
         *      - PieChart (active vs inactive)
         *      - BarChart (waiting list per day)
         *      - LineChart (reservations trend)
         */

        // Future examples:
        // requestSubscribersActivityReport();
    }

    // =========================
    // Future Server Requests
    // =========================

    /**
     * TODO:
     * Request subscribers report DTO from server
     */
    private void requestSubscribersActivityReport() {
        // Example future code:
        //
        // SubscribersReportRequestDTO dto =
        //      new SubscribersReportRequestDTO(year, month);
        //
        // RequestDTO request =
        //      new RequestDTO(Commands.GET_SUBSCRIBERS_REPORT, dto);
        //
        // chatClient.sendToServer(request);
    }

    // =========================
    // Navigation
    // =========================

    @FXML
    private void onBackClicked() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/ReportsMenu.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof ReportsMenuController) {
                ((ReportsMenuController) controller)
                        .setClient(user, chatClient);
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle("Bistro - Reports");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
