package guiControllers;

import java.io.IOException;

import application.ChatClient;
import application.ClientSession;
import dto.ResponseDTO;
import entities.Enums;
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

/**
 * Main menu controller for the Bistro client.
 * <p>
 * This screen is opened after a successful login and serves as the navigation hub.
 * It also supports a staff "Act as" mechanism that allows Restaurant Agents/Managers
 * to perform actions on behalf of another user (registered user by ID) or a newly created guest (by phone).
 * </p>
 *
 * <h3>Main responsibilities</h3>
 * <ul>
 *   <li>Receives and stores the current session context (logged-in {@link User} + {@link ChatClient}).</li>
 *   <li>Controls visibility of menu buttons according to the logged-in user's role.</li>
 *   <li>Allows staff to set an "acting user" using {@link ClientSession}:
 *       <ul>
 *         <li>Find existing user by ID</li>
 *         <li>Create guest by phone</li>
 *         <li>Reset acting user back to the logged-in user</li>
 *       </ul>
 *   </li>
 *   <li>Performs navigation by reusing the same {@link Scene} and replacing only the root.</li>
 *   <li>Implements {@link ClientResponseHandler} to receive async responses (find user / create guest).</li>
 * </ul>
 */
public class Menu_BController implements ClientResponseHandler {

    @FXML private BorderPane rootPane;

    @FXML private Button btnPersonalDetails;
    @FXML private Button btnRestaurantManagement;

    @FXML private BorderPane actAsBox;
    @FXML private TextField txtIdForRegistered;
    @FXML private TextField txtPhoneForNewGuest;
    @FXML private Button btnOk;
    @FXML private Label lblActingUser;

    @FXML private Label lblMessage;

    /** The user that actually logged in (staff uses this as the real identity). */
    private User loggedInUser;

    /** Active network client connection used for requests and async responses. */
    private ChatClient chatClient;

    /** Optional actions interface (injected from outside), passed to controllers that support it. */
    private ClientActions clientActions;

    /** Convenience API wrapper around {@link ChatClient} for common client requests. */
    private ClientAPI api;

    /* ===================== SETUP ===================== */

    /**
     * Injects the session context into this screen.
     * <p>
     * This method:
     * <ul>
     *   <li>Stores the logged-in user and chat client references</li>
     *   <li>Initializes {@link ClientAPI}</li>
     *   <li>Updates {@link ClientSession} (logged-in user, acting user, chat client)</li>
     *   <li>Registers this controller as the current {@link ClientResponseHandler}</li>
     *   <li>Applies role-based UI visibility (management and act-as controls)</li>
     * </ul>
     * </p>
     *
     * @param user      the logged-in user
     * @param chatClient the active connection to the server
     */
    public void setClient(User user, ChatClient chatClient) {
        this.loggedInUser = user;
        this.chatClient = chatClient;
        this.api = new ClientAPI(chatClient);

        ClientSession.setLoggedInUser(user);
        ClientSession.setChatClient(chatClient);

        // Ensure acting user is always set (defaults to logged-in user).
        if (ClientSession.getActingUser() == null) {
            ClientSession.resetActingUser();
        }

        // Receive async responses while this menu is open (find user / create guest).
        if (chatClient != null) {
            chatClient.setResponseHandler(this);
        }

        boolean isStaff =
                user != null &&
                (user.getUserRole() == Enums.UserRole.RestaurantAgent
              || user.getUserRole() == Enums.UserRole.RestaurantManager);

        // Restaurant Management is visible only to staff.
        btnRestaurantManagement.setVisible(isStaff);
        btnRestaurantManagement.setManaged(isStaff);

        // Personal details is hidden for RandomClient.
        boolean showPersonal =
                user != null && user.getUserRole() != Enums.UserRole.RandomClient;

        btnPersonalDetails.setVisible(showPersonal);
        btnPersonalDetails.setManaged(showPersonal);

        // Act-as controls are shown only to staff.
        setActingControlsVisible(isStaff);
        clearOkFields();
        updateActingUserLabel();
        hideMessage();
    }

