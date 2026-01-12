package guiControllers;

import java.io.IOException;

import application.ChatClient;
import dto.ResponseDTO;
import entities.User;
import interfaces.ClientActions;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

public class ManageSubscriberController {

    @FXML private BorderPane rootPane;
    @FXML private Label lblStatus;

    private ClientActions clientActions;
    private User performedBy;
    private ChatClient chatClient;

    private ClientAPI clientAPI;

    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    public void setClient(User performedBy, ChatClient chatClient) {
        this.performedBy = performedBy;
        this.chatClient = chatClient;

        if (chatClient != null) {
            this.clientAPI = new ClientAPI(chatClient);

            // ✅ הכי חשוב: המסך הזה מקבל את התגובות מהשרת
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
					// TODO Auto-generated method stub
					
				}
            });
        }
    }

    // ✅ פה אנחנו מחליטים מה להציג במסך
    private void handleServerResponse(ResponseDTO response) {
        if (response == null) return;

        // אנחנו יודעים ש-REGISTER_SUBSCRIBER מחזיר Integer ב-data
        if ("Subscriber created successfully".equals(response.getMessage()) && response.isSuccess()) {

            Object data = response.getData();
            if (data instanceof Integer newId) {
                showMessage("Subscriber created ✅  |  Subscriber ID: " + newId);
            } else {
                showMessage("Subscriber created ✅");
            }
            return;
        }

        // כשלון רישום
        if (response.getMessage() != null && response.getMessage().startsWith("Subscriber was not created")) {
            showMessage("❌ " + response.getMessage());
            return;
        }

        // כל תגובה אחרת - לא חייבים להציג במסך הזה
        // showMessage(response.getMessage());
    }

    @FXML
    private void onRefreshClicked() {
        showMessage("Refresh - TODO");
    }

    @FXML
    private void onUpdateClicked() {
        showMessage("Update - TODO");
    }

    @FXML
    private void onBackClicked() {
        navigateTo("/gui/selectUser.fxml", "Manage Users");
    }

    @FXML
    private void onCreateSubscriberClicked() {
        hideMessage();

        if (performedBy == null || chatClient == null || clientAPI == null) {
            showMessage("Session missing: please go back and enter this screen again.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/RegisterSubscriberPopup.fxml"));
            Parent root = loader.load();

            RegisterSubscriberPopupController popupController = loader.getController();

            if (popupController != null) {
                popupController.setClientAPI(clientAPI);
                popupController.setPerformedByRole(performedBy.getUserRole());
            }

            Stage popupStage = new Stage();
            popupStage.setTitle("Create User");
            popupStage.initModality(Modality.WINDOW_MODAL);

            Stage owner = (Stage) rootPane.getScene().getWindow();
            popupStage.initOwner(owner);

            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Failed to open create popup.");
        }
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();

            if (controller != null && clientActions != null) {
                try {
                    controller.getClass()
                            .getMethod("setClientActions", ClientActions.class)
                            .invoke(controller, clientActions);
                } catch (Exception ignored) {}
            }

            if (controller != null) {
                try {
                    controller.getClass()
                            .getMethod("setClient", User.class, ChatClient.class)
                            .invoke(controller, performedBy, chatClient);
                } catch (Exception ignored) {}
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle("Bistro - " + title);
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Failed to open: " + fxmlPath);
        }
    }

    private void showMessage(String msg) {
        if (lblStatus == null) return;
        lblStatus.setText(msg);
        lblStatus.setVisible(true);
        lblStatus.setManaged(true);
    }

    private void hideMessage() {
        if (lblStatus == null) return;
        lblStatus.setText("");
        lblStatus.setVisible(false);
        lblStatus.setManaged(false);
    }
}
