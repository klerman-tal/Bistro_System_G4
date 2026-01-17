package guiControllers;

import java.io.IOException;
import java.util.List;

import application.ChatClient;
import dto.ResponseDTO;
import entities.Subscriber;
import entities.User;
import interfaces.ClientActions;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;
import utils.QRUtil;

/**
 * JavaFX controller for managing subscribers (manager/agent view).
 *
 * <p>This screen displays all subscribers in a table, allows selecting a subscriber to populate
 * an editable form, and supports refreshing, updating, deleting, and creating subscribers.
 * It communicates with the server via {@link ClientAPI} and processes asynchronous responses
 * through a {@link ClientResponseHandler} registered on the current {@link ChatClient}.</p>
 *
 * <p>Navigation is performed by swapping the current scene root to preserve the window instance
 * and maximized state.</p>
 */
public class ManageSubscriberController {

    @FXML private BorderPane rootPane;
    @FXML private Label lblStatus;

    @FXML private TableView<Subscriber> tblSubscribers;
    @FXML private TableColumn<Subscriber, Integer> colSubscriberId;
    @FXML private TableColumn<Subscriber, String> colUsername;
    @FXML private TableColumn<Subscriber, String> colFirstName;
    @FXML private TableColumn<Subscriber, String> colLastName;
    @FXML private TableColumn<Subscriber, String> colPhone;
    @FXML private TableColumn<Subscriber, String> colEmail;
    @FXML private TableColumn<Subscriber, String> colRole;

    @FXML private TextField txtSubscriberId;
    @FXML private TextField txtUsername;
    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;
    @FXML private TextField txtRole;

    private ClientActions clientActions;
    private User performedBy;
    private ChatClient chatClient;
    private ClientAPI clientAPI;

