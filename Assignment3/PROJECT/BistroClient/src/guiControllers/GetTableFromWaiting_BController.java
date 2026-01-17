package guiControllers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

import application.ChatClient;
import dto.ResponseDTO;
import entities.Enums.UserRole;
import entities.User;
import entities.Waiting;
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
 * JavaFX controller for confirming arrival from the waiting list and receiving a table assignment.
 *
 * <p>This screen allows a user to enter a waiting-list confirmation code and confirm arrival.
 * For non-random users, it can also display a table of the user's active waiting entries and
 * allows selecting a row to prefill the confirmation code.</p>
 *
 * <p>The controller communicates with the server through {@link ClientAPI} and processes asynchronous
 * responses via {@link ClientResponseHandler}.</p>
 */
public class GetTableFromWaiting_BController implements ClientResponseHandler {

    @FXML private BorderPane rootPane;
    @FXML private TextField txtCode;
    @FXML private Button btnConfirm;
    @FXML private Label lblInfo;
    @FXML private Label lblError;

    @FXML private VBox boxMyActive;
    @FXML private TableView<Waiting> tblMyActive;
    @FXML private TableColumn<Waiting, LocalDate> colDate;
    @FXML private TableColumn<Waiting, LocalTime> colTime;
    @FXML private TableColumn<Waiting, String> colCode;

    private final ObservableList<Waiting> myActiveWaitings = FXCollections.observableArrayList();

    private User user;
    private ChatClient chatClient;
    private ClientActions clientActions;
    private ClientAPI api;

    /**
     * Injects the current session context, initializes {@link ClientAPI}, registers this controller
     * as the active response handler, and initializes the "My Active Waitings" table.
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
     * Initializes the table that displays the user's active waiting entries.
     *
     * <p>Configures column value factories and attaches a listener to prefill the confirmation code
     * input field when a row is selected.</p>
     */
    private void initMyActiveTable() {
        if (tblMyActive == null) return;

        colDate.setCellValueFactory(cd -> {
            if (cd.getValue() == null || cd.getValue().getTableFreedTime() == null) {
                return new SimpleObjectProperty<>(null);
            }
            return new SimpleObjectProperty<>(cd.getValue().getTableFreedTime().toLocalDate());
        });

        colTime.setCellValueFactory(cd -> {
            if (cd.getValue() == null || cd.getValue().getTableFreedTime() == null) {
                return new SimpleObjectProperty<>(null);
            }
            return new SimpleObjectProperty<>(cd.getValue().getTableFreedTime().toLocalTime());
        });

        colCode.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue() == null ? "" : cd.getValue().getConfirmationCode()));

        tblMyActive.setItems(myActiveWaitings);

        tblMyActive.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected != null && selected.getConfirmationCode() != null) {
                txtCode.setText(selected.getConfirmationCode());
            }
        });
    }

    /**
     * Loads active waiting entries for the current user if the role allows it and toggles table visibility.
     *
     * <p>The active waitings table is hidden for {@link UserRole#RandomClient} and shown for other roles.</p>
     */
    private void loadMyActiveIfAllowed() {
        boolean show =
                user != null &&
                user.getUserRole() != UserRole.RandomClient;

        if (boxMyActive != null) {
            boxMyActive.setVisible(show);
            boxMyActive.setManaged(show);
        }

        if (!show) return;

        try {
            api.getMyActiveWaitings(user.getUserId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the Confirm button click.
     *
     * <p>Validates that the confirmation code is a 6-digit number and sends a confirm-arrival request
     * to the server.</p>
     */
    @FXML
    private void onConfirmClicked() {
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
            btnConfirm.setDisable(true);
            api.confirmWaitingArrival(code);
            showInfo("Request sent...");
        } catch (Exception e) {
            btnConfirm.setDisable(false);
            showError("Failed to send request to server.");
            e.printStackTrace();
        }
    }

    /**
     * Handles server responses for:
     * <ul>
     *   <li>Active waiting list data (to populate the table)</li>
     *   <li>Confirm-arrival results (to show table assignment and success message)</li>
     * </ul>
     *
     * @param response the response received from the server
     */
    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {
            btnConfirm.setDisable(false);

            if (response == null) return;

            if (response.isSuccess() && response.getData() instanceof ArrayList<?> list) {
                myActiveWaitings.clear();
                for (Object o : list) {
                    if (o instanceof Waiting w) {
                        myActiveWaitings.add(w);
                    }
                }
                return;
            }

            if (response.isSuccess()) {

                Waiting w = null;
                try { w = (Waiting) response.getData(); } catch (Exception ignored) {}

                String tableMsg = "";
                if (w != null && w.getTableNumber() != null) {
                    tableMsg = "Your table number is: " + w.getTableNumber();
                }

                showSuccessAlert(
                        "You are seated!",
                        "Welcome 🎉\n" + (tableMsg.isEmpty() ? "" : tableMsg)
                );

                showInfo(tableMsg.isEmpty() ? "You are seated." : tableMsg);
                return;
            }

            String msg = response.getMessage() != null
                    ? response.getMessage()
                    : "Arrival confirm failed (expired / not ready / not found)";

            showError(msg);
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
        lblError.setVisible(false);
        lblError.setManaged(false);
        lblInfo.setVisible(false);
        lblInfo.setManaged(false);
    }

    /**
     * Displays an error message.
     *
     * @param msg the message to display
     */
    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    /**
     * Displays an informational message.
     *
     * @param msg the message to display
     */
    private void showInfo(String msg) {
        lblInfo.setText(msg);
        lblInfo.setVisible(true);
        lblInfo.setManaged(true);
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
