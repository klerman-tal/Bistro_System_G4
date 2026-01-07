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
        TextInputDialog dialog = new TextInputDialog("10.0.0.15");
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
            // שימוש בנתיב מוחלט מה-Root
            var resource = getClass().getResource("/gui/Login_B.fxml");
            
            if (resource == null) {
                throw new RuntimeException("Fatal Error: Could not find /gui/Login_B.fxml. Check your folder structure!");
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            // הגדרת הסצנה
            Scene scene = new Scene(root);
            primaryStage.setTitle("Bistro Management System");
            primaryStage.setScene(scene);
            
            primaryStage.centerOnScreen();
            primaryStage.show();
            
            System.out.println("Login_B screen loaded successfully.");
            
        } catch (Exception e) {
            System.err.println("Error in start method: " + e.getMessage());
            e.printStackTrace();
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

	@Override
	public boolean loginGuest(String phone, String email) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean loginSubscriber(int subscriberId, String username) {
		// TODO Auto-generated method stub
		return false;
	}
} 