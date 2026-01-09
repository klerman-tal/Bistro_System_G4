package application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import guiControllers.Login_BController;
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
    // שונה ל-public כדי שיהיה נגיש בקלות מכל מקום כ- ClientUI.client
    public static ChatClient client; 
    private static ChatIF activeController;

    public static void setActiveController(ChatIF controller) {
        activeController = controller;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        TextInputDialog dialog = new TextInputDialog("localhost");
        dialog.setTitle("Server IP Required");
        dialog.setHeaderText("Enter Bistro server IP");
        dialog.setContentText("Server IP:");

        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent() || result.get().trim().isEmpty()) {
            Platform.exit();
            return;
        }

        String host = result.get().trim();

        try {
            // אתחול הלקוח
            client = new ChatClient(host, DEFAULT_PORT, this);
        } catch (IOException e) {
            e.printStackTrace();
            Platform.exit();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Login_B.fxml"));
            Parent root = loader.load();
            primaryStage.setTitle("Bistro Management System");
            primaryStage.setScene(new Scene(root));
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void display(String message) {
        if (activeController != null) {
            Platform.runLater(() -> activeController.display(message));
        }
    }

    @Override
    public void sendToServer(ArrayList<String> msg) {
        if (client != null) client.handleMessageFromClientUI(msg);
    }

    @Override
    public void stop() throws Exception {
        if (client != null) {
            client.closeConnection();
        }
        super.stop();
    }

    public static void main(String[] args) { launch(args); }
    @Override public boolean loginGuest(String p, String e) { return false; }
    @Override public boolean loginSubscriber(int id, String u) { return false; }
}