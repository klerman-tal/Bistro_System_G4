package guiControllers;

import application.ChatClient;
import entities.User;
import interfaces.ClientActions;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * JavaFX controller that lets a management user choose which user-management screen to open.
 *
 * <p>This screen provides navigation to the subscribers management screen and the guests
 * management screen, while preserving the current session ({@link User} and {@link ChatClient})
 * and optionally forwarding {@link ClientActions} to the next controller.</p>
 */
public class SelectUser_BController {

    @FXML
    private BorderPane rootPane;

    private ClientActions clientActions;

    private User user;
    private ChatClient chatClient;

    /**
     * Injects a {@link ClientActions} instance that can be forwarded to subsequent screens.
     *
     * @param clientActions the client actions interface implementation
     */
    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    /**
     * Injects the current session context for navigation.
     *
     * @param user       the current logged-in user
     * @param chatClient the active client connection to the server
     */
    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
    }

    /**
     * Opens the subscribers management screen.
     */
    @FXML
    private void onSubscribersClicked() {
        openWindow("manageSubscriber.fxml", "Manage Subscribers");
    }

    /**
     * Opens the guests management screen.
     */
    @FXML
    private void onGuestsClicked() {
        openWindow("manegeGustsGui.fxml", "Manage Guests");
    }

    /**
     * Navigates back to the restaurant management main screen.
     */
    @FXML
    private void onBackToMenuClicked() {
        openWindow("RestaurantManagement_B.fxml", "Restaurant Management");
    }

    /**
     * Loads an FXML screen from {@code /gui/}, injects the session context and/or {@link ClientActions}
     * if supported, and navigates by swapping the current scene root.
     *
     * @param fxmlName the FXML file name under {@code /gui/}
     * @param title    the window title suffix
     */
    private void openWindow(String fxmlName, String title) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
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

            switchRoot(root, "Bistro - " + title);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Switches the current scene root to the given root node while preserving the existing {@link Scene}.
     *
     * @param root  the new root node to display
     * @param title the window title to set
     */
    private void switchRoot(Parent root, String title) {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        Scene scene = stage.getScene();

        if (scene == null) {
            stage.setScene(new Scene(root));
        } else {
            scene.setRoot(root);
        }

        stage.setTitle(title);
        stage.setMaximized(true);
        stage.show();
    }
}
