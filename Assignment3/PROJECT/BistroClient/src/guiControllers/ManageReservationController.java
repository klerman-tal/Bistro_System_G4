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

    /**
     * Injects a {@link ClientActions} implementation for controllers that rely on GUI-to-client actions.
     *
     * @param clientActions the client actions bridge used by downstream controllers
     */
    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    /**
     * Injects the current session context, initializes {@link ClientAPI}, and installs a response handler
     * that delegates responses to {@link #handleServerResponse(ResponseDTO)} on the JavaFX UI thread.
     *
     * @param user       the current logged-in user
     * @param chatClient the network client used to communicate with the server
     */
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

    /**
     * JavaFX lifecycle method (intentionally left empty).
     *
     * @param location  the location used to resolve relative paths for the root object, or {@code null} if unknown
     * @param resources the resources used to localize the root object, or {@code null} if not localized
     */
    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        // intentionally empty
    }

    /**
     * Configures the reservations table columns and binds the table to the observable data list.
     */
    private void initializeTableBehavior() {
        colReservationId.setCellValueFactory(new PropertyValueFactory<>("reservationId"));
        colDateTime.setCellValueFactory(new PropertyValueFactory<>("reservationTime"));
        colGuests.setCellValueFactory(new PropertyValueFactory<>("guestAmount"));
        colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        colCreatedBy.setCellValueFactory(new PropertyValueFactory<>("createdByUserId"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("reservationStatus"));
        colTableNumber.setCellValueFactory(new PropertyValueFactory<>("tableNumber"));

        tblReservations.setItems(reservationsList);
    }

    /**
     * Loads all reservations from the server when entering the screen.
     */
    private void loadReservationsOnEnter() {
        hideMessage();
        if (clientAPI == null) return;

        try {
            clientAPI.getAllReservations();
        } catch (IOException e) {
            showMessage("Failed to load reservations");
        }
    }

    /**
     * Processes server responses related to reservation management.
     *
     * <p>If the response contains a list of reservations, the table is updated. For other successful
     * operations (e.g., cancel), the method displays a success message and refreshes the data.</p>
     *
     * @param response the response received from the server
     */
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

    /**
     * Opens the "Add Reservation" screen and configures it to navigate back to this screen.
     */
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

    /**
     * Opens the cancel screen for the currently selected reservation.
     *
     * <p>If no reservation is selected, displays a message to the user.</p>
     */
    @FXML
    private void onDeleteClicked() {
        Reservation selected = tblReservations.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Please select a reservation to cancel.");
            return;
        }
        openCancelWindow(selected);
    }

    /**
     * Loads the cancel reservation screen, injects session context and the confirmation code,
     * and configures back navigation to this screen.
     *
     * @param reservation the reservation to cancel
     */
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

    /**
     * Navigates back to the restaurant management screen.
     */
    @FXML
    private void onBackClicked() {
        openWindow("RestaurantManagement_B.fxml", "Management");
    }

    /**
     * Loads the requested FXML and swaps the current scene root to navigate between screens.
     *
     * <p>If the target controller defines {@code setClient(User, ChatClient)}, it will be invoked
     * reflectively to preserve the session context.</p>
     *
     * @param fxmlName the target FXML file name under {@code /gui/}
     * @param title    the window title suffix to display
     */
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

    /**
     * Displays a status message in the UI.
     *
     * @param msg the message to display
     */
    private void showMessage(String msg) {
        lblStatus.setText(msg);
        lblStatus.setVisible(true);
        lblStatus.setManaged(true);
    }

    /**
     * Hides the status message area.
     */
    private void hideMessage() {
        lblStatus.setVisible(false);
        lblStatus.setManaged(false);
    }
}
