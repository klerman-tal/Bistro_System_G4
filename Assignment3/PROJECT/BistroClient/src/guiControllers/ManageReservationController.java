package guiControllers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import application.ChatClient;
import dto.ResponseDTO;
import entities.Reservation;
import entities.User;
import entities.Enums.ReservationStatus;
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
    @FXML private TableColumn<Reservation, ReservationStatus> colStatus;
    @FXML private TableColumn<Reservation, Integer> colTableNumber;

    @FXML private Button btnAdd;
    @FXML private Button btnDelete;
    @FXML private Button btnBack;

    // ===== SESSION =====
    private ClientActions clientActions;
    private User user;
    private ChatClient chatClient;
    private ClientAPI clientAPI;

    private final ObservableList<Reservation> reservationsList =
            FXCollections.observableArrayList();

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
                public void handleConnectionClosed() {}
            });
        }

        initializeTableBehavior();
        loadReservationsOnEnter();
    }

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        // המתודה נשארת ריקה כי מחקנו את ה-ComboBox שגרם לשגיאה
    }

    private void initializeTableBehavior() {
        colReservationId.setCellValueFactory(new PropertyValueFactory<>("reservationId"));
        colDateTime.setCellValueFactory(new PropertyValueFactory<>("reservationTime"));
        colGuests.setCellValueFactory(new PropertyValueFactory<>("guestAmount"));
        colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        colCreatedBy.setCellValueFactory(new PropertyValueFactory<>("createdByUserId"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("reservationStatus"));
        colTableNumber.setCellValueFactory(new PropertyValueFactory<>("tableNumber"));

        tblReservations.setItems(reservationsList);
        
        // הסרנו את ה-Listener שקרא ל-fillForm כי השדות נמחקו
    }

    private void loadReservationsOnEnter() {
        hideMessage();
        if (clientAPI == null) return;

        try {
            clientAPI.getAllReservations();
        } catch (IOException e) {
            showMessage("Failed to load reservations");
        }
    }

    public void handleServerResponse(ResponseDTO response) {
        if (response == null) return;

        if (response.isSuccess() && response.getData() instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<Reservation> list = (List<Reservation>) response.getData();
            reservationsList.setAll(list);
            hideMessage();
        }
        else if (response.isSuccess()) {
            showMessage("Success: " + response.getMessage());
            loadReservationsOnEnter();
        }
        else {
            showMessage("Error: " + response.getMessage());
        }
    }

    @FXML
    private void onAddClicked() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/TableReservation_B.fxml"));
            Parent root = loader.load();

            TableReservation_BController controller = loader.getController();
            if (controller != null) {
                controller.setClient(user, chatClient);
                controller.setBackFxml("/gui/ManageReservation.fxml");
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            showMessage("Error opening Add screen");
        }
    }

    @FXML
    private void onDeleteClicked() {
        Reservation selected = tblReservations.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Please select a reservation to cancel.");
            return;
        }
        openCancelWindow(selected);
    }

    private void openCancelWindow(Reservation reservation) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/CancelReservation_B.fxml"));
            Parent root = loader.load();

            CancelReservation_BController controller = loader.getController();
            controller.setClient(user, chatClient);
            controller.setConfirmationCode(reservation.getConfirmationCode());
            controller.setBackFxml("/gui/ManageReservation.fxml");

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            showMessage("Error opening cancel screen");
        }
    }

    @FXML
    private void onBackClicked() {
        openWindow("RestaurantManagement_B.fxml", "Management");
    }

    private void openWindow(String fxmlName, String title) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller != null) {
                try {
                    controller.getClass()
                            .getMethod("setClient", User.class, ChatClient.class)
                            .invoke(controller, user, chatClient);
                } catch (Exception ignored) {}
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle("Bistro - " + title);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showMessage(String msg) {
        lblStatus.setText(msg);
        lblStatus.setVisible(true);
        lblStatus.setManaged(true);
    }

    private void hideMessage() {
        lblStatus.setVisible(false);
        lblStatus.setManaged(false);
    }
}