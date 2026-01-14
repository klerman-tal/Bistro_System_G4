package guiControllers;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

import application.ChatClient;
import dto.ResponseDTO;
import entities.OpeningHouers;
import entities.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DateCell;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

public class TableReservation_BController implements ClientResponseHandler {

    private User user;
    private ChatClient chatClient;
    private ClientAPI api;
    private ArrayList<OpeningHouers> cachedOpeningHours;

    // ברירת מחדל – חזרה לתפריט
    private String sourceScreen = "/gui/Menu_B.fxml";

    @FXML private BorderPane rootPane;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> cmbHour;
    @FXML private TextField txtGuests;
    @FXML private Label lblMessage;

    public void setSourceScreen(String fxmlPath) {
        this.sourceScreen = fxmlPath;
    }

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
        this.api = new ClientAPI(chatClient);
        this.chatClient.setResponseHandler(this);

        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate today = LocalDate.now();
                LocalDate maxDate = today.plusMonths(1);
                if (date.isBefore(today) || date.isAfter(maxDate)) {
                    setDisable(true);
                    setStyle("-fx-background-color: #eeeeee;");
                }
            }
        });

        try {
            api.getOpeningHours();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onDateSelected() {
        if (datePicker.getValue() != null && cachedOpeningHours != null) {
            updateComboBoxForDate(datePicker.getValue(), cachedOpeningHours);
        }
    }

    @FXML
    private void onCreateClicked() {
        lblMessage.setVisible(false);
        if (!validateInputs()) return;

        try {
            LocalDate date = datePicker.getValue();
            LocalTime time = LocalTime.parse(cmbHour.getValue());

            if (date.equals(LocalDate.now()) && time.isBefore(LocalTime.now().plusHours(1))) {
                showMessage("Reservations must be at least 1 hour in advance.", "red");
                return;
            }

            api.createReservation(date, time, Integer.parseInt(txtGuests.getText().trim()), user);
            showMessage("Sending request...", "blue");

        } catch (Exception e) {
            showMessage("Error creating reservation.", "red");
        }
    }

    @FXML
    private void onBackClicked() {
        if (chatClient != null) chatClient.setResponseHandler(null);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(sourceScreen));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof ManageReservationController) {
                ((ManageReservationController) controller).setClient(user, chatClient);
            } else if (controller instanceof Menu_BController) {
                ((Menu_BController) controller).setClient(user, chatClient);
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {

            if (response == null) return;

            if (response.isSuccess() && response.getData() instanceof ArrayList) {
                @SuppressWarnings("unchecked")
                ArrayList<OpeningHouers> list = (ArrayList<OpeningHouers>) response.getData();
                this.cachedOpeningHours = list;

                if (datePicker.getValue() != null) {
                    updateComboBoxForDate(datePicker.getValue(), cachedOpeningHours);
                }
                return;
            }

            if (response.isSuccess()) {
                String msg = "Reservation Confirmed";
                if (response.getData() instanceof String) {
                    msg += "\nConfirmation Code: " + response.getData();
                }
                showSuccessAlert("Reservation Confirmed", msg);
                return;
            }

            if (response.getData() instanceof ArrayList) {
                @SuppressWarnings("unchecked")
                ArrayList<LocalTime> times = (ArrayList<LocalTime>) response.getData();
                ArrayList<String> suggested = new ArrayList<>();
                for (LocalTime t : times) if (t != null) suggested.add(t.toString());

                if (!suggested.isEmpty()) {
                    cmbHour.getItems().setAll(suggested);
                    cmbHour.getSelectionModel().selectFirst();
                    showMessage("No table available. Suggested times updated.", "red");
                    return;
                }
            }

            showMessage(response.getMessage(), "red");
        });
    }

    private void updateComboBoxForDate(LocalDate date, ArrayList<OpeningHouers> allHours) {
        if (date == null || allHours == null) return;
        cmbHour.getItems().clear();

        String dayName = date.getDayOfWeek().name();
        String formattedDay = dayName.substring(0, 1) + dayName.substring(1).toLowerCase();

        for (OpeningHouers oh : allHours) {
            if (oh.getDayOfWeek().equals(formattedDay)) {
                fillTimeSlots(oh.getOpenTime(), oh.getCloseTime(), date);
                break;
            }
        }
    }

    private void fillTimeSlots(String open, String close, LocalDate selectedDate) {
        if (open == null || close == null) return;

        LocalTime startTime = LocalTime.parse(open.substring(0, 5));
        LocalTime endTime = LocalTime.parse(close.substring(0, 5));
        LocalTime nowPlusOneHour = LocalTime.now().plusHours(1);

        while (!startTime.isAfter(endTime)) {
            if (!selectedDate.equals(LocalDate.now()) || startTime.isAfter(nowPlusOneHour)) {
                cmbHour.getItems().add(startTime.toString());
            }
            startTime = startTime.plusMinutes(30);
        }
    }

    private boolean validateInputs() {
        if (datePicker.getValue() == null) {
            showMessage("Please select a date.", "red");
            return false;
        }
        if (cmbHour.getValue() == null) {
            showMessage("Please select an hour.", "red");
            return false;
        }
        try {
            int g = Integer.parseInt(txtGuests.getText().trim());
            if (g <= 0) throw new Exception();
        } catch (Exception e) {
            showMessage("Invalid number of guests.", "red");
            return false;
        }
        return true;
    }

    private void showMessage(String msg, String color) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: " + color + ";");
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    private void showSuccessAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @Override public void handleConnectionError(Exception e) {
        Platform.runLater(() -> showMessage("Connection lost.", "red"));
    }

    @Override public void handleConnectionClosed() {}
}
