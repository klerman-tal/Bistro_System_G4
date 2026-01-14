package guiControllers;

import application.ChatClient;
import dto.RequestDTO;
import dto.ResponseDTO;
import dto.UpdateOpeningHoursDTO;
import entities.OpeningHouers;
import entities.Restaurant;
import entities.User;
import entities.SpecialOpeningHours; // ✨ וודא שיצרת את ה-Entity הזה
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;
import protocol.Commands;

import java.io.IOException;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;

public class OpeningHoursController implements ClientResponseHandler {

    @FXML private TableView<OpeningHouers> openingHoursTable;
    @FXML private TableColumn<OpeningHouers, String> dayColumn;
    @FXML private TableColumn<OpeningHouers, String> openTimeColumn;
    @FXML private TableColumn<OpeningHouers, String> closeTimeColumn;

    @FXML private TextField openTimeField;
    @FXML private DatePicker specialDatePicker; // ✨ שדה חדש
    @FXML private Button updateButton;
    @FXML private Button backButton;

    private User user;
    private ChatClient chatClient;
    private ClientAPI api;

    private enum EditTarget { OPEN_TIME, CLOSE_TIME }
    private EditTarget editTarget = EditTarget.OPEN_TIME;

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
        this.api = new ClientAPI(chatClient);
        this.chatClient.setResponseHandler(this);

        try {
            api.getOpeningHours();
        } catch (IOException e) {
            showAlert("Failed to load opening hours.");
        }
    }

    @FXML
    public void initialize() {
        dayColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDayOfWeek()));
        openTimeColumn.setCellValueFactory(c -> {
            String v = toHHMM(c.getValue().getOpenTime());
            return new SimpleStringProperty(v.isBlank() ? "CLOSED" : v);
        });
        closeTimeColumn.setCellValueFactory(c -> {
            String v = toHHMM(c.getValue().getCloseTime());
            return new SimpleStringProperty(v.isBlank() ? "CLOSED" : v);
        });

        openingHoursTable.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> detectClickedColumn());

        updateButton.setOnAction(e -> onUpdateClicked());
        backButton.setOnAction(e -> onBackClicked());
    }

    private void onUpdateClicked() {
        if (chatClient == null) return;

        String newTimeStr = openTimeField.getText();
        newTimeStr = (newTimeStr == null) ? "" : newTimeStr.trim();
        LocalDate specialDate = specialDatePicker.getValue();

        // בדיקה: האם זה עדכון לתאריך ספציפי או ליום בשבוע?
        if (specialDate != null) {
            handleSpecialDateUpdate(specialDate, newTimeStr);
        } else {
            handleStandardUpdate(newTimeStr);
        }
    }

    /**
     * עדכון החרגה לתאריך ספציפי
     */
    private void handleSpecialDateUpdate(LocalDate date, String timeStr) {
        if (!timeStr.matches("^\\d{2}:\\d{2}$") && !timeStr.isBlank()) {
            showAlert("Invalid time format. Use HH:MM or leave empty for CLOSED.");
            return;
        }

        // במקרה של תאריך ספציפי, אנחנו מניחים שהעדכון הוא לכל היום (או פתוח או סגור)
        // אפשר לשדרג את זה שיוכל לעדכן פתיחה וסגירה בנפרד, כרגע נשלח את זה כעדכון פשוט
        try {
            // כאן אתה שולח פקודה חדשה לשרת שצריך לממש
            // Commands.UPDATE_SPECIAL_OPENING_HOURS
            Time open = timeStr.isBlank() ? null : Time.valueOf(timeStr + ":00");
            Time close = timeStr.isBlank() ? null : Time.valueOf("23:59:00"); // ברירת מחדל לסגירת יום
            
            SpecialOpeningHours special = new SpecialOpeningHours(date, open, close, timeStr.isBlank());
            RequestDTO request = new RequestDTO(Commands.UPDATE_SPECIAL_OPENING_HOURS, special);            chatClient.sendToServer(request);
            showAlert("Special hours for " + date + " sent to server!");
            specialDatePicker.setValue(null); // איפוס
        } catch (Exception e) {
            showAlert("Error preparing special update.");
        }
    }

    /**
     * עדכון רגיל לפי הטבלה (מה שהיה קודם)
     */
    private void handleStandardUpdate(String newTime) {
        OpeningHouers selected = openingHoursTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select a day from the table or choose a Special Date.");
            return;
        }

        String open = selected.getOpenTime();
        String close = selected.getCloseTime();

        if (newTime.isBlank()) {
            open = null; close = null;
        } else {
            if (!newTime.matches("^\\d{2}:\\d{2}$")) {
                showAlert("Invalid format HH:MM");
                return;
            }
            if (editTarget == EditTarget.OPEN_TIME) open = newTime;
            else close = newTime;
        }

        UpdateOpeningHoursDTO dto = new UpdateOpeningHoursDTO(selected.getDayOfWeek(), open, close);
        try {
            chatClient.sendToServer(new RequestDTO(Commands.UPDATE_OPENING_HOURS, dto));
        } catch (IOException e) {
            showAlert("Failed to send update.");
        }
    }

    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {
            if (response == null || !response.isSuccess()) {
                showAlert(response != null ? response.getMessage() : "Server error");
                return;
            }

            if (response.getData() instanceof ArrayList) {
                ArrayList<OpeningHouers> list = (ArrayList<OpeningHouers>) response.getData();
                openingHoursTable.getItems().setAll(list);
            } else {
                // רענון אחרי עדכון
                try { api.getOpeningHours(); } catch (IOException ignored) {}
            }
        });
    }

    // שאר המתודות (detectClickedColumn, toHHMM, showAlert, וכו') נשארות ללא שינוי
    private void detectClickedColumn() {
        if (openingHoursTable.getSelectionModel().getSelectedCells().isEmpty()) return;
        TablePosition<OpeningHouers, ?> pos = openingHoursTable.getSelectionModel().getSelectedCells().get(0);
        TableColumn<?, ?> col = pos.getTableColumn();
        if (col == openTimeColumn) editTarget = EditTarget.OPEN_TIME;
        else if (col == closeTimeColumn) editTarget = EditTarget.CLOSE_TIME;
    }

    private String toHHMM(String t) {
        if (t == null) return "";
        t = t.trim();
        return t.length() >= 5 ? t.substring(0, 5) : t;
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Opening Hours");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void onBackClicked() {
        if (chatClient != null) chatClient.setResponseHandler(null);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/RestaurantManagement_B.fxml"));
            Parent root = loader.load();
            RestaurantManagement_BController ctrl = loader.getController();
            ctrl.setClient(user, chatClient);
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

	@Override
	public void handleConnectionError(Exception e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleConnectionClosed() {
		// TODO Auto-generated method stub
		
	}
}