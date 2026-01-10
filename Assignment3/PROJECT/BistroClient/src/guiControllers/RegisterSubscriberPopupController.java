package guiControllers;

import entities.Enums;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterSubscriberPopupController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField usernameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;

    @FXML private ComboBox<Enums.UserRole> roleCombo;
    @FXML private Label lblMessage;

    // API for sending requests to server
    private network.ClientAPI clientAPI;

    /* =======================
       INITIALIZE
       ======================= */

    @FXML
    public void initialize() {
        hideMessage();

        // 📌 תמיד לאפשר ComboBox
        roleCombo.setDisable(false);

        // 📌 תמיד להציג את כל ה-ROLEים
        roleCombo.getItems().setAll(
                Enums.UserRole.Subscriber,
                Enums.UserRole.RestaurantAgent,
                Enums.UserRole.RestaurantManager
        );
        roleCombo.getSelectionModel().selectFirst();

        // Phone: digits only, max 10
        phoneField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;

            if (!newV.matches("\\d*")) {
                phoneField.setText(newV.replaceAll("[^\\d]", ""));
                return;
            }

            if (newV.length() > 10) {
                phoneField.setText(oldV);
            }
        });
    }

    /* =======================
       SETTERS
       ======================= */

    public void setClientAPI(network.ClientAPI clientAPI) {
        this.clientAPI = clientAPI;
    }

    /* =======================
       CREATE
       ======================= */

    @FXML
    private void handleCreate() {
        hideMessage();

        if (clientAPI == null) {
            showMessage("Internal error: client API not initialized.");
            return;
        }

        String firstName = safeTrim(firstNameField.getText());
        String lastName  = safeTrim(lastNameField.getText());
        String username  = safeTrim(usernameField.getText());
        String phone     = safeTrim(phoneField.getText());
        String email     = safeTrim(emailField.getText());

        Enums.UserRole selectedRole = roleCombo.getValue();

        if (firstName.isEmpty() || lastName.isEmpty()
                || username.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            showMessage("Please fill in all fields.");
            return;
        }

        if (selectedRole == null) {
            showMessage("Please choose a role.");
            return;
        }

        if (!email.contains("@") || !email.contains(".")) {
            showMessage("Please enter a valid email address.");
            return;
        }

        if (!phone.matches("\\d{9,10}")) {
            showMessage("Phone number must be 9-10 digits.");
            return;
        }

        try {
            clientAPI.registerSubscriber(
                    username,
                    firstName,
                    lastName,
                    phone,
                    email,
                    selectedRole
            );

            // סגירת החלון אחרי שליחה
            closeWindow();

        } catch (Exception e) {
            showMessage("Failed to send request to server.");
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    /* =======================
       HELPERS
       ======================= */

    private void closeWindow() {
        Stage stage = (Stage) firstNameField.getScene().getWindow();
        if (stage != null) stage.close();
    }

    private void showMessage(String msg) {
        lblMessage.setText(msg);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    private void hideMessage() {
        lblMessage.setText("");
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}
