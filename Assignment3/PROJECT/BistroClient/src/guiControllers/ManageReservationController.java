package guiControllers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import application.ChatClient;
import dto.ResponseDTO;
import entities.Reservation;
import entities.User;
import interfaces.ClientActions;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

public class ManageReservationController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private Label lblStatus;

    // ===== TABLE =====
    @FXML private TableView<Reservation> tblReservations;

    @FXML private TableColumn<Reservation, Integer> colReservationId;
    @FXML private TableColumn<Reservation, LocalDateTime> colDateTime;
    @FXML private TableColumn<Reservation, Integer> colGuests;
    @FXML private TableColumn<Reservation, String> colCode;
    @FXML private TableColumn<Reservation, Integer> colCreatedBy;
    @FXML private TableColumn<Reservation, String> colStatus;
    @FXML private TableColumn<Reservation, Integer> colTableNumber;

    // ===== FORM =====
    @FXML private TextField txtReservationId;
    @FXML private TextField txtConfirmationCode;
    @FXML private DatePicker dpDate;
    @FXML private TextField txtTime;
    @FXML private TextField txtGuests;
    @FXML private TextField txtCreatedBy;
    @FXML private ComboBox<String> cmbReservationStatus;
    @FXML private TextField txtTableNumber;

    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;
    @FXML private Button btnBack;

    // ===== SESSION =====
    private ClientActions clientActions;
    private User user;
    private ChatClient chatClient;
    private ClientAPI clientAPI;

    private final ObservableList<Reservation> reservationsList =
            FXCollections.observableArrayList();

    /* =======================
       SESSION SETUP
       ======================= */

    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;

        if (chatClient != null) {
            this.clientAPI = new ClientAPI(chatClient);

            chatClient.setResponseHandler(new ClientResponseHandler() {
                @Override
                public void handleResponse(ResponseDTO response) {
                    Platform.runLater(() -> handleServerResponse(response));
                }

                @Override
                public void handleConnectionError(Exception exception) {
                    Platform.runLater(() ->
                            showMessage("Connection error: " + exception.getMessage()));
                }

                @Override
                public void handleConnectionClosed() {
                }
            });
        }

        initializeTableBehavior();
        loadReservationsOnEnter();
    }

    /* =======================
       INITIALIZE
       ======================= */

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        cmbReservationStatus.getItems().addAll(
                "ACTIVE",
                "CANCELLED",
                "FINISHED"
        );
    }

    private void initializeTableBehavior() {

        colReservationId.setCellValueFactory(
                new PropertyValueFactory<>("reservationId"));

        colDateTime.setCellValueFactory(
                new PropertyValueFactory<>("reservationTime"));

        colGuests.setCellValueFactory(
                new PropertyValueFactory<>("guestAmount"));

        colCode.setCellValueFactory(
                new PropertyValueFactory<>("confirmationCode"));

        colCreatedBy.setCellValueFactory(
                new PropertyValueFactory<>("createdByUserId"));

        colStatus.setCellValueFactory(
                new PropertyValueFactory<>("reservationStatus"));

        colTableNumber.setCellValueFactory(
                new PropertyValueFactory<>("tableNumber"));

        tblReservations.setItems(reservationsList);

        tblReservations.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldVal, selected) -> {
                    if (selected != null) {
                        fillForm(selected);
                    }
                });
    }

    /* =======================
       DATA LOADING
       ======================= */

    private void loadReservationsOnEnter() {
        hideMessage();

        if (clientAPI == null) {
            showMessage("Session missing: please re-enter screen.");
            return;
        }

        try {
            clientAPI.getAllReservations();
        } catch (IOException e) {
            showMessage("Failed to load reservations");
            e.printStackTrace();
        }
    }

    /* =======================
       SERVER RESPONSE
       ======================= */

    public void handleServerResponse(ResponseDTO response) {
        if (response == null) return;

        if (response.isSuccess() && response.getData() instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<Reservation> list = (List<Reservation>) response.getData();
            reservationsList.setAll(list);
            hideMessage();
            return;
        }

        if (!response.isSuccess()) {
            showMessage(response.getMessage());
        }
    }

    /* =======================
       FORM
       ======================= */

    private void fillForm(Reservation r) {
        txtReservationId.setText(String.valueOf(r.getReservationId()));
        txtConfirmationCode.setText(r.getConfirmationCode());

        if (r.getReservationTime() != null) {
            dpDate.setValue(r.getReservationTime().toLocalDate());
            txtTime.setText(r.getReservationTime().toLocalTime().toString());
        } else {
            dpDate.setValue(null);
            txtTime.clear();
        }

        txtGuests.setText(String.valueOf(r.getGuestAmount()));
        txtCreatedBy.setText(String.valueOf(r.getCreatedByUserId()));

        cmbReservationStatus.setValue(
                r.getReservationStatus() != null
                        ? r.getReservationStatus().name()
                        : null
        );

        txtTableNumber.setText(
                r.getTableNumber() != null
                        ? String.valueOf(r.getTableNumber())
                        : ""
        );
    }

    /* =======================
       BUTTONS
       ======================= */

    @FXML
    private void onAddClicked() {
        showMessage("Add not wired yet");
    }

    @FXML
    private void onUpdateClicked() {
        showMessage("Update not wired yet");
    }

    @FXML
    private void onDeleteClicked() {
        // משיכת השורה שנבחרה מהטבלה
        Reservation selected = tblReservations.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showMessage("Please select a reservation from the table to cancel.");
            return;
        }

        // פתיחת מסך הביטול עם הנתונים
        openCancelWindow(selected);
    }

    private void openCancelWindow(Reservation reservation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/CancelReservation_B.fxml"));
            Parent root = loader.load();

            CancelReservation_BController controller = loader.getController();
            
            // העברת המשתמש והקליינט כדי לשמור על החיבור
            controller.setClient(user, chatClient);
            // העברת קוד האישור מהטבלה למסך הביטול
            controller.setConfirmationCode(reservation.getConfirmationCode());

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Error opening cancel screen");
        }
    }

    @FXML
    private void onBackClicked() {
        openWindow("RestaurantManagement_B.fxml", "Restaurant Management");
    }

    /* =======================
       NAVIGATION
       ======================= */

    private void openWindow(String fxmlName, String title) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
            Parent root = loader.load();

            Object controller = loader.getController();

            if (controller != null && clientActions != null) {
                controller.getClass()
                        .getMethod("setClientActions", ClientActions.class)
                        .invoke(controller, clientActions);
            }

            if (controller != null) {
                controller.getClass()
                        .getMethod("setClient", User.class, ChatClient.class)
                        .invoke(controller, user, chatClient);
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle("Bistro - " + title);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Failed to open screen");
        }
    }

    private void showMessage(String msg) {
        lblStatus.setText(msg);
        lblStatus.setVisible(true);
        lblStatus.setManaged(true);
    }

    private void hideMessage() {
        lblStatus.setText("");
        lblStatus.setVisible(false);
        lblStatus.setManaged(false);
    }
}