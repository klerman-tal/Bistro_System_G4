package application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import guiControllers.Login_BController;
import guiControllers.MainGUI;
import interfaces.ChatIF;
import interfaces.ClientActions;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

public class ClientUI extends Application implements ChatIF, ClientActions {

    public static final int DEFAULT_PORT = 5556;

    private static ChatClient client;
    private MainGUI mainController; // אפשר להשאיר - לא חובה

    // NEW: מי המסך הפעיל שמקבל הודעות מהשרת
    private static ChatIF activeController;

    public static void setActiveController(ChatIF controller) {
        activeController = controller;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        // ===== 1) בקשת IP מהמשתמש =====
        TextInputDialog dialog = new TextInputDialog("");
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

        // ===== 2) חיבור לשרת =====
        try {
            client = new ChatClient(host, DEFAULT_PORT, this);
        } catch (IOException e) {
            System.out.println("Error: Can't setup connection! Terminating client.");
            e.printStackTrace();
            Platform.exit();
            return;
        }

        // ===== 3) טעינת Login =====
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Login_B.fxml"));
            Parent root = loader.load();
            Login_BController loginController = loader.getController();
            loginController.setClientActions(this);

            // אם בלוגין יש setClientActions - מומלץ להעביר (אם אין כרגע, לא חובה)
            // Object c = loader.getController();
            // if (c instanceof Login_BController) {
            //     ((Login_BController) c).setClientActions(this);
            // }

            primaryStage.setTitle("Bistro Client – Login");
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
        } catch (IOException e) {
            System.out.println("Error: Can't load Login_B.fxml");
            e.printStackTrace();
            Platform.exit();
        }
    }

    // ---- הודעות מהשרת ----
    @Override
    public void display(String message) {
        System.out.println("> " + message);

        // מסך פעיל מקבל הודעה
        if (activeController != null) {
            Platform.runLater(() -> activeController.display(message));
        }

        // אם את משתמשת ב-mainController (לא חובה)
        if (mainController != null) {
            Platform.runLater(() -> mainController.displayMessageFromServer(message));
        }
    }

    // ---- שליחה לשרת ----
    @Override
    public void sendToServer(ArrayList<String> msg) {
        if (client != null) {
            client.handleMessageFromClientUI(msg);
        }
    }

    // ---- סגירה ----
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

    public static void main(String[] args) {
        launch(args);
    }
} 
