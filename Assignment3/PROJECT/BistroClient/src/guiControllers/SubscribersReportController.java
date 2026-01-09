package guiControllers;

import interfaces.ClientActions;
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
 * This controller is responsible for displaying
 * monthly subscribers-related reports.
 *
 * Data will be provided later via DTOs from the server.
 */
public class SubscribersReportController {

    // ===== Root =====
    @FXML private BorderPane rootPane;

    // ===== TAB 1: Active Subscribers =====
    @FXML private PieChart activeSubscribersPie;
    @FXML private Label lblActiveSubscribers;
    @FXML private Label lblInactiveSubscribers;

    // ===== TAB 2: Waiting List Activity =====
    @FXML private BarChart<String, Number> waitingListChart;

    // ===== TAB 3: Reservation Trend =====
    @FXML private LineChart<String, Number> reservationsTrendChart;

    private ClientActions clientActions;

    /**
     * Injected from previous screen
     */
    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    /**
     * Called automatically after FXML load
     */
    @FXML
    public void initialize() {

        /*
         * ===== FUTURE FLOW =====
         *
         * 1. Request subscribers report data from server
         * 2. Receive DTO containing:
         *    - Active / inactive subscribers counts
         *    - Waiting list entries per day
         *    - Reservations per day
         * 3. Populate charts accordingly
         */

        // Example future calls:
        // requestActiveSubscribersData();
        // requestWaitingListData();
        // requestReservationTrendData();
    }

    // =========================
    // Future Server Requests
    // =========================

    /**
     * TODO:
     * Request active vs inactive subscribers for selected month
     */
    private void requestActiveSubscribersData() {
        // clientActions.sendToServer(...)
    }

    /**
     * TODO:
     * Request waiting list entries per day (subscribers only)
     */
    private void requestWaitingListData() {
        // clientActions.sendToServer(...)
    }

    /**
     * TODO:
     * Request subscriber reservations trend per day
     */
    private void requestReservationTrendData() {
        // clientActions.sendToServer(...)
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
