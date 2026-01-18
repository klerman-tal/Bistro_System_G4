package guiControllers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import application.ChatClient;
import dto.ResponseDTO;
import entities.User;
import entities.Waiting;
import entities.Enums.UserRole;
import entities.Enums.WaitingStatus;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

/**
 * JavaFX controller for managing the waiting list (manager/agent view).
 *
 * <p>This screen displays all waiting entries in a table and supports adding a new waiting,
 * cancelling an existing waiting, refreshing the list, and navigating back to the management screen.
 * Data is loaded from the server using {@link ClientAPI}. The controller installs a
 * {@link ClientResponseHandler} to update the UI when server responses arrive.</p>
 *
 * <p>Navigation is performed by swapping the current scene root to preserve the window instance
 * and maximized state.</p>
 */
public class ManageWaitingListController {

    @FXML private BorderPane rootPane;

    // ===== FILTER (NEW) =====
    @FXML private TextField txtFilter;
    @FXML private ComboBox<String> cmbMatchMode;

    @FXML private TableView<Waiting> tblWaitingList;
    @FXML private TableColumn<Waiting, Integer> colWaitingId;
    @FXML private TableColumn<Waiting, Integer> colCreatedBy;
    @FXML private TableColumn<Waiting, UserRole> colRole;
    @FXML private TableColumn<Waiting, Integer> colGuests;
    @FXML private TableColumn<Waiting, String> colCode;
    @FXML private TableColumn<Waiting, LocalDateTime> colFreedTime;
    @FXML private TableColumn<Waiting, Integer> colTableNum;
    @FXML private TableColumn<Waiting, WaitingStatus> colStatus;

    private User user;
    private ChatClient chatClient;
    private ClientAPI clientAPI;

    private final ObservableList<Waiting> waitingData =
            FXCollections.observableArrayList();

    // ===== FILTER DATA (NEW) =====
    private FilteredList<Waiting> filteredWaiting;

    /**
     * Injects the current session context, initializes {@link ClientAPI}, registers a response handler,
     * and loads the initial waiting list.
     *
     * @param user       the current logged-in user
     * @param chatClient the network client used to communicate with the server
     */
    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
        this.clientAPI = new ClientAPI(chatClient);

        chatClient.setResponseHandler(new ClientResponseHandler() {
            @Override
            public void handleResponse(ResponseDTO response) {
                if (response.isSuccess() && response.getData() instanceof List<?>) {
                    Platform.runLater(() -> {
                        @SuppressWarnings("unchecked")
                        List<Waiting> list = (List<Waiting>) response.getData();
                        waitingData.setAll(list);
                        tblWaitingList.refresh();
                    });
                }
            }

            @Override public void handleConnectionError(Exception e) {}
            @Override public void handleConnectionClosed() {}
        });

