package guiControllers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

import application.ChatClient;
import dto.GetTableResultDTO;
import dto.ResponseDTO;
import entities.Enums.UserRole;
import entities.Reservation;
import entities.User;
import interfaces.ClientActions;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

/**
 * JavaFX controller for checking in to an existing reservation and receiving a table assignment.
 *
 * <p>This screen allows a user to enter a reservation confirmation code and send a check-in request.
 * For non-random users, it can also display a table of the user's active reservations, allowing
 * quick selection of the confirmation code.</p>
 *
 * <p>The controller communicates with the server through {@link ClientAPI} and processes asynchronous
 * responses via {@link ClientResponseHandler}.</p>
 */
public class GetTableFromReservation_BController implements ClientResponseHandler {

    @FXML private BorderPane rootPane;
    @FXML private TextField txtCode;
    @FXML private Button btnGetTable;
    @FXML private Label lblInfo;
    @FXML private Label lblError;

    @FXML private VBox boxMyActive;
    @FXML private TableView<Reservation> tblMyActive;
    @FXML private TableColumn<Reservation, LocalDate> colDate;
    @FXML private TableColumn<Reservation, LocalTime> colTime;
    @FXML private TableColumn<Reservation, String> colCode;

    private final ObservableList<Reservation> myActiveReservations = FXCollections.observableArrayList();

    private User user;
    private ChatClient chatClient;
    private ClientActions clientActions;
    private ClientAPI api;

    /**
     * Injects the current session context, initializes {@link ClientAPI}, registers this controller
     * as the active response handler, and initializes the "My Active Reservations" table.
     *
     * @param user       the current logged-in user
     * @param chatClient the network client used to communicate with the server
     */
    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
        this.api = new ClientAPI(chatClient);

        this.chatClient.setResponseHandler(this);

