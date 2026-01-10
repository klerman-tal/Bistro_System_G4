package guiControllers;

import java.io.IOException;

import application.ChatClient;
import entities.User;
import interfaces.ClientActions;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import network.ClientAPI;

public class ManageSubscriberController {

    @FXML private BorderPane rootPane;
    @FXML private Label lblStatus;

    private ClientActions clientActions;
    private User performedBy;      // המשתמש המחובר (Agent / Manager)
    private ChatClient chatClient;

    // API לשליחת בקשות לשרת
    private ClientAPI clientAPI;

    /* =======================
       SETTERS
       ======================= */

    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    // נקרא מ-SelectUser_BController
    public void setClient(User performedBy, ChatClient chatClient) {
        this.performedBy = performedBy;
        this.chatClient = chatClient;

        if (chatClient != null) {
            this.clientAPI = new ClientAPI(chatClient);
        }
    }

    /* =======================
       BUTTON ACTIONS
       ======================= */

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

        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/RegisterSubscriberPopup.fxml"));
            Parent root = loader.load();

            RegisterSubscriberPopupController popupController =
                    loader.getController();

            if (popupController != null) {
                // ✅ רק זה צריך לעבור לפופאפ
                popupController.setClientAPI(clientAPI);
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

    /* =======================
       NAVIGATION
       ======================= */

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

    /* =======================
       UI HELPERS
       ======================= */

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