    /**
     * Injects {@link ClientActions} (optional).
     * Some screens may expect this interface for actions beyond direct server calls.
     *
     * @param clientActions client actions implementation
     */
    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    /**
     * Shows/hides the "Act as" section.
     *
     * @param visible true to show, false to hide
     */
    private void setActingControlsVisible(boolean visible) {
        if (actAsBox != null) {
            actAsBox.setVisible(visible);
            actAsBox.setManaged(visible);
        }
        if (txtIdForRegistered != null) {
            txtIdForRegistered.setVisible(visible);
            txtIdForRegistered.setManaged(visible);
        }
        if (txtPhoneForNewGuest != null) {
            txtPhoneForNewGuest.setVisible(visible);
            txtPhoneForNewGuest.setManaged(visible);
        }
        if (btnOk != null) {
            btnOk.setVisible(visible);
            btnOk.setManaged(visible);
        }
        if (lblActingUser != null) {
            lblActingUser.setVisible(visible);
            lblActingUser.setManaged(visible);
        }
    }

    /* ===================== MENU BUTTONS ===================== */

    /**
     * Opens the reservation menu screen.
     */
    @FXML
    private void onSelectReservationsClicked() {
        openWindow("ReservationMenu_B.fxml", "Reservations", false);
    }

    /**
     * Opens the "My Visit" menu screen.
     */
    @FXML
    private void onSelectMyVisitClicked() {
        openWindow("MyVisitMenu_B.fxml", "My Visit", false);
    }

    /**
     * Opens the personal details screen.
     */
    @FXML
    private void onSelectPersonalDetailsClicked() {
        openWindow("ClientDetails_B.fxml", "Personal Details", false);
    }

    /**
     * Opens the restaurant management screen (staff only).
     * <p>
     * Note: Management screens always use the logged-in user (not the acting user),
     * so staff identity is preserved.
     * </p>
     */
    @FXML
    private void onSelectRestaurantManagementClicked() {
        openWindow("RestaurantManagement_B.fxml", "Restaurant Management", true);
    }

    /**
     * Resets the acting user to the logged-in user.
     * This is useful after finishing actions performed on behalf of someone else.
     */
    @FXML
    private void onResetToMyselfClicked() {
        ClientSession.setActingUser(ClientSession.getLoggedInUser());
        clearOkFields();
        updateActingUserLabel();
        showMessage("Back to logged-in user.", "blue");
    }

    /**
     * Logs out and navigates back to the login screen.
     * Clears the session users in {@link ClientSession}.
     */
    @FXML
    private void onLogoutClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Login_B.fxml"));
            Parent root = loader.load();

            ClientSession.setLoggedInUser(null);
            ClientSession.setActingUser(null);

            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = stage.getScene();