        initMyActiveTable();
        loadMyActiveIfAllowed();
    }

    /**
     * Injects a {@link ClientActions} implementation for controllers that rely on GUI-to-client actions.
     *
     * @param clientActions the client actions bridge used by downstream controllers
     */
    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    /**
     * Initializes the table that displays the user's active reservations.
     *
     * <p>Configures column value factories and attaches a listener to prefill the confirmation code
     * input field when a row is selected.</p>
     */
    private void initMyActiveTable() {
        if (tblMyActive == null) return;

        colDate.setCellValueFactory(cd -> {
            if (cd.getValue() == null || cd.getValue().getReservationTime() == null) {
                return new SimpleObjectProperty<>(null);
            }
            return new SimpleObjectProperty<>(cd.getValue().getReservationTime().toLocalDate());
        });

        colTime.setCellValueFactory(cd -> {
            if (cd.getValue() == null || cd.getValue().getReservationTime() == null) {
                return new SimpleObjectProperty<>(null);
            }
            return new SimpleObjectProperty<>(cd.getValue().getReservationTime().toLocalTime());
        });

        colCode.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue() == null ? "" : cd.getValue().getConfirmationCode()));

        tblMyActive.setItems(myActiveReservations);

        tblMyActive.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected != null && selected.getConfirmationCode() != null) {
                txtCode.setText(selected.getConfirmationCode());
            }
        });
    }

    /**
     * Loads active reservations for the current user if the role allows it.
     *
     * <p>The active reservations table is hidden for {@link UserRole#RandomClient} and shown for other roles.</p>
     */
    private void loadMyActiveIfAllowed() {
        boolean show = user != null && user.getUserRole() != UserRole.RandomClient;

        if (boxMyActive != null) {
            boxMyActive.setVisible(show);
            boxMyActive.setManaged(show);
        }

        if (!show) return;

        try {
            api.getMyActiveReservations(user.getUserId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the "Get Table" button click.
     *
     * <p>Validates that the confirmation code is a 6-digit number and sends a check-in request to the server.</p>
     */
    @FXML
    private void onGetTableClicked() {
        hideMessages();

        if (chatClient == null || api == null) {
            showError("Session error. Please login again.");
            return;
        }

        String code = txtCode.getText() == null ? "" : txtCode.getText().trim();

        if (!code.matches("\\d{6}")) {
            showError("Please enter a valid 6-digit confirmation code.");
            return;
        }

        try {
            btnGetTable.setDisable(true);
            api.checkinReservation(code);
            showInfo("Checking your reservation...");
        } catch (Exception e) {
            btnGetTable.setDisable(false);
            showError("Failed to send request to server.");
            e.printStackTrace();
        }
    }

    /**
     * Handles server responses for:
     * <ul>
     *   <li>Active reservations list (to populate the table)</li>
     *   <li>Check-in results ({@link GetTableResultDTO})</li>
     * </ul>
     *
     * @param response the response received from the server
     */
    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {
            btnGetTable.setDisable(false);

            if (response == null) {
                showError("No response from server.");
                return;
            }

            if (response.isSuccess() && response.getData() instanceof ArrayList<?> list) {
                myActiveReservations.clear();
                for (Object o : list) {
                    if (o instanceof Reservation r) {
                        myActiveReservations.add(r);
                    }
                }
                return;
            }

            GetTableResultDTO res = null;
            try { res = (GetTableResultDTO) response.getData(); } catch (Exception ignored) {}

            if (res != null) {

                if (res.isSuccess() && res.getTableNumber() != null) {
                    String msg = "Welcome 🎉\nYour table number is: " + res.getTableNumber();
                    showSuccessAlert("You are checked-in!", msg);
                    showInfo("Your table number is: " + res.getTableNumber());
                    return;
                }

                if (res.isShouldWait()) {
                    showInfo(res.getMessage() != null
                            ? res.getMessage()
                            : "Please wait. We will notify you when your table is ready.");
                    return;
                }

                showError(res.getMessage() != null ? res.getMessage() : "Check-in failed.");
                return;
            }

            if (response.isSuccess()) {
                showInfo(response.getMessage() != null ? response.getMessage() : "Done.");
            } else {
                showError(response.getMessage() != null ? response.getMessage() : "Request failed.");
            }
        });
    }

    /**
     * Handles connection errors by displaying an error message on the UI thread.
     *
     * @param e the connection exception
     */
    @Override
    public void handleConnectionError(Exception e) {
        Platform.runLater(() -> showError("Connection lost."));
    }

    /**
     * Handles connection closure events (no UI action defined in this controller).
     */
    @Override
    public void handleConnectionClosed() {}

    /**
     * Navigates back to the table-choice screen and clears the active response handler.
     */
    @FXML
    private void onBackClicked() {
        if (chatClient != null) chatClient.setResponseHandler(null);
        openWindow("GetTableChoice_B.fxml", "Get Table");
    }

    /**
     * Loads the requested FXML and swaps the current scene root to navigate between screens.
     *
     * <p>If the target controller defines {@code setClientActions(ClientActions)} and/or
     * {@code setClient(User, ChatClient)}, these will be invoked reflectively to preserve
     * session context.</p>
     *
     * @param fxmlName the target FXML file name under {@code /gui/}
     * @param title    the window title suffix to display
     */
    private void openWindow(String fxmlName, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
            Parent root = loader.load();

            Object controller = loader.getController();

            if (controller != null && clientActions != null) {
                try {
                    controller.getClass()
                            .getMethod("setClientActions", ClientActions.class)
                            .invoke(controller, clientActions);
                } catch (Exception ignored) {}
            }

            if (controller != null && user != null && chatClient != null) {
                try {
                    controller.getClass()
                            .getMethod("setClient", User.class, ChatClient.class)
                            .invoke(controller, user, chatClient);
                } catch (Exception ignored) {}
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = stage.getScene();

            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }

            stage.setTitle("Bistro - " + title);
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Hides both information and error messages.
     */
    private void hideMessages() {
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);

        lblInfo.setText("");
        lblInfo.setVisible(false);
        lblInfo.setManaged(false);
    }

    /**
     * Displays an error message and hides the information message area.
     *
     * @param msg the message to display
     */
    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);

        lblInfo.setText("");
        lblInfo.setVisible(false);
        lblInfo.setManaged(false);
    }

    /**
     * Displays an informational message and hides the error message area.
     *
     * @param msg the message to display
     */
    private void showInfo(String msg) {
        lblInfo.setText(msg);
        lblInfo.setVisible(true);
        lblInfo.setManaged(true);

        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    /**
     * Shows a success information alert dialog.
     *
     * @param title   the alert title
     * @param content the alert content text
     */
    private void showSuccessAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
