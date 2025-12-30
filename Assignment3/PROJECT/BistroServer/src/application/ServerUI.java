package application;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.util.Optional;

import dbControllers.DBController;
import gui.ServerGUIController;

public class ServerUI extends Application {
	//Test push to dev
    private static ServerGUIController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {

        // ===== Step 1: Request MySQL password from user =====
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("MySQL Password Required");
        dialog.setHeaderText("Enter your MySQL password");
        dialog.setContentText("Password:");

        Optional<String> result = dialog.showAndWait();
        String mysqlPassword = result.orElse("");

        // Store the password inside DBController for later database initialization
        DBController.MYSQL_PASSWORD = mysqlPassword;

        // ===== Step 2: Load and display the GUI =====
        // Load FXML layout and get its controller
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ServerGUI.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        primaryStage.setTitle("Restaurant Server Log");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        // ===== Step 3: Start the server in a separate thread =====
        // Prevent blocking the JavaFX UI thread
        new Thread(() -> {
            RestaurantServer server = new RestaurantServer(5556);
            server.setUiController(controller);
            try {
                server.listen(); // Start listening to client connections
            } catch (Exception e) {
                controller.addLog("ERROR: " + e.getMessage());
            }
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
