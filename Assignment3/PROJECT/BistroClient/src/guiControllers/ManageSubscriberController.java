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


public class ManageSubscriberController {

	@FXML
	private BorderPane rootPane;

	@FXML
	private Label lblStatus;

	// ===== TABLE =====
	@FXML
	private TableView<Subscriber> tblSubscribers;

	@FXML
	private TableColumn<Subscriber, Integer> colSubscriberId;

	@FXML
	private TableColumn<Subscriber, String> colUsername;

	@FXML
	private TableColumn<Subscriber, String> colFirstName;

	@FXML
	private TableColumn<Subscriber, String> colLastName;

	@FXML
	private TableColumn<Subscriber, String> colPhone;

	@FXML
	private TableColumn<Subscriber, String> colEmail;

	@FXML
	private TableColumn<Subscriber, String> colRole;

	// ===== FORM FIELDS =====
	@FXML
	private TextField txtSubscriberId;

	@FXML
	private TextField txtUsername;

	@FXML
	private TextField txtFirstName;

	@FXML
	private TextField txtLastName;

	@FXML
	private TextField txtPhone;

	@FXML
	private TextField txtEmail;

	@FXML
	private TextField txtRole;

	private ClientActions clientActions;
	private User performedBy;
	private ChatClient chatClient;
	private ClientAPI clientAPI;

	private final ObservableList<Subscriber> subscribersList = FXCollections.observableArrayList();

	/*
	 * ======================= SESSION SETUP =======================
	 */

	public void setClientActions(ClientActions clientActions) {
		this.clientActions = clientActions;
	}

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
					Platform.runLater(() -> showMessage("Connection error: " + exception.getMessage()));
				}

				@Override
				public void handleConnectionClosed() {
				}
			});
		}

		initializeTableBehavior();
		loadSubscribersOnEnter();
	}

	/*
	 * ======================= TABLE INITIALIZATION =======================
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

	private void fillForm(Subscriber s) {
		txtSubscriberId.setText(String.valueOf(s.getUserId()));
		txtUsername.setText(s.getUsername());
		txtFirstName.setText(s.getFirstName());
		txtLastName.setText(s.getLastName());
		txtPhone.setText(s.getPhoneNumber());
		txtEmail.setText(s.getEmail());
		txtRole.setText(s.getUserRole().name());
	}

	private void clearForm() {
		txtSubscriberId.clear();
		txtUsername.clear();
		txtFirstName.clear();
		txtLastName.clear();
		txtPhone.clear();
		txtEmail.clear();
		txtRole.clear();
	}

	/*
	 * ======================= DATA LOADING =======================
	 */

	private void loadSubscribersOnEnter() {
		hideMessage();
		try {
			clientAPI.getAllSubscribers();
			showMessage("Loading subscribers...");
		} catch (IOException e) {
			showMessage("Failed to load subscribers");
			e.printStackTrace();
		}
	}

	/*
	 * ======================= SERVER RESPONSES =======================
	 */

	private void handleServerResponse(ResponseDTO response) {
		if (response == null)
			return;

		// GET ALL SUBSCRIBERS
		if (response.isSuccess() && response.getData() instanceof List<?>) {
			@SuppressWarnings("unchecked")
			List<Subscriber> list = (List<Subscriber>) response.getData();
			subscribersList.setAll(list);
			hideMessage();
			return;
		}

		// ===== CREATE SUCCESS =====
		if (response.isSuccess() && response.getData() instanceof Integer createdId) {
		    showSubscriberCreatedAlertWithQR(createdId);
		    loadSubscribersOnEnter();
		    return;
		}


		// UPDATE SUCCESS
		if (response.isSuccess() && "Details updated successfully".equals(response.getMessage())) {

			showMessage("Subscriber updated successfully ✅");
			loadSubscribersOnEnter();
			return;
		}

		// DELETE SUCCESS
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

	/*
	 * ======================= BUTTONS =======================
	 */

	@FXML
	private void onRefreshClicked() {
		loadSubscribersOnEnter();
	}

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
			e.printStackTrace();
			showMessage("Failed to delete subscriber.");
		}
	}

	@FXML
	private void onUpdateClicked() {

		Subscriber selected = tblSubscribers.getSelectionModel().getSelectedItem();

		if (selected == null) {
			showMessage("Please select a subscriber to update.");
			return;
		}

		try {
			clientAPI.updateSubscriberDetails(selected.getUserId(), txtUsername.getText().trim(), // ✅ username
					txtFirstName.getText().trim(), // ✅ firstName
					txtLastName.getText().trim(), // ✅ lastName
					txtPhone.getText().trim(), // ✅ phone
					txtEmail.getText().trim() // ✅ email
			);

			showMessage("Updating subscriber...");
		} catch (IOException e) {
			e.printStackTrace();
			showMessage("Failed to update subscriber.");
		}
	}

	@FXML
	private void onBackClicked() {
		navigateTo("/gui/selectUser.fxml", "Manage Users");
	}

	@FXML
	private void onCreateSubscriberClicked() {
		hideMessage();

		if (performedBy == null || chatClient == null || clientAPI == null) {
			showMessage("Session missing: please re-enter screen.");
			return;
		}

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/RegisterSubscriberPopup.fxml"));
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
			e.printStackTrace();
			showMessage("Failed to open create popup.");
		}
	}

	/*
	 * ======================= NAVIGATION =======================
	 */

	private void navigateTo(String fxmlPath, String title) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
			Parent root = loader.load();

			Object controller = loader.getController();

			if (controller != null && clientActions != null) {
				controller.getClass().getMethod("setClientActions", ClientActions.class).invoke(controller,
						clientActions);
			}

			if (controller != null) {
				controller.getClass().getMethod("setClient", User.class, ChatClient.class).invoke(controller,
						performedBy, chatClient);
			}

			Stage stage = (Stage) rootPane.getScene().getWindow();
			stage.setTitle("Bistro - " + title);
			stage.setScene(new Scene(root));
			stage.show();

		} catch (Exception e) {
			e.printStackTrace();
			showMessage("Failed to open screen.");
		}
	}

	/*
	 * ======================= UI MESSAGES =======================
	 */

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
