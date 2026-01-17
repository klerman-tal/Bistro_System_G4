package guiControllers;

import java.io.IOException;
import java.util.List;

import application.ChatClient;
import dto.DeleteTableDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import dto.SaveTableDTO;
import entities.Table;
import entities.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import network.ClientResponseHandler;
import protocol.Commands;

/**
 * JavaFX controller for managing restaurant tables (create/update/delete).
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Loads the current tables list from the server and displays it in a {@link TableView}.</li>
 *   <li>Validates user input (numeric-only and positive values) for table number and seats amount.</li>
 *   <li>Sends table save requests ({@link Commands#SAVE_TABLE}) to add a new table or update an existing one.</li>
 *   <li>Sends table deletion requests ({@link Commands#DELETE_TABLE}) for the selected table.</li>
 *   <li>Handles async responses from the server via {@link ClientResponseHandler} and updates the UI safely.</li>
 *   <li>Navigates back to the management screen while reusing the existing {@link Scene}.</li>
 * </ul>
 * </p>
 */
public class UpdateTablesController implements ClientResponseHandler {

    /** Active client connection used to send requests and receive async responses. */
    private ChatClient chatClient;

    /** Session user (needed mainly for navigation back to the management screen). */
    private User user;

    /* ======================= FXML ======================= */

    @FXML private BorderPane rootPane;

    /** Table UI displaying all restaurant tables received from the server. */
    @FXML private TableView<Table> tblTables;

    /** Column for table number. */
    @FXML private TableColumn<Table, Integer> colNumber;

    /** Column for seats amount. */
    @FXML private TableColumn<Table, Integer> colSeats;

    /** Input for table number (digits only). */
    @FXML private TextField txtTableNumber;

    /** Input for seats amount (digits only). */
    @FXML private TextField txtSeats;

    /** Status/feedback label for user messages. */
    @FXML private Label lblMsg;

    /** Backing list for the TableView items. */
    private final ObservableList<Table> tables = FXCollections.observableArrayList();

