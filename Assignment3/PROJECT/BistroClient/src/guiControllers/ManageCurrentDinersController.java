package guiControllers;

import java.io.IOException;
import java.util.List;

import application.ChatClient;
import dto.CurrentDinerDTO;
import dto.ResponseDTO;
import entities.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import network.ClientAPI;
import network.ClientResponseHandler;

/**
 * JavaFX controller for displaying the current diners list.
 *
 * <p>This controller requests a list of current diners from the server via {@link ClientAPI} and
 * renders the results in a {@link TableView}. It also supports navigation back to the restaurant
 * management screen while preserving the current session context.</p>
 *
 * <p>The controller implements {@link ClientResponseHandler} to process asynchronous server responses
 * and connection events.</p>
 */
public class ManageCurrentDinersController implements ClientResponseHandler {

    @FXML private BorderPane rootPane;
    @FXML private Label lblStatus;

    @FXML private TableView<CurrentDinerDTO> tblCurrentDiners;
    @FXML private TableColumn<CurrentDinerDTO, Integer> colCreatedBy;
    @FXML private TableColumn<CurrentDinerDTO, String> colCreatedByRole;
    @FXML private TableColumn<CurrentDinerDTO, Integer> colTableNumber;

    private User user;
    private ChatClient chatClient;
    private ClientAPI clientAPI;

    private final ObservableList<CurrentDinerDTO> dinersList =
            FXCollections.observableArrayList();

    /**
     * Injects the current session context, initializes {@link ClientAPI}, registers this controller
     * as the active response handler, and triggers the initial data load.
     *
     * @param user       the current logged-in user
     * @param chatClient the network client used to communicate with the server
     */
    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;

        if (chatClient != null) {
            this.clientAPI = new ClientAPI(chatClient);
            chatClient.setResponseHandler(this);
        }

        initTable();
        loadCurrentDiners();
    }

    /**
     * Initializes the table columns and binds the table to the observable data list.
     */
    private void initTable() {
        colCreatedBy.setCellValueFactory(new PropertyValueFactory<>("createdBy"));
        colCreatedByRole.setCellValueFactory(new PropertyValueFactory<>("createdByRole"));
        colTableNumber.setCellValueFactory(new PropertyValueFactory<>("tableNumber"));
        tblCurrentDiners.setItems(dinersList);
    }

    /**
     * Requests the current diners list from the server.
     */
    private void loadCurrentDiners() {
        hideMessage();

        if (clientAPI == null) {
            showMessage("ClientAPI is null (setClient was not called)");
            return;
        }

        try {
            clientAPI.getCurrentDiners();
        } catch (IOException e) {
            showMessage("Failed to load current diners");
        }
    }

    /**
     * Handles server responses for the "current diners" request.
     *
     * <p>On success, replaces the table content with the received list.</p>
     *
     * @param response the response received from the server
     */
    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {

            if (response == null) return;

            if (response.isSuccess() && response.getData() instanceof List<?>) {

                @SuppressWarnings("unchecked")
                List<CurrentDinerDTO> list =
                        (List<CurrentDinerDTO>) response.getData();

                dinersList.setAll(list);
                hideMessage();
            }
            else if (response.isSuccess()) {
                showMessage(response.getMessage());
            }
            else {
                showMessage("Error: " + response.getMessage());
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
        Platform.runLater(() ->
                showMessage("Connection error: " + e.getMessage()));
    }

    /**
     * Handles connection closure events (no UI action defined in this controller).
     */
    @Override
    public void handleConnectionClosed() {}

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

    /**
     * Navigates back to the restaurant management screen while preserving session context.
     */
    @FXML
    private void onBackClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/gui/RestaurantManagement_B.fxml")
            );

            Parent root = loader.load();
            RestaurantManagement_BController controller = loader.getController();
            controller.setClient(user, chatClient);

            javafx.stage.Stage stage =
                    (javafx.stage.Stage) rootPane.getScene().getWindow();

            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }

            stage.setMaximized(true);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Failed to go back");
        }
    }
}