        setupTable();
        loadWaitingList();
    }

    /**
     * Configures the waiting list table columns, binds it to the observable list,
     * applies row styling based on {@link WaitingStatus}, and installs filtering. (UPDATED)
     */
    private void setupTable() {
        colWaitingId.setCellValueFactory(new PropertyValueFactory<>("waitingId"));
        colCreatedBy.setCellValueFactory(new PropertyValueFactory<>("createdByUserId"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("createdByRole"));
        colGuests.setCellValueFactory(new PropertyValueFactory<>("guestAmount"));
        colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        colFreedTime.setCellValueFactory(new PropertyValueFactory<>("tableFreedTime"));
        colTableNum.setCellValueFactory(new PropertyValueFactory<>("tableNumber"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("waitingStatus"));

        tblWaitingList.setRowFactory(tv -> new TableRow<Waiting>() {
            @Override
            protected void updateItem(Waiting item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("row-cancelled", "row-seated");

                if (item != null && !empty) {
                    if (item.getWaitingStatus() == WaitingStatus.Cancelled) {
                        getStyleClass().add("row-cancelled");
                    } else if (item.getWaitingStatus() == WaitingStatus.Seated) {
                        getStyleClass().add("row-seated");
                    }
                }
            }
        });

        // ===== FILTER + SORT (NEW) =====
        filteredWaiting = new FilteredList<>(waitingData, w -> true);

        SortedList<Waiting> sorted = new SortedList<>(filteredWaiting);
        sorted.comparatorProperty().bind(tblWaitingList.comparatorProperty());

        tblWaitingList.setItems(sorted);

        // ===== MATCH MODE (Exact default) (NEW) =====
        if (cmbMatchMode != null) {
            cmbMatchMode.getItems().setAll("Contains", "Exact");
            cmbMatchMode.getSelectionModel().select("Exact");
        }

        // ===== LISTENERS (NEW) =====
        if (txtFilter != null) {
            txtFilter.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        }
        if (cmbMatchMode != null) {
            cmbMatchMode.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        }
    }

    /**
     * Applies a general filter across multiple waiting fields. (NEW)
     */
    private void applyFilter() {
        String input = (txtFilter == null || txtFilter.getText() == null) ? "" : txtFilter.getText();
        String filter = input.trim().toLowerCase();

        String mode = (cmbMatchMode == null || cmbMatchMode.getValue() == null)
                ? "Exact"
                : cmbMatchMode.getValue();

        boolean exact = mode.equalsIgnoreCase("Exact");

        if (filter.isEmpty()) {
            filteredWaiting.setPredicate(w -> true);
            return;
        }

        filteredWaiting.setPredicate(w -> {
            String id = String.valueOf(w.getWaitingId());
            String createdBy = String.valueOf(w.getCreatedByUserId());
            String role = (w.getCreatedByRole() == null) ? "" : w.getCreatedByRole().toString().toLowerCase();
            String guests = String.valueOf(w.getGuestAmount());
            String code = safeLower(w.getConfirmationCode());
            String freed = (w.getTableFreedTime() == null) ? "" : w.getTableFreedTime().toString().toLowerCase();
            String tableNo = (w.getTableNumber() == null) ? "" : String.valueOf(w.getTableNumber());
            String status = (w.getWaitingStatus() == null) ? "" : w.getWaitingStatus().toString().toLowerCase();

            if (exact) {
                return id.equals(filter)
                        || createdBy.equals(filter)
                        || role.equals(filter)
                        || guests.equals(filter)
                        || code.equals(filter)
                        || freed.equals(filter)
                        || tableNo.equals(filter)
                        || status.equals(filter);
            }

            return id.contains(filter)
                    || createdBy.contains(filter)
                    || role.contains(filter)
                    || guests.contains(filter)
                    || code.contains(filter)
                    || freed.contains(filter)
                    || tableNo.contains(filter)
                    || status.contains(filter);
        });
    }

    private String safeLower(String s) {
        return (s == null) ? "" : s.toLowerCase();
    }

    /**
     * Clears filter input and restores default match mode (Exact). (NEW)
     */
    @FXML
    private void onClearFilter() {
        if (txtFilter != null) txtFilter.clear();
        if (cmbMatchMode != null) cmbMatchMode.getSelectionModel().select("Exact");
    }

    /**
     * Opens the "Join Waiting" screen and configures it to navigate back to this screen.
     */
    @FXML
    private void onAddWaitingClicked() {
        openScreen("/gui/JoinWaiting_B.fxml", controller -> {
            JoinWaiting_BController c = (JoinWaiting_BController) controller;
            c.setClient(user, chatClient);
            c.setBackFxml("/gui/ManageWaitingList.fxml");
        });
    }

    /**
     * Opens the "Cancel Waiting" screen for the selected waiting entry.
     */
    @FXML
    private void onCancelWaitingClicked() {
        Waiting selected = tblWaitingList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        openScreen("/gui/CancelWaiting_B.fxml", controller -> {
            CancelWaiting_BController c = (CancelWaiting_BController) controller;
            c.setClient(user, chatClient, selected.getConfirmationCode());
            c.setBackFxml("/gui/ManageWaitingList.fxml");
        });
    }

    /**
     * Clears the current table data and reloads the waiting list from the server.
     */
    @FXML
    private void onRefreshClicked() {
        waitingData.clear();
        loadWaitingList();
    }

    /**
     * Requests the full waiting list from the server.
     */
    private void loadWaitingList() {
        try {
            clientAPI.getWaitingList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Navigates back to the restaurant management screen.
     */
    @FXML
    private void onBackClicked() {
        openScreen("/gui/RestaurantManagement_B.fxml", controller ->
                ((RestaurantManagement_BController) controller)
                        .setClient(user, chatClient)
        );
    }

    /**
     * Loads the requested FXML and swaps the current scene root to navigate between screens.
     */
    private void openScreen(String fxmlPath, java.util.function.Consumer<Object> injector) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller != null && injector != null) {
                injector.accept(controller);
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
            e.printStackTrace();
        }
    }
}
