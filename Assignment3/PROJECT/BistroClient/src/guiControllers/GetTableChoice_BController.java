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
 * JavaFX controller for choosing a table flow source.
 *
 * <p>This screen acts as a navigation hub that lets the user choose whether to
 * proceed to a table-selection flow starting from an existing reservation or from
 * the waiting list. It preserves the current session context by passing
 * {@link User}, {@link ChatClient}, and (optionally) {@link ClientActions} to the
 * next screen controllers.</p>
 */
public class GetTableChoice_BController {

    @FXML private BorderPane rootPane;

    private User user;
    private ChatClient chatClient;
    private ClientActions clientActions;

    /**
     * Injects the current session context into this controller.
     *
     * @param user       the current logged-in user
     * @param chatClient the network client used to communicate with the server
     */
    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
    }

    /**
     * Injects a {@link ClientActions} implementation for controllers that rely on GUI-to-client actions.
     *
     * @param clientActions the client actions bridge used by downstream controllers
     */
    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    /**
     * Navigates to the "Get Table From Reservation" flow.
     */
    @FXML
    private void onFromReservationClicked() {
        openWindow("GetTableFromReservation_B.fxml", "Get Table (Reservation)");
    }

    /**
     * Navigates to the "Get Table From Waiting" flow.
     */
    @FXML
    private void onFromWaitingClicked() {
        openWindow("GetTableFromWaiting_B.fxml", "Get Table (Waiting)");
    }

    /**
     * Navigates back to the main visit menu.
     */
    @FXML
    private void onBackClicked() {
        openWindow("MyVisitMenu_B.fxml", "Main Menu");
    }

    /**
     * Loads the requested FXML and swaps the current scene root to navigate between screens.
     *
     * <p>If the target controller defines {@code setClientActions(ClientActions)} and/or
     * {@code setClient(User, ChatClient)}, these will be invoked reflectively to preserve
     * session context.</p>
     *
     * @param fxmlName the target FXML file name under {@code /gui/}
     * @param title    the window title suffix to display
     */
    private void openWindow(String fxmlName, String title) {
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

            if (controller != null && user != null && chatClient != null) {
                try {
                    controller.getClass()
                            .getMethod("setClient", User.class, ChatClient.class)
                            .invoke(controller, user, chatClient);
                } catch (Exception ignored) {}
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle("Bistro - " + title);

            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }

            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
