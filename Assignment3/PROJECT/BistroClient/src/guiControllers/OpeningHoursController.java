package guiControllers;

import application.ChatClient;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.OpeningHouers;
import entities.Restaurant;
import entities.SpecialOpeningHours;
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
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;

public class OpeningHoursController implements ClientResponseHandler {

    @FXML private TableView<OpeningHouers> openingHoursTable;
    @FXML private TableColumn<OpeningHouers, String> dayColumn;
    @FXML private TableColumn<OpeningHouers, String> openTimeColumn;
    @FXML private TableColumn<OpeningHouers, String> closeTimeColumn;

    @FXML private TextField openTimeField;
    @FXML private Button updateButton;
    @FXML private Button backButton;

    // Special opening hours
    @FXML private TableView<SpecialOpeningHours> specialHoursTable;
    @FXML private TableColumn<SpecialOpeningHours, String> specialDateColumn;
    @FXML private TableColumn<SpecialOpeningHours, String> specialOpenColumn;
    @FXML private TableColumn<SpecialOpeningHours, String> specialCloseColumn;

    @FXML private DatePicker specialDatePicker;
    @FXML private CheckBox specialClosedCheck;
    @FXML private TextField specialOpenField;
    @FXML private TextField specialCloseField;
    @FXML private Button addSpecialButton;

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
            api.getSpecialOpeningHours();
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

        // Special table
        specialDateColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getSpecialDate().toString()));

        specialOpenColumn.setCellValueFactory(c -> {
            if (c.getValue().isClosed()) return new SimpleStringProperty("CLOSED");
            Time t = c.getValue().getOpenTime();
            return new SimpleStringProperty(t == null ? "" : t.toString().substring(0, 5));
        });

        specialCloseColumn.setCellValueFactory(c -> {
            if (c.getValue().isClosed()) return new SimpleStringProperty("CLOSED");
            Time t = c.getValue().getCloseTime();
            return new SimpleStringProperty(t == null ? "" : t.toString().substring(0, 5));
        });

        specialClosedCheck.selectedProperty().addListener((obs, o, isClosed) -> {
            specialOpenField.setDisable(isClosed);
            specialCloseField.setDisable(isClosed);
            if (isClosed) {
                specialOpenField.clear();
                specialCloseField.clear();
            }
        });

        addSpecialButton.setOnAction(e -> onSaveSpecialClicked());
    }

    private void onUpdateClicked() {

        OpeningHouers selected = openingHoursTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select a day.");
            return;
        }

        String newTime = openTimeField.getText().trim();

        String open = selected.getOpenTime();
        String close = selected.getCloseTime();

        if (newTime.isBlank()) {
            open = null;
            close = null;
        } else {
            if (!newTime.matches("^\\d{2}:\\d{2}$")) {
                showAlert("Invalid time format (HH:MM).");
                return;
            }
            if (editTarget == EditTarget.OPEN_TIME) open = newTime;
            else close = newTime;
        }

        try {
            chatClient.sendToServer(
                    new RequestDTO(Commands.UPDATE_OPENING_HOURS,
                            new dto.UpdateOpeningHoursDTO(
                                    selected.getDayOfWeek(), open, close)));
        } catch (IOException e) {
            showAlert("Failed to update opening hours.");
        }
    }

    private void onSaveSpecialClicked() {
        LocalDate date = specialDatePicker.getValue();
        if (date == null) {
            showAlert("Please choose a date.");
            return;
        }

        boolean closed = specialClosedCheck.isSelected();
        Time open = null, close = null;

        if (!closed) {
            open = Time.valueOf(specialOpenField.getText() + ":00");
            close = Time.valueOf(specialCloseField.getText() + ":00");
        }

        try {
            api.updateSpecialOpeningHours(date, open, close, closed);
        } catch (Exception e) {
            showAlert("Failed to save special hours.");
        }
    }

    @Override
    public void handleResponse(ResponseDTO response) {

        if (response == null || !response.isSuccess()) {
            showAlert(response != null ? response.getMessage() : "Unknown error");
            return;
        }

        if (response.getData() instanceof ArrayList<?> list && !list.isEmpty()) {

            if (list.get(0) instanceof OpeningHouers) {
                openingHoursTable.getItems().setAll((ArrayList<OpeningHouers>) list);
                Restaurant.getInstance().setOpeningHours((ArrayList<OpeningHouers>) list);
            }

            if (list.get(0) instanceof SpecialOpeningHours) {
                specialHoursTable.getItems().setAll((ArrayList<SpecialOpeningHours>) list);
            }
        }
    }

    private void onBackClicked() {
        chatClient.setResponseHandler(null);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/RestaurantManagement_B.fxml"));
            Parent root = loader.load();
            ((RestaurantManagement_BController) loader.getController()).setClient(user, chatClient);
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            showAlert("Failed to go back.");
        }
    }

    private void detectClickedColumn() {
        if (openingHoursTable.getSelectionModel().getSelectedCells().isEmpty()) return;
        TablePosition<?, ?> pos = openingHoursTable.getSelectionModel().getSelectedCells().get(0);
        editTarget = pos.getTableColumn() == openTimeColumn
                ? EditTarget.OPEN_TIME
                : EditTarget.CLOSE_TIME;
    }

    private String toHHMM(String t) {
        return (t == null || t.length() < 5) ? "" : t.substring(0, 5);
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Opening Hours");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
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
