package guiControllers;

import entities.Enums;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import network.ClientAPI;

public class RegisterSubscriberPopupController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField usernameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;

    @FXML private ComboBox<Enums.UserRole> roleCombo;
    @FXML private Label lblMessage;

    private ClientAPI clientAPI;

    // ===== REGEX =====
    private static final String EMAIL_REGEX =
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    // ===== STYLE =====
    private static final String ERROR_STYLE =
            "-fx-border-color: #e74c3c; -fx-border-width: 2;";
    private static final String NORMAL_STYLE = "";

    /* =======================
       INITIALIZE
       ======================= */
    @FXML
    public void initialize() {
        hideMessage();
        clearFieldStyles();

        // ❗ חשוב: לא נוגעים כאן ב-roleCombo
        // ההחלטה על roles נעשית רק ב-setPerformedByRole()

        // 📞 טלפון – רק ספרות, עד 10
        phoneField.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*")) {
                phoneField.setText(newV.replaceAll("[^\\d]", ""));
            }
            if (newV.length() > 10) {
                phoneField.setText(oldV);
            }
        });
    }

    /* =======================
       SETTERS (חובה)
       ======================= */
    public void setClientAPI(ClientAPI clientAPI) {
        this.clientAPI = clientAPI;
    }

    /**
     * נקרא מה-Controller שפותח את ה-Popup
     */
    public void setPerformedByRole(Enums.UserRole performedByRole) {

        roleCombo.getItems().clear();

        if (performedByRole == Enums.UserRole.RestaurantManager) {
            roleCombo.getItems().setAll(
                    Enums.UserRole.Subscriber,
                    Enums.UserRole.RestaurantAgent,
                    Enums.UserRole.RestaurantManager
            );
        } 
        else if (performedByRole == Enums.UserRole.RestaurantAgent) {
            roleCombo.getItems().setAll(
                    Enums.UserRole.Subscriber
            );
        }

        roleCombo.getSelectionModel().selectFirst();
    }

    /* =======================
       CREATE
       ======================= */
    @FXML
    private void handleCreate() {
        hideMessage();
        clearFieldStyles();

        if (clientAPI == null) {
            showMessage("Internal error: client not initialized");
            return;
        }

        String firstName = firstNameField.getText().trim();
        String lastName  = lastNameField.getText().trim();
        String username  = usernameField.getText().trim();
        String phone     = phoneField.getText().trim();
        String email     = emailField.getText().trim();
        Enums.UserRole role = roleCombo.getValue();

        boolean valid = true;

        if (firstName.isEmpty()) { markInvalid(firstNameField); valid = false; }
        if (lastName.isEmpty())  { markInvalid(lastNameField);  valid = false; }
        if (username.isEmpty())  { markInvalid(usernameField);  valid = false; }
        if (!isValidPhone(phone)) { markInvalid(phoneField);    valid = false; }
        if (!isValidEmail(email)) { markInvalid(emailField);    valid = false; }

        if (!valid) {
            showMessage("Please correct the highlighted fields");
            return;
        }

        try {
            clientAPI.registerSubscriber(
                    username,
                    firstName,
                    lastName,
                    phone,
                    email,
                    role
            );
            closeWindow();
        } catch (Exception e) {
            showMessage("Failed to send request");
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    /* =======================
       VALIDATION
       ======================= */
    private boolean isValidEmail(String email) {
        return email.matches(EMAIL_REGEX) && !email.contains("..");
    }

    private boolean isValidPhone(String phone) {
        return phone.matches("\\d{10}");
    }

    /* =======================
       UI HELPERS
       ======================= */
    private void markInvalid(TextField field) {
        field.setStyle(ERROR_STYLE);
    }

    private void clearFieldStyles() {
        firstNameField.setStyle(NORMAL_STYLE);
        lastNameField.setStyle(NORMAL_STYLE);
        usernameField.setStyle(NORMAL_STYLE);
        phoneField.setStyle(NORMAL_STYLE);
        emailField.setStyle(NORMAL_STYLE);
    }

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
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
    }
}
