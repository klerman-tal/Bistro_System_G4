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
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

public class TableReservation_BController implements ClientResponseHandler {

    private User user;
    private ChatClient chatClient;
    private ClientAPI api;
    private ArrayList<OpeningHouers> cachedOpeningHours;
    private String backFxml;

    @FXML private BorderPane rootPane;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> cmbHour;
    @FXML private TextField txtGuests;
    @FXML private Label lblMessage;

    /* ================= CONTEXT ================= */

    public void setBackFxml(String backFxml) {
        this.backFxml = backFxml;
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
            api.getOpeningHours(); // נשאר – אולי את משתמשת בזה בעוד מקום
        } catch (IOException e) {
            showMessage("Failed to load opening hours", "red");
        }
    }

    /* ================= ACTIONS ================= */

    @FXML
    private void onDateSelected() {
        hideMessage();
        cmbHour.getItems().clear();
        cmbHour.setValue(null);

        LocalDate selected = datePicker.getValue();
        if (selected == null) return;

        int guests = parseGuestsOrDefault(1);

        try {
            // ✅ העיקר: להביא רק שעות שיש בהן שולחן פנוי ל-2 שעות בגריד
            api.getAvailableTimesForDate(selected, guests);
        } catch (IOException e) {
            showMessage("Failed to load available times", "red");
        }
    }

    @FXML
    private void onCreateClicked() {
        hideMessage();

        if (!validateInputs()) return;

        try {
            LocalDate date = datePicker.getValue();
            LocalTime time = LocalTime.parse(cmbHour.getValue());

            if (date.equals(LocalDate.now())
                    && time.isBefore(LocalTime.now().plusHours(1))) {
                showMessage("Reservations must be at least 1 hour in advance.", "red");
                return;
            }

            api.createReservation(
                    date,
                    time,
                    Integer.parseInt(txtGuests.getText().trim()),
                    user
            );

            showMessage("Sending request...", "blue");

        } catch (Exception e) {
            showMessage("Error creating reservation.", "red");
        }
    }

    /* ================= BACK ================= */

    @FXML
    private void onBackClicked() {
        if (backFxml == null) return;

        if (chatClient != null) {
            chatClient.setResponseHandler(null);
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(backFxml));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller != null && user != null && chatClient != null) {
                try {
                    controller.getClass()
                            .getMethod("setClient", User.class, ChatClient.class)
                            .invoke(
                            	    controller,
                            	    application.ClientSession.getLoggedInUser(),
                            	    chatClient
                            	);

                } catch (Exception ignored) {}
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================= SERVER RESPONSE ================= */

    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {

            if (response == null) return;

            Object data = response.getData();

            // ✅ 1) OpeningHours list
            if (response.isSuccess() && data instanceof ArrayList<?> list && isOpeningHoursList(list)) {
                @SuppressWarnings("unchecked")
                ArrayList<OpeningHouers> oh = (ArrayList<OpeningHouers>) data;
                cachedOpeningHours = oh;
                return;
            }

            // ✅ 2) Available times list (success)
            if (response.isSuccess() && data instanceof ArrayList<?> list && isLocalTimeList(list)) {
                @SuppressWarnings("unchecked")
                ArrayList<LocalTime> times = (ArrayList<LocalTime>) data;

                updateHoursComboFromLocalTimes(times);

                if (times.isEmpty()) {
                    showMessage("אין מקום פנוי שעומד בדרישות ליום הנבחר", "red");
                } else {
                    hideMessage();
                }
                return;
            }

            // ✅ 3) Reservation created
            if (response.isSuccess()) {
                String msg = "Reservation Confirmed";
                if (response.getData() instanceof String) {
                    msg += "\nConfirmation Code: " + response.getData();
                }
                showSuccessAlert("Reservation Confirmed", msg);
                return;
            }

            // ✅ 4) Reservation failed but got suggested times (LocalTime list)
            if (!response.isSuccess() && data instanceof ArrayList<?> list && isLocalTimeList(list)) {
                @SuppressWarnings("unchecked")
                ArrayList<LocalTime> times = (ArrayList<LocalTime>) data;

                updateHoursComboFromLocalTimes(times);

                if (times.isEmpty()) {
                    showMessage("No available tables match the selected day requirements.", "red");
                } else {
                    showMessage("The available time slots have been updated based on your selection.", "blue");
                }
                return;
            }

            // fallback
            showMessage(response.getMessage(), "red");
        });
    }

    private void updateHoursComboFromLocalTimes(ArrayList<LocalTime> times) {
        cmbHour.getItems().clear();
        cmbHour.setValue(null);

        if (times == null) return;

        for (LocalTime t : times) {
            cmbHour.getItems().add(t.toString());
        }
    }

    private boolean isOpeningHoursList(ArrayList<?> list) {
        if (list == null || list.isEmpty()) return false;
        return list.get(0) instanceof OpeningHouers;
    }

    private boolean isLocalTimeList(ArrayList<?> list) {
        if (list == null) return true;
        if (list.isEmpty()) return true;
        return list.get(0) instanceof LocalTime;
    }

    /* ================= HELPERS ================= */

    private int parseGuestsOrDefault(int def) {
        try {
            String s = txtGuests.getText();
            if (s == null || s.isBlank()) return def;
            int g = Integer.parseInt(s.trim());
            return (g > 0) ? g : def;
        } catch (Exception e) {
            return def;
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
        lblMessage.setStyle("-fx-text-fill: " + color);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    private void hideMessage() {
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
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
