package guiControllers;

import entities.Enums;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import network.ClientAPI;

/**
 * JavaFX popup controller used to create a new subscriber (or staff user) from the management UI.
 *
 * <p>The popup collects basic user details (name, username, phone, email) and a role that is restricted
 * by the role of the user who opened the popup (manager vs. agent). Input is validated locally before
 * sending the request to the server using {@link ClientAPI}.</p>
 *
 * <p>On successful validation, the controller sends the registration request and closes the popup window.</p>
 */
public class RegisterSubscriberPopupController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField usernameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;

    @FXML private ComboBox<Enums.UserRole> roleCombo;
    @FXML private Label lblMessage;

    private ClientAPI clientAPI;

    private static final String EMAIL_REGEX =
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    private static final String ERROR_STYLE =
            "-fx-border-color: #e74c3c; -fx-border-width: 2;";
    private static final String NORMAL_STYLE = "";

    /**
     * Initializes the popup controls and installs real-time input constraints.
     * This method is called automatically by JavaFX after FXML injection.
     */
    @FXML
    public void initialize() {
        hideMessage();
        clearFieldStyles();

        phoneField.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*")) {
                phoneField.setText(newV.replaceAll("[^\\d]", ""));
            }
            if (newV.length() > 10) {
                phoneField.setText(oldV);
            }
        });
    }

    /**
     * Injects the API instance used to send registration requests to the server.
     *
     * @param clientAPI the client API wrapper around the network client
     */
    public void setClientAPI(ClientAPI clientAPI) {
        this.clientAPI = clientAPI;
    }

    /**
     * Configures the available roles in the role dropdown according to the role of the user
     * who opened this popup.
     *
     * @param performedByRole the role of the user performing the creation action
     */
    public void setPerformedByRole(Enums.UserRole performedByRole) {

        roleCombo.getItems().clear();

        if (performedByRole == Enums.UserRole.RestaurantManager) {
            roleCombo.getItems().setAll(
                    Enums.UserRole.Subscriber,
                    Enums.UserRole.RestaurantAgent,
                    Enums.UserRole.RestaurantManager
            );
        } else if (performedByRole == Enums.UserRole.RestaurantAgent) {
            roleCombo.getItems().setAll(
                    Enums.UserRole.Subscriber
            );
        }

        roleCombo.getSelectionModel().selectFirst();
    }

    /**
     * Validates the form fields and sends a "register subscriber" request to the server.
     * Highlights invalid fields and shows a message if validation fails.
     */
    @FXML
    private void handleCreate() {
        hideMessage();
        clearFieldStyles();

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

            showMessage("Creating subscriber...");
            closeWindow();

        } catch (Exception e) {
            showMessage("Failed to send request");
        }
    }

    /**
     * Cancels the operation and closes the popup window.
     */
    @FXML
    private void handleCancel() {
        closeWindow();
    }

    /**
     * Validates an email string using a basic regex and a simple double-dot guard.
     *
     * @param email the email to validate
     * @return {@code true} if the email is considered valid; otherwise {@code false}
     */
    private boolean isValidEmail(String email) {
        return email.matches(EMAIL_REGEX) && !email.contains("..");
    }

    /**
     * Validates that the phone number contains exactly 10 digits.
     *
     * @param phone the phone number to validate
     * @return {@code true} if the phone number is valid; otherwise {@code false}
     */
    private boolean isValidPhone(String phone) {
        return phone.matches("\\d{10}");
    }

    /**
     * Applies an error style to the given field to indicate invalid input.
     *
     * @param field the field to mark as invalid
     */
    private void markInvalid(TextField field) {
        field.setStyle(ERROR_STYLE);
    }

    /**
     * Resets all input fields to the normal style (clears validation highlights).
     */
    private void clearFieldStyles() {
        firstNameField.setStyle(NORMAL_STYLE);
        lastNameField.setStyle(NORMAL_STYLE);
        usernameField.setStyle(NORMAL_STYLE);
        phoneField.setStyle(NORMAL_STYLE);
        emailField.setStyle(NORMAL_STYLE);
    }

    /**
     * Closes the popup window using one of the injected controls to locate the stage.
     */
    private void closeWindow() {
        Stage stage = (Stage) firstNameField.getScene().getWindow();
        stage.close();
    }

    /**
     * Displays a message to the user inside the popup.
     *
     * @param msg the message to display
     */
    private void showMessage(String msg) {
        lblMessage.setText(msg);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    /**
     * Hides the popup message label.
     */
    private void hideMessage() {
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
    }
}