    private final ObservableList<Subscriber> subscribersList =
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
     * Injects the current session context, initializes {@link ClientAPI}, and registers a response handler
     * that delegates server responses to {@link #handleServerResponse(ResponseDTO)} on the JavaFX UI thread.
     *
     * @param performedBy the user performing the management actions
     * @param chatClient   the network client used to communicate with the server
     */
    public void setClient(User performedBy, ChatClient chatClient) {
        this.performedBy = performedBy;
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
        loadSubscribersOnEnter();
    }

    /**
     * Configures the subscribers table columns, binds the table to the observable list,
     * and installs a selection listener to populate the form.
     */
    private void initializeTableBehavior() {
        colSubscriberId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("userRole"));

        tblSubscribers.setItems(subscribersList);

        tblSubscribers.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                fillForm(selected);
            }
        });
    }

    /**
     * Fills the form fields with details from the selected subscriber.
     *
     * @param s the selected subscriber
     */
    private void fillForm(Subscriber s) {
        txtSubscriberId.setText(String.valueOf(s.getUserId()));
        txtUsername.setText(s.getUsername());
        txtFirstName.setText(s.getFirstName());
        txtLastName.setText(s.getLastName());
        txtPhone.setText(s.getPhoneNumber());
        txtEmail.setText(s.getEmail());
        txtRole.setText(s.getUserRole().name());
    }

    /**
     * Clears all form input fields.
     */
    private void clearForm() {
        txtSubscriberId.clear();
        txtUsername.clear();
        txtFirstName.clear();
        txtLastName.clear();
        txtPhone.clear();
        txtEmail.clear();
        txtRole.clear();
    }

    /**
     * Requests the subscribers list from the server when entering the screen.
     */
    private void loadSubscribersOnEnter() {
        hideMessage();
        try {
            clientAPI.getAllSubscribers();
            showMessage("Loading subscribers...");
        } catch (IOException e) {
            showMessage("Failed to load subscribers");
        }
    }

    /**
     * Processes server responses related to subscriber management.
     *
     * <p>Supported response cases:</p>
     * <ul>
     *   <li>List of subscribers: updates the table.</li>
     *   <li>Created subscriber ID (Integer): shows a QR popup and refreshes the list.</li>
     *   <li>Update success message: shows confirmation and refreshes the list.</li>
     *   <li>Delete success (null data): shows confirmation, clears the form, and refreshes the list.</li>
     * </ul>
     *
     * @param response the response received from the server
     */
    private void handleServerResponse(ResponseDTO response) {
        if (response == null) return;

        if (response.isSuccess() && response.getData() instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<Subscriber> list = (List<Subscriber>) response.getData();
            subscribersList.setAll(list);
            hideMessage();
            return;
        }

        if (response.isSuccess() && response.getData() instanceof Integer createdId) {
            showSubscriberCreatedAlertWithQR(createdId);
            loadSubscribersOnEnter();
            return;
        }

        if (response.isSuccess() && "Details updated successfully".equals(response.getMessage())) {
            showMessage("Subscriber updated successfully ✅");
            loadSubscribersOnEnter();
            return;
        }

        if (response.isSuccess() && response.getData() == null) {
            showMessage("Subscriber deleted successfully ✅");
            clearForm();
            loadSubscribersOnEnter();
            return;
        }

        if (!response.isSuccess()) {
            showMessage(response.getMessage());
        }
    }

    /**
     * Refreshes the subscribers list.
     */
    @FXML
    private void onRefreshClicked() {
        loadSubscribersOnEnter();
    }

    /**
     * Deletes the currently selected subscriber.
     *
     * <p>If no subscriber is selected, displays a message to the user.</p>
     */
    @FXML
    private void onDeleteClicked() {
        Subscriber selected = tblSubscribers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Please select a subscriber to delete.");
            return;
        }

        try {
            clientAPI.deleteSubscriber(selected.getUserId());
            showMessage("Deleting subscriber...");
        } catch (IOException e) {
            showMessage("Failed to delete subscriber.");
        }
    }

    /**
     * Updates the currently selected subscriber using the values in the form fields.
     *
     * <p>Performs basic phone and email validation before sending the update request.</p>
     */
    @FXML
    private void onUpdateClicked() {
        hideMessage();

        Subscriber selected = tblSubscribers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Please select a subscriber to update.");
            return;
        }

        String phone = txtPhone.getText().trim();
        String email = txtEmail.getText().trim();

        if (!phone.startsWith("05") || phone.length() != 10 || !phone.matches("\\d+")) {
            showMessage("Invalid phone: must start with '05' and be exactly 10 digits.");
            return;
        }

        if (!email.contains("@") || !email.contains(".") || email.indexOf("@") > email.lastIndexOf(".")) {
            showMessage("Invalid email: must contain '@' and a '.'.");
            return;
        }

        try {
            clientAPI.updateSubscriberDetails(
                    selected.getUserId(),
                    txtUsername.getText().trim(),
                    txtFirstName.getText().trim(),
                    txtLastName.getText().trim(),
                    phone,
                    email
            );
            showMessage("Updating subscriber...");
        } catch (IOException e) {
            showMessage("Failed to update subscriber.");
        }
    }

    /**
     * Navigates back to the restaurant management screen.
     */
    @FXML
    private void onBackClicked() {
        navigateTo("/gui/RestaurantManagement_B.fxml", "Restaurant Management");
    }

    /**
     * Opens the "Create Subscriber" popup window and injects required context into the popup controller.
     */
    @FXML
    private void onCreateSubscriberClicked() {
        hideMessage();

        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/RegisterSubscriberPopup.fxml"));
            Parent root = loader.load();

            RegisterSubscriberPopupController popupController = loader.getController();
            popupController.setClientAPI(clientAPI);
            popupController.setPerformedByRole(performedBy.getUserRole());

            Stage popupStage = new Stage();
            popupStage.setTitle("Create User");
            popupStage.initModality(Modality.WINDOW_MODAL);
            popupStage.initOwner((Stage) rootPane.getScene().getWindow());
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();

        } catch (IOException e) {
            showMessage("Failed to open create popup.");
        }
    }

    /**
     * Loads the requested FXML and swaps the current scene root to navigate between screens.
     *
     * <p>If the target controller defines {@code setClientActions(ClientActions)} and/or
     * {@code setClient(User, ChatClient)}, these will be invoked reflectively to preserve context.</p>
     *
     * @param fxmlPath the classpath resource path of the target FXML
     * @param title    the window title suffix to display
     */
    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
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
                        .invoke(controller, performedBy, chatClient);
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
            showMessage("Failed to open screen.");
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
     * Clears and hides the status message area.
     */
    private void hideMessage() {
        lblStatus.setText("");
        lblStatus.setVisible(false);
        lblStatus.setManaged(false);
    }

    /**
     * Shows an information alert indicating a subscriber was created, including a QR code for the ID.
     *
     * @param createdId the created subscriber ID
     */
    private void showSubscriberCreatedAlertWithQR(int createdId) {
        Image qrImage = QRUtil.generateQR(String.valueOf(createdId));
        ImageView qrView = new ImageView(qrImage);
        qrView.setFitWidth(220);
        qrView.setFitHeight(220);
        qrView.setPreserveRatio(true);

        Label lbl = new Label("Subscriber Code: " + createdId);
        VBox content = new VBox(15, lbl, qrView);
        content.setAlignment(Pos.CENTER);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Subscriber Created");
        alert.setHeaderText(null);
        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }
}
