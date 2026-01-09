package guiControllers;

import java.io.IOException;

import application.ChatClient;
import entities.User;
import interfaces.ClientActions;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ManageSubscriberController {

    @FXML private BorderPane rootPane;
    @FXML private Label lblStatus;

    private ClientActions clientActions;
    private User performedBy;      // מי שנכנס למערכת (Agent / Manager)
    private ChatClient chatClient; // אם תצטרכי לשלוח DTO בהמשך

    // SelectUser_BController מעביר clientActions ברפלקשן
    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    // מומלץ: מי שפותח את המסך יעביר גם את המשתמש המחובר
    public void setClient(User performedBy, ChatClient chatClient) {
        this.performedBy = performedBy;
        this.chatClient = chatClient;
    }

    @FXML
    private void onRefreshClicked() {
        // TODO (שלב אחר): למשוך רשימת מנויים מהשרת ולהציג בטבלה
        showMessage("Refresh - TODO");
    }

    @FXML
    private void onUpdateClicked() {
        // TODO (שלב אחר): לשלוח DTO לעדכון פרטים
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/RegisterSubscriberPopup.fxml"));
            Parent root = loader.load();

            RegisterSubscriberPopupController popupController = loader.getController();

            // מעבירים מי ביצע את הפעולה (כדי להציג תפקידים לפי הרשאות)
            if (popupController != null) {
                popupController.setPerformedBy(performedBy);
                // בהמשך נוכל גם להעביר clientActions/chatClient אם תרצי לשלוח DTO ישר מהפופאפ
                // popupController.setClientActions(clientActions);
                // popupController.setChatClient(chatClient);
            }

            Stage popupStage = new Stage();
            popupStage.setTitle("Create User");
            popupStage.initModality(Modality.WINDOW_MODAL);

            // owner = החלון הנוכחי
            Stage owner = (Stage) rootPane.getScene().getWindow();
            popupStage.initOwner(owner);

            popupStage.setScene(new Scene(root));

            // כדי שלא "יעוף" עם resize (אפשר לשנות ל-true אם תרצי)
            popupStage.setResizable(false);

            popupStage.showAndWait();

            // אחרי שהפופאפ נסגר - בד"כ נרצה לעשות refresh לרשימה (בהמשך מול שרת)
            // onRefreshClicked();

        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Failed to open create popup.");
        }
    }

    // =========================
    // Navigation helpers
    // =========================

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // להעביר clientActions למסך הבא אם יש לו setClientActions
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

    // =========================
    // UI messages
    // =========================

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
