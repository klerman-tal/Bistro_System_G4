package guiControllers;

import interfaces.ClientActions;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;

public class ReportsScreenController {

    @FXML
    private BarChart<Number, String> timesChart;

    private ClientActions clientActions;

    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    @FXML
    public void initialize() {
        loadDummyData();
    }

    private void loadDummyData() {

        XYChart.Series<Number, String> arrivalDelay =
                new XYChart.Series<>();
        arrivalDelay.setName("Arrival Delay (min)");

        XYChart.Series<Number, String> stayDuration =
                new XYChart.Series<>();
        stayDuration.setName("Stay Duration (min)");

        arrivalDelay.getData().add(new XYChart.Data<>(5, "01"));
        arrivalDelay.getData().add(new XYChart.Data<>(10, "02"));
        arrivalDelay.getData().add(new XYChart.Data<>(0, "03"));

        stayDuration.getData().add(new XYChart.Data<>(90, "01"));
        stayDuration.getData().add(new XYChart.Data<>(120, "02"));
        stayDuration.getData().add(new XYChart.Data<>(75, "03"));

        timesChart.getData().addAll(arrivalDelay, stayDuration);
    }
}
