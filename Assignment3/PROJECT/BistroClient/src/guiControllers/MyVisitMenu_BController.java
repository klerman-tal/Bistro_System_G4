package guiControllers;

import java.io.IOException;

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
 * Controller for the "My Visit" menu screen.
 * <p>
 * This screen is intended for customers during an active/ongoing visit and provides
 * quick access to visit-related actions (e.g., getting a table, making a payment),
 * and a way to navigate back to the main menu.
 * </p>
 *
 * <h3>Main responsibilities</h3>
 * <ul>
 *   <li>Receives and stores the session context: {@link User} and {@link ChatClient}.</li>
 *   <li>Optionally receives {@link ClientActions} and forwards it to other screens that support it.</li>
 *   <li>Handles button actions for:
 *     <ul>
 *       <li>Get Table flow</li>
 *       <li>Payment flow</li>
 *       <li>Back to Main Menu</li>
 *     </ul>
 *   </li>
 *   <li>Navigates by reusing the existing {@link Scene} and replacing only the root node.</li>
 * </ul>
 *
 * <p><b>Navigation rule:</b> This controller does not create new stages; it uses the existing stage
 * and swaps the root to keep one consistent full-screen window across the app.</p>
 */
public class MyVisitMenu_BController {

    /** Current user (customer / acting user) operating this screen. */
    private User user;

    /** Active client connection to the server. */
    private ChatClient chatClient;

    /** Optional client actions implementation (passed through if supported by target controllers). */
    private ClientActions clientActions;

    @FXML private BorderPane rootPane;

    // ================= SESSION =================

    /**
     * Injects the session context into this controller.
     *
     * @param user       the current user for this screen
     * @param chatClient the active client connection
     */
    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
    }

    /**
     * Injects {@link ClientActions} into this controller.
     * This is optional and is forwarded to other controllers that expose a
     * <code>setClientActions(ClientActions)</code> method.
     *
     * @param clientActions client actions implementation
     */
    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    // ================= ACTIONS =================

    /**
     * Opens the "Get Table" flow screen.
     */
    @FXML
    private void onGetTable() {
        openWindow("GetTableChoice_B.fxml", "Get Table");
    }

    /**
     * Opens the payment screen.
     */
    @FXML
    private void onPayment() {
        openWindow("Payment_B.fxml", "Payment");
    }

    /**
     * Navigates back to the main menu.
     */
    @FXML
    private void onBack() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/Menu_B.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();

            // ✅ ALWAYS pass the real logged-in user to Menu
            User logged = application.ClientSession.getLoggedInUser();

            if (controller != null && logged != null && chatClient != null) {
                try {
                    controller.getClass()
                            .getMethod("setClient", User.class, ChatClient.class)
                            .invoke(controller, logged, chatClient);
                } catch (Exception ignored) {}
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = stage.getScene();

            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }

            stage.setTitle("Bistro - Menu");
            stage.setMaximized(true);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // ================= NAVIGATION =================

    /**
     * Loads an FXML screen and navigates to it by replacing the current scene root.
     * <p>
     * Injection behavior (best-effort via reflection):
     * <ul>
     *   <li>If the target controller has <code>setClientActions(ClientActions)</code>,
     *       passes the current {@link #clientActions}.</li>
     *   <li>If the target controller has <code>setClient(User, ChatClient)</code>,
     *       passes the current {@link #user} and {@link #chatClient}.</li>
     * </ul>
     * </p>
     *
     * @param fxmlName FXML file name under <code>/gui/</code>
     * @param title    window title suffix
     */
    private void openWindow(String fxmlName, String title) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
            Parent root = loader.load();

            Object controller = loader.getController();

            // pass clientActions if exists
            if (controller != null && clientActions != null) {
                try {
                    controller.getClass()
                            .getMethod("setClientActions", ClientActions.class)
                            .invoke(controller, clientActions);
                } catch (Exception ignored) {}
            }

            // preserve user + chatClient
            if (controller != null && user != null && chatClient != null) {
                try {
                    controller.getClass()
                            .getMethod("setClient", User.class, ChatClient.class)
                            .invoke(controller, user, chatClient);
                } catch (Exception ignored) {}
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = stage.getScene();

            // ✅ Rule: reuse Scene, replace root
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }

            stage.setTitle("Bistro - " + title);
            stage.setMaximized(true);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
