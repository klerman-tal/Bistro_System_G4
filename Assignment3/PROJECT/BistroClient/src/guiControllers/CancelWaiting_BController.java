package guiControllers;

import java.io.IOException;

import application.ChatClient;
import dto.ResponseDTO;
import entities.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

/**
 * JavaFX controller for the "Cancel Waiting" screen.
 *
 * <p>
 * This controller allows a user to cancel a waiting-list entry by providing a
 * confirmation code. It uses {@link ClientAPI} to send the cancel request to
 * the server and implements {@link ClientResponseHandler} to display the server
 * response on the UI thread.
 * </p>
 */
public class CancelWaiting_BController implements ClientResponseHandler {

	@FXML
	private BorderPane rootPane;
	@FXML
	private TextField txtConfirmationCode;
	@FXML
	private Label lblMessage;

	private User user;
	private ChatClient chatClient;
	private ClientAPI api;

	private String backFxml;

	/**
	 * Injects the session context into this controller and registers it as the
	 * active response handler.
	 *
	 * @param user       the current logged-in user
	 * @param chatClient the network client used to send requests
	 */
	public void setClient(User user, ChatClient chatClient) {
		this.user = user;
		this.chatClient = chatClient;
		this.api = new ClientAPI(chatClient);

		if (chatClient != null) {
			chatClient.setResponseHandler(this);
		}
	}

	/**
	 * Injects the session context and optionally pre-fills the confirmation code.
	 *
	 * @param user             the current logged-in user
	 * @param chatClient       the network client used to send requests
	 * @param confirmationCode the confirmation code to prefill in the UI
	 */
	public void setClient(User user, ChatClient chatClient, String confirmationCode) {
		setClient(user, chatClient);
		if (confirmationCode != null && txtConfirmationCode != null) {
			txtConfirmationCode.setText(confirmationCode);
		}
	}

	/**
	 * Defines which FXML should be loaded when the user clicks the Back button.
	 *
	 * @param backFxml the FXML resource path to navigate back to
	 */
	public void setBackFxml(String backFxml) {
		this.backFxml = backFxml;
	}

	/**
	 * Handles the Cancel Waiting button click.
	 *
	 * <p>
	 * Validates the confirmation code and sends a cancel request through
	 * {@link ClientAPI}. UI feedback is shown only after receiving the server
	 * response.
	 * </p>
	 */
	@FXML
	private void onCancelWaitingClicked() {
		hideMessage();

		String code = txtConfirmationCode.getText();
		if (code == null || code.isBlank()) {
			showError("Confirmation code is required.");
			return;
		}

		try {
			api.cancelWaiting(code.trim());
		} catch (IOException e) {
			showError("Failed to send cancel request.");
			e.printStackTrace();
		}
	}

	/**
	 * Handles the Back button click by loading the configured FXML and replacing
	 * the current scene root.
	 *
	 * <p>
	 * If the target controller has a {@code setClient(User, ChatClient)} method, it
	 * will be invoked reflectively to preserve the session context.
	 * </p>
	 */
	@FXML
	private void onBackClicked() {
		if (chatClient != null) {
			chatClient.setResponseHandler(null);
		}

		if (backFxml == null) {
			System.err.println("CancelWaiting_BController: backFxml not set");
			return;
		}

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource(backFxml));
			Parent root = loader.load();

			Object controller = loader.getController();
			if (controller != null) {
				controller.getClass().getMethod("setClient", User.class, ChatClient.class).invoke(controller, user,
						chatClient);
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

	/**
	 * Handles a server response and updates the UI accordingly.
	 *
	 * @param response the response received from the server
	 */
	@Override
	public void handleResponse(ResponseDTO response) {
		Platform.runLater(() -> {
			if (response == null)
				return;

			if (response.isSuccess()) {
				showSuccess(response.getMessage() != null ? response.getMessage() : "Waiting cancelled successfully");
			} else {
				showError(response.getMessage() != null ? response.getMessage() : "Cancel failed.");
			}
		});
	}

	/**
	 * Displays an error message in the UI.
	 *
	 * @param msg the message to display
	 */
	private void showError(String msg) {
		lblMessage.setText(msg);
		lblMessage.setStyle("-fx-text-fill: red;");
		lblMessage.setVisible(true);
	}

	/**
	 * Displays a success message in the UI.
	 *
	 * @param msg the message to display
	 */
	private void showSuccess(String msg) {
		lblMessage.setText(msg);
		lblMessage.setStyle("-fx-text-fill: green;");
		lblMessage.setVisible(true);
	}

	/**
	 * Hides the message label area.
	 */
	private void hideMessage() {
		lblMessage.setVisible(false);
	}

	/**
	 * Handles connection errors (no UI action defined in this controller).
	 *
	 * @param e the connection exception
	 */
	@Override
	public void handleConnectionError(Exception e) {
	}

	/**
	 * Handles connection closure events (no UI action defined in this controller).
	 */
	@Override
	public void handleConnectionClosed() {
	}
}
