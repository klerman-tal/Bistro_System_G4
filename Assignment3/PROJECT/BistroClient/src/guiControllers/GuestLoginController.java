package guiControllers;

import application.ChatClient;
import application.ClientSession;
import dto.ResponseDTO;
import entities.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

import java.io.IOException;
import java.util.function.UnaryOperator;

/**
 * JavaFX controller for guest login.
 *
 * <p>
 * This controller validates guest phone and email input, sends a guest login
 * request to the server via {@link ClientAPI}, and on success stores the
 * logged-in user in {@link ClientSession} before navigating to the main menu.
 * It also opens a recovery popup for retrieving a guest confirmation code.
 * </p>
 *
 * <p>
 * The controller implements {@link ClientResponseHandler} to process
 * asynchronous server responses and connection events.
 * </p>
 */
public class GuestLoginController implements ClientResponseHandler {

	@FXML
	private TextField phoneField;
	@FXML
	private TextField emailField;
	@FXML
	private Label lblMessage;

	private ChatClient chatClient;
	private ClientAPI api;

	private final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

	/**
	 * Initializes the controller after FXML injection.
	 *
	 * <p>
	 * Applies a numeric-only input filter to the phone field and limits input
	 * length to 10 digits.
	 * </p>
	 */
	@FXML
	public void initialize() {
		UnaryOperator<TextFormatter.Change> filter = change -> {
			String text = change.getControlNewText();
			if (text.matches("\\d*") && text.length() <= 10)
				return change;
			return null;
		};
		phoneField.setTextFormatter(new TextFormatter<>(filter));
	}

	/**
	 * Injects the {@link ChatClient} into this controller, initializes
	 * {@link ClientAPI}, and registers this controller as the active response
	 * handler.
	 *
	 * @param chatClient the network client used to send requests
	 */
	public void setClient(ChatClient chatClient) {
		this.chatClient = chatClient;
		if (chatClient != null) {
			this.api = new ClientAPI(chatClient);
			this.chatClient.setResponseHandler(this);
		}
	}

	/**
	 * Handles the Guest Login action.
	 *
	 * <p>
	 * Validates the phone and email inputs and sends a guest login request to the
	 * server.
	 * </p>
	 *
	 * @param event the JavaFX action event
	 */
	@FXML
	private void handleGuestLogin(ActionEvent event) {
		hideError();

		String phone = phoneField.getText() != null ? phoneField.getText().trim() : "";
		String email = emailField.getText() != null ? emailField.getText().trim() : "";

		if (phone.isEmpty() && email.isEmpty()) {
			showError("Please enter both phone and email.");
			return;
		}

		
		if (!phone.isEmpty()) {
			if (!phone.startsWith("05") || phone.length() != 10) {
				showError("Phone must start with '05' and be exactly 10 digits.");
				return;
		}
		}

		if (!email.isEmpty()) {
		
			if (!email.matches(EMAIL_REGEX)) {
				showError("Invalid email address (check for special characters or typos).");
				return;
			}

		}
		
		if (api != null) {
			try {
				api.loginGuest(phone, email);
				lblMessage.setText("Connecting...");
				lblMessage.setVisible(true);
				lblMessage.setManaged(true);
			} catch (IOException e) {
				showError("Connection error.");
			}
		} else {
			showError("Client not initialized.");
		}
	}

	/**
	 * Handles the server response to the guest login request.
	 *
	 * <p>
	 * On success, stores the logged-in user in {@link ClientSession} and navigates
	 * to the menu. On failure, displays the error message.
	 * </p>
	 *
	 * @param response the response received from the server
	 */
	@Override
	public void handleResponse(ResponseDTO response) {
		Platform.runLater(() -> {
			if (response.isSuccess()) {
				User guestUser = (User) response.getData();

				ClientSession.setLoggedInUser(guestUser);
				ClientSession.setActingUser(guestUser);

				goToMenu(guestUser);
			} else {
				showError(response.getMessage());
			}
		});
	}

	/**
	 * Navigates to the main menu screen while preserving the current session
	 * context.
	 *
	 * @param loggedInUser the user to inject into the menu controller
	 */
	private void goToMenu(User loggedInUser) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Menu_B.fxml"));
			Parent root = loader.load();
			Menu_BController menu = loader.getController();
			menu.setClient(loggedInUser, chatClient);

			Stage stage = (Stage) phoneField.getScene().getWindow();

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
			showError("Failed to open menu.");
		}
	}

	/**
	 * Opens a modal popup for recovering a guest reservation confirmation code.
	 *
	 * <p>
	 * After closing the popup, this controller is re-registered as the active
	 * response handler.
	 * </p>
	 *
	 * @param event the JavaFX action event
	 */
	@FXML
	private void handleForgotConfirmationCode(ActionEvent event) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ForgotGuestConfirmation.fxml"));
			Parent root = loader.load();
			Object controller = loader.getController();
			if (controller instanceof ForgotGuestConfirmationController) {
				((ForgotGuestConfirmationController) controller).setClient(chatClient);
			}
			Stage popupStage = new Stage();
			popupStage.initModality(Modality.APPLICATION_MODAL);
			popupStage.setTitle("Recover Confirmation Code");
			popupStage.setScene(new Scene(root));
			popupStage.setResizable(false);
			popupStage.showAndWait();
			if (chatClient != null) {
				chatClient.setResponseHandler(this);
			}
		} catch (IOException e) {
			e.printStackTrace();
			showError("Failed to open recovery window.");
		}
	}

	/**
	 * Handles the Back action by navigating to the login screen.
	 *
	 * @param event the JavaFX action event
	 */
	@FXML
	private void handleBackButton(ActionEvent event) {
		navigateTo(event, "/gui/Login_B.fxml");
	}

	/**
	 * Navigates to a target FXML screen by swapping the current scene root.
	 *
	 * @param event    the JavaFX action event
	 * @param fxmlPath the classpath resource path to the FXML file
	 */
	private void navigateTo(ActionEvent event, String fxmlPath) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
			Parent root = loader.load();
			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

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
		}
	}

	/**
	 * Displays an error or status message in the UI label.
	 *
	 * @param msg the message to display
	 */
	private void showError(String msg) {
		if (lblMessage != null) {
			lblMessage.setText(msg);
			lblMessage.setVisible(true);
			lblMessage.setManaged(true);
		}
	}

	/**
	 * Hides the message label area.
	 */
	private void hideError() {
		if (lblMessage != null) {
			lblMessage.setVisible(false);
			lblMessage.setManaged(false);
		}
	}

	/**
	 * Handles connection errors by displaying an error message on the UI thread.
	 *
	 * @param e the connection exception
	 */
	@Override
	public void handleConnectionError(Exception e) {
		Platform.runLater(() -> showError("Server connection lost."));
	}

	/**
	 * Handles connection closure events (no UI action defined in this controller).
	 */
	@Override
	public void handleConnectionClosed() {
	}
}
