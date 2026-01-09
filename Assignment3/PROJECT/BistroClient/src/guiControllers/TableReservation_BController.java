package guiControllers;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @FXML private BorderPane rootPane;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> cmbHour;
    @FXML private TextField txtGuests;
    @FXML private Label lblMessage;

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
        this.api = new ClientAPI(chatClient);
        this.chatClient.setResponseHandler(this);

        // ✨ הגבלת ה-DatePicker: חסימת עבר וחסימת תאריך שרחוק יותר מחודש
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
        } catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * פותר את השגיאה: Error resolving onAction='#onDateSelected'
     */
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
            
            // ✨ בדיקה נוספת ב-UI: האם הזמן הוא לפחות שעה מעכשיו
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

    /**
     * ✨ פותר את השגיאה: Error resolving onAction='#onBackToMenuClicked'
     */
    @FXML
    private void onBackToMenuClicked() {
        if (chatClient != null) chatClient.setResponseHandler(null);
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Menu_B.fxml"));
            Parent root = loader.load();
            
            Menu_BController menuController = loader.getController();
            menuController.setClient(user, chatClient);

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleResponse(ResponseDTO response) {

        Platform.runLater(() -> {

            // GET_OPENING_HOURS
            if (response.isSuccess() && response.getData() instanceof ArrayList) {
                this.cachedOpeningHours = (ArrayList<OpeningHouers>) response.getData();
                if (datePicker.getValue() != null) {
                    updateComboBoxForDate(datePicker.getValue(), cachedOpeningHours);
                }
                return;
            }

            // CREATE_RESERVATION – הצלחה
            if (response.isSuccess()) {

                if (response.getData() instanceof String) {
                    String confirmationCode = (String) response.getData();
                    showSuccessAlert(
                            "Reservation Confirmed",
                            "Your reservation was successfully created.\n\nConfirmation Code: "
                                    + confirmationCode
                    );
                } else {
                    showSuccessAlert("Reservation Confirmed", "Reservation created successfully.");
                }

                return;
            }

            // CREATE_RESERVATION – כישלון
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
            // ✨ סינון שעות: אם זה היום, הוסף רק שעות שהן לפחות שעה מעכשיו
            if (selectedDate.equals(LocalDate.now())) {
                if (startTime.isAfter(nowPlusOneHour)) {
                    cmbHour.getItems().add(startTime.toString());
                }
            } else {
                cmbHour.getItems().add(startTime.toString());
            }
            startTime = startTime.plusMinutes(30);
        }
    }

    private boolean validateInputs() {
        if (datePicker.getValue() == null) { showMessage("Please select a date.", "red"); return false; }
        if (cmbHour.getValue() == null) { showMessage("Please select an hour.", "red"); return false; }
        try {
            int g = Integer.parseInt(txtGuests.getText().trim());
            if (g <= 0) throw new Exception();
        } catch (Exception e) { showMessage("Invalid number of guests.", "red"); return false; }
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
    
    @FXML private void onJoinWaitingListClicked() { showMessage("Waiting list feature coming soon!", "blue"); }
    @Override public void handleConnectionError(Exception e) { Platform.runLater(() -> showMessage("Connection lost.", "red")); }
    @Override public void handleConnectionClosed() {}
}