    /**
     * JavaFX initialization hook.
     * <p>
     * Configures the table columns, binds the observable list to the TableView,
     * applies numeric-only input limiters, and populates the form when a row is selected.
     * </p>
     */
    @FXML
    private void initialize() {
        colNumber.setCellValueFactory(new PropertyValueFactory<>("tableNumber"));
        colSeats.setCellValueFactory(new PropertyValueFactory<>("seatsAmount"));
        tblTables.setItems(tables);

        addNumericLimiter(txtTableNumber);
        addNumericLimiter(txtSeats);

        tblTables.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                txtTableNumber.setText(String.valueOf(n.getTableNumber()));
                txtSeats.setText(String.valueOf(n.getSeatsAmount()));
            }
        });
    }

    /**
     * Restricts a {@link TextField} to accept digits only (filters out non-numeric characters).
     *
     * @param tf the TextField to apply the limiter to
     */
    private void addNumericLimiter(TextField tf) {
        tf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            if (!newVal.matches("\\d*")) {
                tf.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
    }

    /* ======================= SESSION ======================= */

    /**
     * Injects the current session context.
     * <p>
     * Also registers this controller as the response handler and triggers an initial load of tables.
     * </p>
     *
     * @param user the current user
     * @param chatClient the active client connection
     */
    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        setClient(chatClient);
    }

    /**
     * Injects only the {@link ChatClient} and registers this controller as the response handler.
     * Triggers a tables request immediately.
     *
     * @param chatClient the active client connection
     */
    public void setClient(ChatClient chatClient) {
        this.chatClient = chatClient;
        chatClient.setResponseHandler(this);
        requestTables();
    }

    /* ======================= SERVER REQUESTS ======================= */

    /**
     * Requests the current tables list from the server.
     * Uses {@link Commands#GET_TABLES}.
     */
    private void requestTables() {
        try {
            chatClient.sendToServer(new RequestDTO(Commands.GET_TABLES, null));
            show("Loading tables...");
        } catch (IOException e) {
            show("Failed to load tables");
        }
    }

    /* ======================= ACTIONS ======================= */

    /**
     * Adds a new table or saves updates to an existing one.
     * <p>
     * Validates that table number and seats are present, numeric, and greater than 0,
     * then sends {@link Commands#SAVE_TABLE} with {@link SaveTableDTO}.
     * </p>
     */
    @FXML
    private void onAddOrSave() {
        hide();

        Integer num = parseInt(txtTableNumber.getText(), "Table #");
        Integer seats = parseInt(txtSeats.getText(), "Seats");

        if (num == null || seats == null) return;

        if (num <= 0) {
            show("Table number must be greater than 0");
            return;
        }
        if (seats <= 0) {
            show("Seats amount must be greater than 0");
            return;
        }

        try {
            chatClient.sendToServer(
                    new RequestDTO(Commands.SAVE_TABLE, new SaveTableDTO(num, seats))
            );
            show("Saving table...");
        } catch (IOException e) {
            show("Failed to save table");
        }
    }

    /**
     * Deletes the currently selected table.
     * <p>
     * Requires a selection in {@link #tblTables}; sends {@link Commands#DELETE_TABLE}
     * with {@link DeleteTableDTO}.
     * </p>
     */
    @FXML
    private void onDeleteSelected() {
        Table selected = tblTables.getSelectionModel().getSelectedItem();
        if (selected == null) {
            show("Select a table first");
            return;
        }

        try {
            chatClient.sendToServer(
                    new RequestDTO(
                            Commands.DELETE_TABLE,
                            new DeleteTableDTO(selected.getTableNumber())
                    )
            );
            show("Deleting table...");
        } catch (IOException e) {
            show("Failed to delete table");
        }
    }

    /**
     * Reloads tables from the server.
     */
    @FXML
    private void onRefresh() {
        requestTables();
    }

    /* ======================= SERVER RESPONSE ======================= */

    /**
     * Handles async server responses for table operations.
     * <p>
     * Expected response patterns:
     * <ul>
     *   <li>A {@link List} of {@link Table} objects on successful {@link Commands#GET_TABLES}.</li>
     *   <li>Any other successful response for save/delete triggers a refresh.</li>
     * </ul>
     * UI updates are executed on the JavaFX Application Thread using {@link Platform#runLater(Runnable)}.
     * </p>
     *
     * @param response the server response wrapper
     */
    @Override
    public void handleResponse(ResponseDTO response) {
        if (!response.isSuccess()) {
            Platform.runLater(() -> show(response.getMessage()));
            return;
        }

        Object data = response.getData();
        if (data instanceof List<?> list) {
            if (!list.isEmpty() && !(list.get(0) instanceof Table)) {
                Platform.runLater(() -> show("Unexpected data type from server"));
                return;
            }

            @SuppressWarnings("unchecked")
            List<Table> tableList = (List<Table>) list;
            Platform.runLater(() -> {
                tables.setAll(tableList);
                hide();
            });
            return;
        }

        Platform.runLater(() -> {
            hide();
            requestTables();
        });
    }

    /**
     * Called when a connection error occurs while this controller is active.
     *
     * @param e the connection exception
     */
    @Override
    public void handleConnectionError(Exception e) {
        Platform.runLater(() -> show("Connection error"));
    }

    /**
     * Called when the server connection is closed while this controller is active.
     */
    @Override
    public void handleConnectionClosed() {}

    /* ================= BACK (FIXED) ================= */

    /**
     * Navigates back to the restaurant management screen.
     * <p>
     * Reuses the existing {@link Scene} (does not create a new one) by replacing the root node.
     * Passes the current session ({@link #user}, {@link #chatClient}) into the target controller.
     * </p>
     */
    @FXML
    private void onBack() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/RestaurantManagement_B.fxml"));
            Parent root = loader.load();

            RestaurantManagement_BController controller = loader.getController();
            controller.setClient(user, chatClient);

            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = stage.getScene();

            scene.setRoot(root);
            stage.setTitle("Bistro - Restaurant Management");
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================= HELPERS ================= */

    /**
     * Parses an integer from a string and shows a user-friendly error if invalid.
     *
     * @param s the raw string to parse
     * @param field the logical field name (used in error messages)
     * @return parsed integer, or null when invalid
     */
    private Integer parseInt(String s, String field) {
        try {
            if (s == null || s.isBlank()) {
                show(field + " is required");
                return null;
            }
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            show(field + " must be a valid number");
            return null;
        }
    }

    /**
     * Displays a message to the user via {@link #lblMsg}.
     *
     * @param msg message text
     */
    private void show(String msg) {
        lblMsg.setText(msg);
        lblMsg.setVisible(true);
        lblMsg.setManaged(true);
    }

    /**
     * Hides and clears the message label.
     */
    private void hide() {
        lblMsg.setText("");
        lblMsg.setVisible(false);
        lblMsg.setManaged(false);
    }
}
