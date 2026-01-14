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
            api.getOpeningHours();
        } catch (IOException e) {
            showMessage("Failed to load opening hours", "red");
        }
    }

    /* ================= ACTIONS ================= */

    @FXML
    private void onDateSelected() {
        if (datePicker.getValue() != null && cachedOpeningHours != null) {
            updateComboBoxForDate(datePicker.getValue(), cachedOpeningHours);
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
                            .invoke(controller, user, chatClient);
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

            if (response.isSuccess() && response.getData() instanceof ArrayList) {
                @SuppressWarnings("unchecked")
                ArrayList<OpeningHouers> list =
                        (ArrayList<OpeningHouers>) response.getData();
                cachedOpeningHours = list;

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

            showMessage(response.getMessage(), "red");
        });
    }

    /* ================= HELPERS ================= */

    private void updateComboBoxForDate(LocalDate date, ArrayList<OpeningHouers> allHours) {
        cmbHour.getItems().clear();
        String day = date.getDayOfWeek().name();
        String formattedDay = day.substring(0,1) + day.substring(1).toLowerCase();

        for (OpeningHouers oh : allHours) {
            if (oh.getDayOfWeek().equals(formattedDay)) {
                fillTimeSlots(oh.getOpenTime(), oh.getCloseTime(), date);
                break;
            }
        }
    }

    private void fillTimeSlots(String open, String close, LocalDate selectedDate) {
        LocalTime start = LocalTime.parse(open.substring(0,5));
        LocalTime end = LocalTime.parse(close.substring(0,5));
        LocalTime nowPlusHour = LocalTime.now().plusHours(1);

        while (!start.isAfter(end)) {
            if (!selectedDate.equals(LocalDate.now())
                    || start.isAfter(nowPlusHour)) {
                cmbHour.getItems().add(start.toString());
            }
            start = start.plusMinutes(30);
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
