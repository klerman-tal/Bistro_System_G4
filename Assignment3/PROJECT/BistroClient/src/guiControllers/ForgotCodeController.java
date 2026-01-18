package guiControllers;

import application.ChatClient;
import dto.ResponseDTO;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

/**
 * JavaFX controller for recovering a subscriber code.
 *
 * <p>
 * This controller collects username, phone, and email details, sends a recovery
 * request to the server via {@link ClientAPI}, and displays the recovered
 * subscriber code in an information dialog. It implements
 * {@link ClientResponseHandler} to process the server response.
 * </p>
 */
public class ForgotCodeController implements ClientResponseHandler {

	private ChatClient chatClient;
	private ClientAPI api;
	@FXML
	private TextField usernameField, phoneField, emailField;
	@FXML
	private Label lblError;

	/**
	 * Injects the {@link ChatClient} into this controller, initializes
	 * {@link ClientAPI}, and registers this controller as the active response
	 * handler.
	 *
	 * @param chatClient the network client used to send requests
	 */
	public void setClient(ChatClient chatClient) {
		this.chatClient = chatClient;
		this.api = new ClientAPI(chatClient);
		chatClient.setResponseHandler(this);
	}

	/**
	 * Handles the Recover action.
	 *
	 * <p>
	 * Validates that all fields are filled and sends a subscriber code recovery
	 * request to the server.
	 * </p>
	 *
	 * @param event the JavaFX action event
	 */
	@FXML
	private void handleRecover(ActionEvent event) {

		if (usernameField.getText().isBlank() || phoneField.getText().isBlank() || emailField.getText().isBlank()) {

			lblError.setText("Please fill all fields.");
			lblError.setVisible(true);
			lblError.setManaged(true);
			return;
		}

		try {
			api.recoverSubscriberCode(usernameField.getText().trim(), phoneField.getText().trim(),
					emailField.getText().trim());
		} catch (Exception e) {
			lblError.setText("Failed to contact server.");
			lblError.setVisible(true);
		}
	}

	/**
	 * Handles the Cancel action by closing the current window.
	 *
	 * @param event the JavaFX action event
	 */
	@FXML
	private void handleCancel(ActionEvent event) {
		Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
		stage.close();
	}

	/**
	 * Handles a server response for the subscriber code recovery request.
	 *
	 * <p>
	 * On success, displays the recovered code in an information alert and closes
	 * the window. On failure, displays the error message.
	 * </p>
	 *
	 * @param response the response received from the server
	 */
	@Override
	public void handleResponse(ResponseDTO response) {

		Platform.runLater(() -> {

			if (!response.isSuccess()) {
				lblError.setText(response.getMessage());
				lblError.setVisible(true);
				lblError.setManaged(true);
				return;
			}

			int subscriberCode = (int) response.getData();

			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			alert.setTitle("Subscriber Code Recovered");
			alert.setHeaderText(null);
			alert.setContentText("Your subscriber code is:\n\n" + subscriberCode);
			alert.showAndWait();

			((Stage) usernameField.getScene().getWindow()).close();
		});
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
