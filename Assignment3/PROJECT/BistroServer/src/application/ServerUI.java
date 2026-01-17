package application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.util.Optional;

import dbControllers.DBController;
import gui.ServerGUIController;

/**
 * JavaFX entry point for the restaurant server UI.
 *
 * <p>This application:
 * prompts the user for the MySQL password, stores it in {@link DBController},
 * loads the server GUI, and starts {@link RestaurantServer} on a background thread
 * so the JavaFX UI remains responsive.</p>
 *
 * <p>The server may auto-shutdown after an idle period (as configured in the server),
 * and in that case the UI is closed automatically.</p>
 */
public class ServerUI extends Application {

    private static ServerGUIController controller;

    /**
     * Initializes and displays the server UI, then starts the server in a separate thread.
     *
     * @param primaryStage the main JavaFX stage provided by the runtime
     * @throws Exception if the FXML cannot be loaded or JavaFX initialization fails
     */
    @Override
    public void start(Stage primaryStage) throws Exception {

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("MySQL Password Required");
        dialog.setHeaderText("Enter your MySQL password");
        dialog.setContentText("Password:");

        Optional<String> result = dialog.showAndWait();
        String mysqlPassword = result.orElse("");

        DBController.MYSQL_PASSWORD = mysqlPassword;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ServerGUI.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        primaryStage.setTitle("Restaurant Server Log");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        new Thread(() -> {
            RestaurantServer server = new RestaurantServer(5556);
            server.setUiController(controller);

            server.setOnAutoShutdown(() -> Platform.runLater(() -> {
                try {
                    primaryStage.close();
                } catch (Exception ignored) {}
                Platform.exit();
            }));

            try {
                server.listen();
            } catch (Exception e) {
                controller.addLog("ERROR: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Launches the JavaFX application.
     *
     * @param args command-line arguments passed to the JavaFX runtime
     */
    public static void main(String[] args) {
        launch(args);
    }
}
