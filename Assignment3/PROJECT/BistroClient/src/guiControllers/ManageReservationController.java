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
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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

/**
 * JavaFX controller for managing reservations (manager/agent view).
 *
 * <p>This screen displays all reservations in a table, supports refreshing the data from the server,
 * and provides actions to create a new reservation or cancel an existing one. Navigation between screens
 * is performed by swapping the current scene root to preserve the window instance and maximized state.</p>
 *
 * <p>The controller uses {@link ClientAPI} to send requests to the server and attaches a
 * {@link ClientResponseHandler} to process asynchronous responses.</p>
 */
public class ManageReservationController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private Label lblStatus;

    // ===== FILTER =====
    @FXML private TextField txtFilter;
    @FXML private ComboBox<String> cmbMatchMode;

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

    private ClientActions clientActions;
    private User user;
    private ChatClient chatClient;
    private ClientAPI clientAPI;

    private final ObservableList<Reservation> reservationsList =
            FXCollections.observableArrayList();

    private FilteredList<Reservation> filteredReservations;

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
        // intentionally empty
    }

    private void initializeTableBehavior() {
        colReservationId.setCellValueFactory(new PropertyValueFactory<>("reservationId"));
        colDateTime.setCellValueFactory(new PropertyValueFactory<>("reservationTime"));
        colGuests.setCellValueFactory(new PropertyValueFactory<>("guestAmount"));
        colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        colCreatedBy.setCellValueFactory(new PropertyValueFactory<>("createdByUserId"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("reservationStatus"));
        colTableNumber.setCellValueFactory(new PropertyValueFactory<>("tableNumber"));

        // ===== STATUS COLORING (NEW) =====
        installStatusColoring();

        // ===== FILTER + SORT =====
        filteredReservations = new FilteredList<>(reservationsList, r -> true);

        SortedList<Reservation> sorted = new SortedList<>(filteredReservations);
        sorted.comparatorProperty().bind(tblReservations.comparatorProperty());

        tblReservations.setItems(sorted);

        // ===== MATCH MODE (Exact default) =====
        if (cmbMatchMode != null) {
            cmbMatchMode.getItems().setAll("Contains", "Exact");
            cmbMatchMode.getSelectionModel().select("Exact");
        }

        // ===== LISTENERS =====
        if (txtFilter != null) {
            txtFilter.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        }
        if (cmbMatchMode != null) {
            cmbMatchMode.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        }
    }

    /**
     * Colors only the text in the Status column:
     * Active -> green, Cancelled -> red, Finished -> default.
     */
    private void installStatusColoring() {
        colStatus.setCellFactory(col -> new TableCell<Reservation, ReservationStatus>() {
            @Override
            protected void updateItem(ReservationStatus status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(status.toString());
                setStyle(""); // reset

                if (status == ReservationStatus.Active) {
                    setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                } else if (status == ReservationStatus.Cancelled) {
                    setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
                }
                // Finished -> default (no style)
            }
        });
    }

    private void applyFilter() {
        String input = (txtFilter == null || txtFilter.getText() == null) ? "" : txtFilter.getText();
        String filter = input.trim().toLowerCase();

        String mode = (cmbMatchMode == null || cmbMatchMode.getValue() == null)
                ? "Exact"
                : cmbMatchMode.getValue();

        boolean exact = mode.equalsIgnoreCase("Exact");

        if (filter.isEmpty()) {
            filteredReservations.setPredicate(r -> true);
            return;
        }

        filteredReservations.setPredicate(r -> {
            String id = String.valueOf(r.getReservationId());
            String code = safeLower(r.getConfirmationCode());
            String createdBy = String.valueOf(r.getCreatedByUserId());
            String status = (r.getReservationStatus() == null) ? "" : r.getReservationStatus().toString().toLowerCase();
            String tableNo = (r.getTableNumber() == null) ? "" : String.valueOf(r.getTableNumber());
            String guests = String.valueOf(r.getGuestAmount());
            String time = (r.getReservationTime() == null) ? "" : r.getReservationTime().toString().toLowerCase();

            if (exact) {
                return id.equals(filter)
                        || code.equals(filter)
                        || createdBy.equals(filter)
                        || status.equals(filter)
                        || tableNo.equals(filter)
                        || guests.equals(filter)
                        || time.equals(filter);
            }

            return id.contains(filter)
                    || code.contains(filter)
                    || createdBy.contains(filter)
                    || status.contains(filter)
                    || tableNo.contains(filter)
                    || guests.contains(filter)
                    || time.contains(filter);
        });
    }

    private String safeLower(String s) {
        return (s == null) ? "" : s.toLowerCase();
    }

    @FXML
    private void onClearFilter() {
        if (txtFilter != null) txtFilter.clear();
        if (cmbMatchMode != null) cmbMatchMode.getSelectionModel().select("Exact");
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

            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }

            stage.setMaximized(true);
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

            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }

            stage.setMaximized(true);
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

            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }

            stage.setMaximized(true);
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
