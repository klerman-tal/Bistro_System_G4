package guiControllers;

import application.ChatClient;
import dto.RequestDTO;
import dto.ResponseDTO;
import dto.UpdateOpeningHoursDTO;
import entities.OpeningHouers;
import entities.Restaurant;
import entities.User;
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
import java.util.ArrayList;

public class OpeningHoursController implements ClientResponseHandler {

    @FXML private TableView<OpeningHouers> openingHoursTable;
    @FXML private TableColumn<OpeningHouers, String> dayColumn;
    @FXML private TableColumn<OpeningHouers, String> openTimeColumn;
    @FXML private TableColumn<OpeningHouers, String> closeTimeColumn;

    @FXML private TextField openTimeField;
    @FXML private Button updateButton;
    @FXML private Button backButton;

    private User user;
    private ChatClient chatClient;
    private ClientAPI api;

    private enum EditTarget { OPEN_TIME, CLOSE_TIME }
    private EditTarget editTarget = EditTarget.OPEN_TIME;

    /**
     * נקרא מהמסך הקודם אחרי טעינת ה-FXML.
     */
    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
        this.api = new ClientAPI(chatClient);

        // מקשרים את ה-controller הזה לקבלת ResponseDTO
        this.chatClient.setResponseHandler(this);

        try {
            api.getOpeningHours();
        } catch (IOException e) {
            showAlert("Failed to load opening hours.");
        }
    }

    @FXML
    public void initialize() {

        dayColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDayOfWeek()));

        openTimeColumn.setCellValueFactory(c -> {
            String v = toHHMM(c.getValue().getOpenTime());
            return new SimpleStringProperty(v.isBlank() ? "CLOSED" : v);
        });

        closeTimeColumn.setCellValueFactory(c -> {
            String v = toHHMM(c.getValue().getCloseTime());
            return new SimpleStringProperty(v.isBlank() ? "CLOSED" : v);
        });

        openingHoursTable.addEventFilter(MouseEvent.MOUSE_CLICKED,
                e -> detectClickedColumn());

        openingHoursTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected == null) return;

            openTimeField.setText(
                    editTarget == EditTarget.OPEN_TIME
                            ? toHHMM(selected.getOpenTime())
                            : toHHMM(selected.getCloseTime())
            );
        });

        updateButton.setOnAction(e -> onUpdateClicked());
        backButton.setOnAction(e -> onBackClicked());
    }

    // =====================
    // Button handlers
    // =====================

    private void onUpdateClicked() {

        if (chatClient == null) {
            showAlert("ChatClient not set (setClient was not called).");
            return;
        }

        OpeningHouers selected = openingHoursTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select a day from the table.");
            return;
        }

        String newTime = openTimeField.getText();
        newTime = (newTime == null) ? "" : newTime.trim();

        String open = selected.getOpenTime();
        String close = selected.getCloseTime();

        // אם ריק -> סגור יום (שולחים null כדי שייכנס NULL ב-DB)
        if (newTime.isBlank()) {
            open = null;
            close = null;
        } else {
            if (!newTime.matches("^\\d{2}:\\d{2}$")) {
                showAlert("Invalid time format. Use HH:MM (e.g., 09:30) or leave empty for CLOSED.");
                return;
            }

            if (editTarget == EditTarget.OPEN_TIME) open = newTime;
            else close = newTime;
        }

        UpdateOpeningHoursDTO dto =
                new UpdateOpeningHoursDTO(selected.getDayOfWeek(), open, close);

        try {
            RequestDTO request = new RequestDTO(Commands.UPDATE_OPENING_HOURS, dto);
            chatClient.sendToServer(request);
        } catch (IOException e) {
            showAlert("Failed to send update to server.");
        }
    }

    private void onBackClicked() {

        if (chatClient != null) chatClient.setResponseHandler(null);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/RestaurantManagement_B.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof RestaurantManagement_BController) {
                ((RestaurantManagement_BController) controller).setClient(user, chatClient); // ✅ זה התיקון
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
    public void handleResponse(ResponseDTO response) {

        Platform.runLater(() -> {

            if (response == null) return;

            if (!response.isSuccess()) {
                showAlert(response.getMessage());
                return;
            }

            // GET_OPENING_HOURS מחזיר ArrayList<OpeningHouers>
            if (response.getData() instanceof ArrayList) {
                @SuppressWarnings("unchecked")
                ArrayList<OpeningHouers> list = (ArrayList<OpeningHouers>) response.getData();

                Restaurant.getInstance().setOpeningHours(list);
                openingHoursTable.getItems().setAll(list);
                return;
            }

            // UPDATE_OPENING_HOURS הצליח -> רענון
            try {
                api.getOpeningHours();
            } catch (IOException e) {
                showAlert("Updated, but failed to refresh opening hours.");
            }
        });
    }

    @Override
    public void handleConnectionError(Exception e) {
        Platform.runLater(() -> showAlert("Connection lost."));
    }

    @Override
    public void handleConnectionClosed() {}

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

    private String toHHMM(String t) {
        if (t == null) return "";
        t = t.trim();
        if (t.length() >= 5) return t.substring(0, 5);
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