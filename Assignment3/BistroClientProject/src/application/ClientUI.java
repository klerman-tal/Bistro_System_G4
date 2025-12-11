package application;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import guiControllers.MainGUI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import interfaces.ChatIF;
import interfaces.ClientActions;

// ClientUI class handles the client-side UI and connection logic.
public class ClientUI extends Application implements ChatIF, ClientActions {

    public static final int DEFAULT_PORT = 5556;

    private static ChatClient client;    // Reference to the OCSF client object
    private MainGUI mainController;      // Reference to the main GUI controller (כרגע לא בשימוש)

    @Override
    public void start(Stage primaryStage) throws Exception {

        // ===== 1. בקשת IP מהמשתמש =====
        TextInputDialog dialog = new TextInputDialog("10.0.0.15"); // אפשר לשים "localhost" כברירת מחדל
        dialog.setTitle("Server IP Required");
        dialog.setHeaderText("Enter Bistro server IP");
        dialog.setContentText("Server IP:");

        Optional<String> result = dialog.showAndWait();

        if (!result.isPresent() || result.get().trim().isEmpty()) {
            System.out.println("No IP entered. Exiting client.");
            Platform.exit();
            return;
        }

        String host = result.get().trim();

        // ===== 2. יצירת חיבור לשרת עם ה-IP שהמשתמש הכניס =====
        try {
            // Initialize the OCSF ChatClient connection
            client = new ChatClient(host, DEFAULT_PORT, this);
        } catch (IOException e) {
            System.out.println("Error: Can't setup connection! Terminating client.");
            e.printStackTrace();
            Platform.exit();
            return;
        }

        // ===== 3. טעינת מסך ה-LOGIN במקום GUI.fxml =====
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Login_B.fxml")); // *** CHANGED ***
            Parent root = loader.load();

            // אם תרצי בעתיד להעביר clientActions ללוגין:
            // Login_BController loginController = loader.getController();
            // loginController.setClientActions(this);

            primaryStage.setTitle("Bistro Client – Login"); // *** CHANGED ***
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
        } catch (IOException e) {
            System.out.println("Error: Can't load Login_B.fxml");
            e.printStackTrace();
            Platform.exit();
        }

        // ===== 4. כרגע לא מבקשים PRINT_ORDERS =====
        // את הלוגיקה של ההזמנות תשאירי למסכים החדשים
    }

    // ---- Implementation of ChatIF: Messages from the server ----
    @Override
    public void display(String message) {
        System.out.println("> " + message);

        if (mainController != null) {
            // Update the UI on the JavaFX application thread
            Platform.runLater(() -> mainController.displayMessageFromServer(message));
        }
    }

    // ---- Implementation of ClientActions: Sending to server from the GUI ----
    @Override
    public void sendToServer(ArrayList<String> msg) {
        // Pass the message to the OCSF client handler
        if (client != null) {
            client.handleMessageFromClientUI(msg);
        }
    }

    // ---- Called when the JavaFX window is closed ----
    @Override
    public void stop() throws Exception {
        System.out.println("Closing client connection...");

        if (client != null) {
            try {
                ArrayList<String> logoutMsg = new ArrayList<>();
                logoutMsg.add("CLIENT_LOGOUT");
                client.handleMessageFromClientUI(logoutMsg);

                client.closeConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        super.stop();
    }

    // Main method to launch the JavaFX application
    public static void main(String[] args) {
        launch(args);
    }
}
