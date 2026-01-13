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

public class ForgotCodeController implements ClientResponseHandler{

	private ChatClient chatClient;
    private ClientAPI api;
    @FXML private TextField usernameField, phoneField, emailField;
    @FXML private Label lblError;
    


    public void setClient(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.api = new ClientAPI(chatClient);
    }

    @FXML
    private void handleRecover(ActionEvent event) {

        if (usernameField.getText().isBlank() ||
            phoneField.getText().isBlank() ||
            emailField.getText().isBlank()) {

            lblError.setText("Please fill all fields.");
            lblError.setVisible(true);
            lblError.setManaged(true);
            return;
        }

        try {
            api.recoverSubscriberCode(
                    usernameField.getText().trim(),
                    phoneField.getText().trim(),
                    emailField.getText().trim()
            );
        } catch (Exception e) {
            lblError.setText("Failed to contact server.");
            lblError.setVisible(true);
        }
    }


    @FXML
    private void handleCancel(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

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
            alert.setContentText(
                    "Your subscriber code is:\n\n" + subscriberCode
            );
            alert.showAndWait();

            ((Stage) usernameField.getScene().getWindow()).close();
        });
    }


	@Override
	public void handleConnectionError(Exception e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleConnectionClosed() {
		// TODO Auto-generated method stub
		
	}
}