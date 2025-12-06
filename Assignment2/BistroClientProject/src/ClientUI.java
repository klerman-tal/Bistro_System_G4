import java.io.IOException;
import java.util.ArrayList;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import common.ChatIF;
import common.ClientActions;
import gui.MainGUI;

// ClientUI class handles the client-side UI and connection logic.
public class ClientUI extends Application implements ChatIF, ClientActions {

    public static final int DEFAULT_PORT = 5556;

    private static ChatClient client;    // Reference to the OCSF client object
    private MainGUI mainController;      // Reference to the main GUI controller

    @Override
    public void start(Stage primaryStage) throws Exception {

        String host = "10.0.0.15"; // Default server host

        try {
            // Initialize the OCSF ChatClient connection
            client = new ChatClient(host, DEFAULT_PORT, this);
        } catch (IOException e) {
            System.out.println("Error: Can't setup connection! Terminating client.");
            System.exit(1);
        }

        // Load the FXML file for the main UI
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/GUI.fxml"));
        Parent root = loader.load();

        // Get the controller and set the client actions interface
        mainController = loader.getController();
        mainController.setClientActions(this);

        primaryStage.setTitle("Bistro Client – Orders Management");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        // Request all orders from the server immediately after the window loads
        ArrayList<String> msg = new ArrayList<>();
        msg.add("PRINT_ORDERS");
        sendToServer(msg);
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
            }
        }

        super.stop();
    }

    // Main method to launch the JavaFX application
    public static void main(String[] args) {
        launch(args);
    }
}
