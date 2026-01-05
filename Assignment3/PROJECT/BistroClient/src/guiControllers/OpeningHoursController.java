package guiControllers;

import application.ClientUI;
import entities.OpeningHouers;
import entities.Restaurant;
import interfaces.ChatIF;
import interfaces.ClientActions;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;

public class OpeningHoursController implements ChatIF {

    @FXML private TableView<OpeningHouers> openingHoursTable;
    @FXML private TableColumn<OpeningHouers, String> dayColumn;
    @FXML private TableColumn<OpeningHouers, String> openTimeColumn;
    @FXML private TableColumn<OpeningHouers, String> closeTimeColumn;

    @FXML private TextField openTimeField; // New Time (HH:MM)
    @FXML private Button updateButton;
    @FXML private Button backButton;

    private ClientActions clientActions;

    private enum EditTarget { OPEN_TIME, CLOSE_TIME }
    private EditTarget editTarget = EditTarget.OPEN_TIME;

    // נקרא מהמסך הקודם אחרי טעינת ה-FXML (כמו UpdateTables)
    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
        requestOpeningHoursFromServer(); // ✅ רק אחרי שהוזרק
    }

    @FXML
    public void initialize() {
        ClientUI.setActiveController(this);

        dayColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDayOfWeek()));
        openTimeColumn.setCellValueFactory(cell -> new SimpleStringProperty(toHHMM(cell.getValue().getOpenTime())));
        closeTimeColumn.setCellValueFactory(cell -> new SimpleStringProperty(toHHMM(cell.getValue().getCloseTime())));

        openingHoursTable.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> detectClickedColumn());

        openingHoursTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected == null) return;
            openTimeField.setText(editTarget == EditTarget.OPEN_TIME
                    ? toHHMM(selected.getOpenTime())
                    : toHHMM(selected.getCloseTime()));
        });

        updateButton.setOnAction(e -> onUpdateClicked());
        backButton.setOnAction(e -> onBackClicked());
    }

    // =====================
    // Client -> Server
    // =====================
    private void requestOpeningHoursFromServer() {
        if (clientActions == null) return;

        ArrayList<String> msg = new ArrayList<>();
        msg.add("RM_GET_OPENING_HOURS");
        clientActions.sendToServer(msg);
    }

    private void sendUpdateToServer(String day, String openHHMM, String closeHHMM) {
        if (clientActions == null) {
            showAlert("clientActions not set");
            return;
        }

        ArrayList<String> msg = new ArrayList<>();
        msg.add("RM_UPDATE_OPENING_HOURS");
        msg.add(day);
        msg.add(openHHMM);
        msg.add(closeHHMM);
        clientActions.sendToServer(msg);
    }

    // =====================
    // Button handlers
    // =====================
    private void onUpdateClicked() {
        OpeningHouers selected = openingHoursTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select a day from the table.");
            return;
        }

        String newTime = openTimeField.getText();
        if (newTime == null || newTime.isBlank()) {
            showAlert("Please enter a time (HH:MM).");
            return;
        }

        newTime = newTime.trim();
        if (!newTime.matches("^\\d{2}:\\d{2}$")) {
            showAlert("Invalid time format. Use HH:MM (e.g., 09:30).");
            return;
        }

        String day = selected.getDayOfWeek();
        String openHHMM = toHHMM(selected.getOpenTime());
        String closeHHMM = toHHMM(selected.getCloseTime());

        if (editTarget == EditTarget.OPEN_TIME) {
            openHHMM = newTime;
            selected.setOpenTime(newTime);
        } else {
            closeHHMM = newTime;
            selected.setCloseTime(newTime);
        }

        openingHoursTable.refresh();
        sendUpdateToServer(day, openHHMM, closeHHMM);
    }

    private void onBackClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/RestaurantManagement_B.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof RestaurantManagement_BController) {
                ((RestaurantManagement_BController) controller).setClientActions(clientActions);
            }

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setTitle("Bistro - Restaurant Management");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Failed to go back.");
        }
    }

    // =====================
    // Server -> Client
    // =====================
    @Override
    public void display(String message) {
        if (message == null) return;

        if (message.startsWith("RM_ERROR|")) {
            String err = message.substring("RM_ERROR|".length());
            Platform.runLater(() -> showAlert(err));
            return;
        }

        if (message.equals("RM_OK") || message.startsWith("RM_OK|")) {
            Platform.runLater(this::requestOpeningHoursFromServer);
            return;
        }

        if (message.startsWith("RM_OPENING_HOURS|")) {
            String payload = message.substring("RM_OPENING_HOURS|".length());
            ArrayList<OpeningHouers> list = parseOpeningHours(payload);

            Platform.runLater(() -> {
                Restaurant.getInstance().setOpeningHours(list);
                openingHoursTable.getItems().setAll(list);
            });
        }
    }

    // =====================
    // Helpers
    // =====================
    private void detectClickedColumn() {
        if (openingHoursTable.getSelectionModel().getSelectedCells().isEmpty()) return;

        TablePosition<OpeningHouers, ?> pos = openingHoursTable.getSelectionModel().getSelectedCells().get(0);
        TableColumn<?, ?> col = pos.getTableColumn();

        if (col == openTimeColumn) editTarget = EditTarget.OPEN_TIME;
        else if (col == closeTimeColumn) editTarget = EditTarget.CLOSE_TIME;

        OpeningHouers selected = openingHoursTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openTimeField.setText(editTarget == EditTarget.OPEN_TIME
                    ? toHHMM(selected.getOpenTime())
                    : toHHMM(selected.getCloseTime()));
        }
    }

    // payload: "Sunday,09:00,23:00;Monday,09:00,23:00;"
    private ArrayList<OpeningHouers> parseOpeningHours(String payload) {
        ArrayList<OpeningHouers> list = new ArrayList<>();
        if (payload == null || payload.isBlank()) return list;

        String[] rows = payload.split(";");
        for (String row : rows) {
            row = row.trim();
            if (row.isEmpty()) continue;

            String[] parts = row.split(",");
            if (parts.length < 3) continue;

            OpeningHouers oh = new OpeningHouers();
            oh.setDayOfWeek(parts[0].trim());
            oh.setOpenTime(parts[1].trim());
            oh.setCloseTime(parts[2].trim());
            list.add(oh);
        }
        return list;
    }

    private String toHHMM(String t) {
        if (t == null) return "";
        t = t.trim();
        if (t.matches("^\\d{2}:\\d{2}:\\d{2}$")) return t.substring(0, 5);
        return t;
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Opening Hours");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