            // Rule: reuse Scene, replace root.
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }

            stage.setTitle("Bistro - Login");
            stage.setMaximized(true);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* ===================== NAVIGATION ===================== */

    /**
     * Loads an FXML screen and navigates to it by swapping the scene root.
     * <p>
     * Also injects:
     * <ul>
     *   <li>{@link ClientActions} if the controller supports setClientActions</li>
     *   <li>{@link User} + {@link ChatClient} via a reflective setClient(User, ChatClient) if present</li>
     * </ul>
     * </p>
     *
     * @param fxmlName          FXML file name (located under /gui/)
     * @param title             window title suffix
     * @param managementScreen  if true, pass logged-in user; otherwise pass acting user
     */
    private void openWindow(String fxmlName, String title, boolean managementScreen) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
            Parent root = loader.load();

            Object controller = loader.getController();

            // Inject ClientActions (if supported)
            if (controller != null && clientActions != null) {
                try {
                    controller.getClass()
                            .getMethod("setClientActions", ClientActions.class)
                            .invoke(controller, clientActions);
                } catch (Exception ignored) {}
            }

            // For normal screens -> acting user, for management -> logged-in user
            User userToPass = managementScreen
                    ? ClientSession.getLoggedInUser()
                    : ClientSession.getActingUser();

            // Inject session (if supported)
            if (controller != null && userToPass != null && chatClient != null) {
                try {
                    controller.getClass()
                            .getMethod("setClient", User.class, ChatClient.class)
                            .invoke(controller, userToPass, chatClient);
                } catch (Exception ignored) {}
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = stage.getScene();

            // Rule: reuse Scene, replace root.
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }

            stage.setTitle("Bistro - " + title);
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the "OK" action in the staff "Act as" box.
     * <p>
     * Input rules:
     * <ul>
     *   <li>Only staff may use this action</li>
     *   <li>User must fill ONLY one field: ID or phone</li>
     *   <li>Both fields empty resets acting user to logged-in user</li>
     * </ul>
     * </p>
     *
     * Behavior:
     * <ul>
     *   <li>If ID provided -> sends request to find user by ID</li>
     *   <li>If phone provided -> sends request to create a guest by phone</li>
     * </ul>
     */
    @FXML
    private void onOkClicked() {
        hideMessage();

        if (loggedInUser == null || api == null) {
            showMessage("Internal error: session not initialized.", "red");
            return;
        }

        // Only staff can use OK
        if (loggedInUser.getUserRole() != Enums.UserRole.RestaurantAgent &&
            loggedInUser.getUserRole() != Enums.UserRole.RestaurantManager) {
            showMessage("Permission denied.", "red");
            return;
        }

        String idStr = txtIdForRegistered != null ? txtIdForRegistered.getText().trim() : "";
        String phoneStr = txtPhoneForNewGuest != null ? txtPhoneForNewGuest.getText().trim() : "";

        boolean idFilled = !idStr.isBlank();
        boolean phoneFilled = !phoneStr.isBlank();

        // Both filled is not allowed
        if (idFilled && phoneFilled) {
            showMessage("Please fill only ONE field (ID or Phone).", "red");
            return;
        }

        // both empty -> reset acting user to myself
        if (!idFilled && !phoneFilled) {
            ClientSession.resetActingUser();
            updateActingUserLabel();
            showMessage("Acting user reset to logged-in user.", "blue");
            return;
        }

        // ID path
        if (idFilled) {
            int id;
            try {
                id = Integer.parseInt(idStr);
                if (id <= 0) throw new NumberFormatException();
            } catch (Exception e) {
                showMessage("Invalid ID.", "red");
                return;
            }

            try {
                api.findUserById(id);
                showMessage("Searching user...", "blue");
            } catch (IOException e) {
                showMessage("Connection error.", "red");
            }
            return;
        }

        // Phone path
        if (phoneFilled) {
            // Local validation: Israeli mobile format (starts with 05, exactly 10 digits)
            if (!phoneStr.matches("\\d{10}") || !phoneStr.startsWith("05")) {
                showMessage("Phone must start with '05' and be exactly 10 digits.", "red");
                return;
            }

            try {
                api.createGuestByPhone(phoneStr);
                showMessage("Creating guest...", "blue");
            } catch (IOException e) {
                showMessage("Connection error.", "red");
            }
        }
    }

    /* ===================== SERVER ===================== */

    /**
     * Handles async responses for "Act as" requests (find user / create guest).
     * <p>
     * On success with a {@link User} payload, updates {@link ClientSession}'s acting user.
     * </p>
     *
     * @param response server response wrapper
     */
    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {
            if (response == null) return;

            if (!response.isSuccess()) {
                showMessage(response.getMessage(), "red");
                return;
            }

            // Success payload for find/create user -> update acting user
            if (response.getData() instanceof User u) {
                ClientSession.setActingUser(u);
                clearOkFields();
                updateActingUserLabel();
                showMessage("Acting user updated.", "blue");
                return;
            }

            hideMessage();
        });
    }

    /**
     * Called when a connection error occurs while this controller is active.
     *
     * @param e the connection exception
     */
    @Override
    public void handleConnectionError(Exception e) {
        Platform.runLater(() -> showMessage("Connection error.", "red"));
    }

    /**
     * Called when the connection is closed while this controller is active.
     * Shows a message to the user.
     */
    @Override
    public void handleConnectionClosed() {
        Platform.runLater(() -> showMessage("Connection closed.", "red"));
    }

    /* ===================== UI HELPERS ===================== */

    /**
     * Updates the acting user label based on {@link ClientSession#getActingUser()}.
     * Displays user id and role, or a placeholder when no acting user is set.
     */
    private void updateActingUserLabel() {
        if (lblActingUser == null) return;
        User acting = ClientSession.getActingUser();
        lblActingUser.setText(
                acting == null
                        ? "Acting User: —"
                        : "Acting User: " + acting.getUserId() + " (" + acting.getUserRole() + ")"
        );
    }

    /**
     * Clears the staff "Act as" input fields.
     */
    private void clearOkFields() {
        if (txtIdForRegistered != null) txtIdForRegistered.clear();
        if (txtPhoneForNewGuest != null) txtPhoneForNewGuest.clear();
    }

    /**
     * Shows a message label with a specified text color.
     *
     * @param msg   message to show
     * @param color JavaFX CSS color string (e.g., "red", "blue")
     */
    private void showMessage(String msg, String color) {
        if (lblMessage == null) return;
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: " + color + ";");
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    /**
     * Hides the message label and removes it from layout calculations.
     */
    private void hideMessage() {
        if (lblMessage == null) return;
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
    }
}
