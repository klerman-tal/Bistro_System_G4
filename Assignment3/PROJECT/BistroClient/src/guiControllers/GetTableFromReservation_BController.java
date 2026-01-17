package guiControllers;

import application.ChatClient;
import dto.GetTableResultDTO;
import dto.ResponseDTO;
import entities.User;
import interfaces.ClientActions;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

public class GetTableFromReservation_BController implements ClientResponseHandler {

    @FXML private BorderPane rootPane;
    @FXML private TextField txtCode;
    @FXML private Button btnGetTable;
    @FXML private Label lblInfo;
    @FXML private Label lblError;

    private User user;
    private ChatClient chatClient;
    private ClientActions clientActions;
    private ClientAPI api;

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
        this.api = new ClientAPI(chatClient);

        // this screen handles responses
        this.chatClient.setResponseHandler(this);
    }

    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    @FXML
    private void onGetTableClicked() {
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
            btnGetTable.setDisable(true);
            api.checkinReservation(code);
            showInfo("Checking your reservation...");
        } catch (Exception e) {
            btnGetTable.setDisable(false);
            showError("Failed to send request to server.");
            e.printStackTrace();
        }
    }

    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {
            btnGetTable.setDisable(false);

            if (response == null) {
                showError("No response from server.");
                return;
            }

            GetTableResultDTO res = null;
            try {
                res = (GetTableResultDTO) response.getData();
            } catch (Exception ignored) {}

            if (res != null) {

                if (res.isSuccess() && res.getTableNumber() != null) {
                    String msg = "Welcome 🎉\nYour table number is: " + res.getTableNumber();
                    showSuccessAlert("You are checked-in!", msg);
                    showInfo("Your table number is: " + res.getTableNumber());
                    return;
                }

                if (res.isShouldWait()) {
                    showInfo(res.getMessage() != null
                            ? res.getMessage()
                            : "Please wait. We will notify you when your table is ready.");
                    return;
                }

                showError(res.getMessage() != null ? res.getMessage() : "Check-in failed.");
                return;
            }

            if (response.isSuccess()) {
                showInfo(response.getMessage() != null ? response.getMessage() : "Done.");
            } else {
                showError(response.getMessage() != null ? response.getMessage() : "Request failed.");
            }
        });
    }

    @Override
    public void handleConnectionError(Exception e) {
        Platform.runLater(() -> showError("Connection lost."));
    }

    @Override
    public void handleConnectionClosed() {}

    @FXML
    private void onBackClicked() {
        if (chatClient != null) chatClient.setResponseHandler(null);
        openWindow("GetTableChoice_B.fxml", "Get Table");
    }

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
            stage.setTitle("Bistro - " + title);

            // ✅ תצוגה בלבד – בלי Scene חדשה
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

    private void hideMessages() {
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);

        lblInfo.setText("");
        lblInfo.setVisible(false);
        lblInfo.setManaged(false);
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);

        lblInfo.setText("");
        lblInfo.setVisible(false);
        lblInfo.setManaged(false);
    }

    private void showInfo(String msg) {
        lblInfo.setText(msg);
        lblInfo.setVisible(true);
        lblInfo.setManaged(true);

        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    private void showSuccessAlert(String title, String content) {
        javafx.scene.control.Alert alert =
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
