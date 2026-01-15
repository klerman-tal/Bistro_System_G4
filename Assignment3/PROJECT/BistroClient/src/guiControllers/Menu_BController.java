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

public class Menu_BController implements ClientResponseHandler {

    @FXML private BorderPane rootPane;

    @FXML private Button btnPersonalDetails;
    @FXML private Button btnRestaurantManagement;

    // ✅ Acting box container (was missing -> stayed hidden forever)
    @FXML private BorderPane actAsBox;

    // ✅ Acting user controls
    @FXML private TextField txtIdForRegistered;
    @FXML private TextField txtPhoneForNewGuest;
    @FXML private Button btnOk;
    @FXML private Label lblActingUser;

    @FXML private Label lblMessage;

    private User loggedInUser;
    private ChatClient chatClient;
    private ClientActions clientActions;
    private ClientAPI api;

    /* =======================
       SETTERS
       ======================= */

    public void setClient(User user, ChatClient chatClient) {
        this.loggedInUser = user;
        this.chatClient = chatClient;
        this.api = new ClientAPI(chatClient);

        // keep session
        ClientSession.setLoggedInUser(user);
        ClientSession.setChatClient(chatClient);
        ClientSession.resetActingUser();

        // menu can receive OK responses
        if (this.chatClient != null) {
            this.chatClient.setResponseHandler(this);
        }

        boolean isRestaurantStaff =
                user != null &&
                (user.getUserRole() == Enums.UserRole.RestaurantAgent
              || user.getUserRole() == Enums.UserRole.RestaurantManager);

        // ✅ Restaurant Management visible ONLY for Agent/Manager
        btnRestaurantManagement.setVisible(isRestaurantStaff);
        btnRestaurantManagement.setManaged(isRestaurantStaff);

        // Personal Details hidden for RandomClient
        boolean showPersonal =
                user != null && user.getUserRole() != Enums.UserRole.RandomClient;

        btnPersonalDetails.setVisible(showPersonal);
        btnPersonalDetails.setManaged(showPersonal);

        // ✅ Acting controls visible ONLY for staff
        setActingControlsVisible(isRestaurantStaff);

        clearOkFields();
        updateActingUserLabel();
        hideMessage();
    }

    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    private void setActingControlsVisible(boolean visible) {
        // ✅ THIS is what you were missing
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

    /* =======================
       OK (Act as)
       ======================= */

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

    /* =======================
       BUTTON ACTIONS
       ======================= */

    @FXML
    private void onSelectReservationsClicked() {
        openWindow("ReservationMenu_B.fxml", "Reservations", false);
    }

    @FXML
    private void onSelectMyVisitClicked() {
        openWindow("MyVisitMenu_B.fxml", "My Visit", false);
    }

    @FXML
    private void onSelectPersonalDetailsClicked() {
        openWindow("ClientDetails_B.fxml", "Personal Details", false);
    }

    @FXML
    private void onSelectRestaurantManagementClicked() {
        openWindow("RestaurantManagement_B.fxml", "Restaurant Management", true);
    }

    @FXML
    private void onLogoutClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Login_B.fxml"));
            Parent root = loader.load();

            ClientSession.setLoggedInUser(null);
            ClientSession.setActingUser(null);

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle("Bistro - Login");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* =======================
       NAVIGATION
       ======================= */

    private void openWindow(String fxmlName, String title, boolean managementScreen) {
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

            User userToPass = managementScreen
                    ? ClientSession.getLoggedInUser()
                    : ClientSession.getActingUser();

            if (controller != null && userToPass != null && chatClient != null) {
                try {
                    controller.getClass()
                            .getMethod("setClient", User.class, ChatClient.class)
                            .invoke(controller, userToPass, chatClient);
                } catch (Exception ignored) {}
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle("Bistro - " + title);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* =======================
       SERVER RESPONSE
       ======================= */

    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {
            if (response == null) return;

            if (!response.isSuccess()) {
                showMessage(response.getMessage(), "red");
                return;
            }

            // OK flows return a User
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

    @Override public void handleConnectionError(Exception e) {
        Platform.runLater(() -> showMessage("Connection error.", "red"));
    }

    @Override public void handleConnectionClosed() {
        Platform.runLater(() -> showMessage("Connection closed.", "red"));
    }

    /* =======================
       UI HELPERS
       ======================= */

    private void updateActingUserLabel() {
        if (lblActingUser == null) return;

        User acting = ClientSession.getActingUser();
        if (acting == null) {
            lblActingUser.setText("Acting User: —");
            return;
        }

        lblActingUser.setText(
                "Acting User: " + acting.getUserId() + " (" + acting.getUserRole() + ")"
        );
    }

    private void clearOkFields() {
        if (txtIdForRegistered != null) txtIdForRegistered.clear();
        if (txtPhoneForNewGuest != null) txtPhoneForNewGuest.clear();
    }

    private void showMessage(String msg, String color) {
        if (lblMessage == null) return;
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: " + color + ";");
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    private void hideMessage() {
        if (lblMessage == null) return;
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
    }
}
