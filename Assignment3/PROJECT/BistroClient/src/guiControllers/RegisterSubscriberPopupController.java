package guiControllers;

import entities.Enums;
import entities.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class RegisterSubscriberPopupController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField usernameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;

    @FXML private ComboBox<Enums.UserRole> roleCombo;

    @FXML private Label lblMessage;

    private User performedBy; // מי שפתח את הפופאפ (Agent/Manager)

    @FXML
    public void initialize() {
        hideMessage();

        // Only digits, max 10
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

    /**
     * Call this right after loading the FXML popup.
     * Determines which roles are available based on the performer role.
     */
    public void setPerformedBy(User performedBy) {
        this.performedBy = performedBy;
        setupRoleOptions();
    }

    private void setupRoleOptions() {
        if (roleCombo == null) return;

        List<Enums.UserRole> allowed = new ArrayList<>();

        if (performedBy == null || performedBy.getUserRole() == null) {
            // safest: allow nothing
            roleCombo.setItems(FXCollections.observableArrayList());
            roleCombo.setDisable(true);
            return;
        }

        Enums.UserRole performerRole = performedBy.getUserRole();

        if (performerRole == Enums.UserRole.RestaurantAgent) {
            allowed.add(Enums.UserRole.Subscriber);
        } else if (performerRole == Enums.UserRole.RestaurantManager) {
            allowed.add(Enums.UserRole.Subscriber);
            allowed.add(Enums.UserRole.RestaurantAgent);
        } else {
            // not allowed to create users
            roleCombo.setItems(FXCollections.observableArrayList());
            roleCombo.setDisable(true);
            return;
        }

        roleCombo.setDisable(false);
        roleCombo.setItems(FXCollections.observableArrayList(allowed));
        roleCombo.getSelectionModel().selectFirst(); // default selection
    }

    @FXML
    private void handleCreate() {
        hideMessage();

        if (performedBy == null || performedBy.getUserRole() == null) {
            showMessage("No permission to create users.");
            return;
        }

        String firstName = safeTrim(firstNameField.getText());
        String lastName = safeTrim(lastNameField.getText());
        String username = safeTrim(usernameField.getText());
        String phone = safeTrim(phoneField.getText());
        String email = safeTrim(emailField.getText());

        Enums.UserRole selectedRole = roleCombo.getValue();

        if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            showMessage("Please fill in all fields.");
            return;
        }

        if (selectedRole == null) {
            showMessage("Please choose a role.");
            return;
        }

        // Permission enforcement
        if (performedBy.getUserRole() == Enums.UserRole.RestaurantAgent &&
            selectedRole != Enums.UserRole.Subscriber) {
            showMessage("Restaurant Agent can create only Subscriber.");
            return;
        }

        if (performedBy.getUserRole() == Enums.UserRole.RestaurantManager &&
            (selectedRole != Enums.UserRole.Subscriber && selectedRole != Enums.UserRole.RestaurantAgent)) {
            showMessage("Restaurant Manager can create Subscriber or Restaurant Agent.");
            return;
        }

        if (!isValidEmail(email)) {
            showMessage("Please enter a valid email address.");
            return;
        }

        if (!phone.matches("\\d{9,10}")) {
            showMessage("Phone number must be 9-10 digits.");
            return;
        }

        // ✅ For now: just close the popup (next step: send DTO to server)
        closeWindow();
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = getStage();
        if (stage != null) stage.close();
    }

    private Stage getStage() {
        if (firstNameField == null) return null;
        return (Stage) firstNameField.getScene().getWindow();
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

    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".") && email.length() >= 5;
    }
}
