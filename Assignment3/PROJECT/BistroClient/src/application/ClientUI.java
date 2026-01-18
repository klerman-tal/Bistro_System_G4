package application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import interfaces.ChatIF;
import interfaces.ClientActions;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import javafx.scene.image.Image;

/**
 * Main JavaFX entry point for the Bistro client application.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Bootstraps the JavaFX application and initializes the primary {@link Stage}.</li>
 *   <li>Prompts the user for the server host/IP, then creates a {@link ChatClient} connection.</li>
 *   <li>Loads the initial FXML screen and ensures the app uses a single {@link Stage} and a single {@link Scene}.</li>
 *   <li>Provides a centralized navigation helper ({@link #switchTo(String)}) that swaps only the scene root.</li>
 *   <li>Routes incoming text/log messages to the currently active controller implementing {@link ChatIF}.</li>
 * </ul>
 * </p>
 */
public class ClientUI extends Application implements ChatIF, ClientActions {

    /** Default TCP port used by the Bistro server. */
    public static final int DEFAULT_PORT = 5556;

    /** Shared client instance used by controllers to communicate with the server. */
    public static ChatClient client;

    /** The currently active controller that receives text output via {@link ChatIF#display(String)}. */
    private static ChatIF activeController;

    /**
     * The single primary stage for the entire application.
     * <p>
     * Navigation in this app is implemented by swapping the root node of the existing scene
     * rather than creating new stages/scenes.
     * </p>
     */
    public static Stage primaryStage;

    /**
     * Sets the controller that will receive messages displayed by {@link #display(String)}.
     *
     * @param controller a controller implementing {@link ChatIF}
     */
    public static void setActiveController(ChatIF controller) {
        activeController = controller;
    }

    /**
     * JavaFX lifecycle method called on application startup.
     * <p>
     * Prompts for server host, initializes {@link ChatClient}, loads the initial FXML,
     * and applies consistent window settings (icon, title, min size, maximized).
     * </p>
     *
     * @param stage the primary stage provided by JavaFX
     * @throws Exception if JavaFX initialization fails
     */
    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Prompt user for server host/IP before starting the client connection.
        TextInputDialog dialog = new TextInputDialog("localhost");
        dialog.setTitle("Server IP Required");
        dialog.setHeaderText("Enter Bistro server IP");
        dialog.setContentText("Server IP:");

        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent() || result.get().trim().isEmpty()) {
            // User cancelled or provided empty input -> exit cleanly.
            Platform.exit();
            return;
        }

        // Host/IP to connect to (trimmed user input).
//test
        String host = result.get().trim();

        // Create and connect the network client (opens connection in constructor).
        try {
            client = new ChatClient(host, DEFAULT_PORT, this);
        } catch (IOException e) {
            // Connection failed -> exit application.
            e.printStackTrace();
            Platform.exit();
            return;
        }

        // Load the first screen once and create a single Scene for the app.
        Parent root = loadFxmlAndBindController("/gui/Login_B.fxml");

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);

        // Apply consistent window configuration for all screens.
        primaryStage.setTitle("Bistro Management System");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/gui/BistroLogo.PNG")));
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(700);
        primaryStage.setMaximized(true);

        primaryStage.show();
    }

    /**
     * Centralized navigation helper that switches the displayed screen.
     * <p>
     * Rule: reuse the existing {@link Scene} and replace only the root node.
     * This prevents issues caused by creating multiple scenes/stages and keeps UI consistent.
     * </p>
     *
     * @param fxmlPath absolute path to the FXML resource (e.g., "/gui/Menu_B.fxml")
     */
    public static void switchTo(String fxmlPath) {
        try {
            Parent root = loadFxmlAndBindController(fxmlPath);

            Scene scene = primaryStage.getScene();
            if (scene == null) {
                // Fallback safety: create a scene only if none exists.
                primaryStage.setScene(new Scene(root));
            } else {
                // Preferred: swap root only.
                scene.setRoot(root);
            }

            // Ensure the window remains maximized after navigation.
            primaryStage.setMaximized(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads an FXML file and, if the controller implements {@link ChatIF},
     * registers it as the active controller for message display.
     *
     * @param fxmlPath absolute path to the FXML resource
     * @return the loaded root node
     * @throws IOException if the FXML resource cannot be loaded
     */
    private static Parent loadFxmlAndBindController(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(ClientUI.class.getResource(fxmlPath));
        Parent root = loader.load();

        Object controller = loader.getController();
        if (controller instanceof ChatIF chatIF) {
            setActiveController(chatIF);
        }

        return root;
    }

    /**
     * Displays a message to the currently active controller (if set).
     * <p>
     * Runs on the JavaFX Application Thread via {@link Platform#runLater(Runnable)}.
     * </p>
     *
     * @param message text to display
     */
    @Override
    public void display(String message) {
        if (activeController != null) {
            Platform.runLater(() -> activeController.display(message));
        }
    }

    /**
     * Sends a message to the server through the {@link ChatClient}.
     *
     * @param msg list of message parts to send
     */
    @Override
    public void sendToServer(ArrayList<String> msg) {
        if (client != null) client.handleMessageFromClientUI(msg);
    }

    /**
     * JavaFX lifecycle method called when the application is stopping.
     * Ensures the network connection is closed before exit.
     *
     * @throws Exception if an error occurs during shutdown
     */
    @Override
    public void stop() throws Exception {
        if (client != null) {
            client.closeConnection();
        }
        super.stop();
    }

    /**
     * Standard Java entry point that launches the JavaFX application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) { launch(args); }

    /**
     * Unused interface method (implemented only because {@link ClientActions} is implemented here).
     * @return always false in this implementation
     */
    @Override public boolean loginGuest(String p, String e) { return false; }

    /**
     * Unused interface method (implemented only because {@link ClientActions} is implemented here).
     * @return always false in this implementation
     */
    @Override public boolean loginSubscriber(int id, String u) { return false; }
}
