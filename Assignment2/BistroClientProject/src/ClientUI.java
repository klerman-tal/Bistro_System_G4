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

public class ClientUI extends Application implements ChatIF, ClientActions {

    public static final int DEFAULT_PORT = 5556;

    private static ChatClient client;   // ChatClient שלך מה-default package
    private MainGUI mainController;

    @Override
    public void start(Stage primaryStage) throws Exception {

        String host = "localhost";

        try {
            client = new ChatClient(host, DEFAULT_PORT, this);
        } catch (IOException e) {
            System.out.println("Error: Can't setup connection! Terminating client.");
            System.exit(1);
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/GUI.fxml"));
        Parent root = loader.load();

        mainController = loader.getController();
        mainController.setClientActions(this);

        primaryStage.setTitle("Bistro Client – Orders Management");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        // ברגע שהחלון עולה – לבקש מהשרת את כל ההזמנות
        ArrayList<String> msg = new ArrayList<>();
        msg.add("PRINT_ORDERS");
        sendToServer(msg);
    }

    // ---- ChatIF: הודעות מהשרת ----
    @Override
    public void display(String message) {
        System.out.println("> " + message);

        if (mainController != null) {
            Platform.runLater(() -> mainController.displayMessageFromServer(message));
        }
    }

    // ---- ClientActions: שליחה לשרת מה-GUI ----
    @Override
    public void sendToServer(ArrayList<String> msg) {
        if (client != null) {
            client.handleMessageFromClientUI(msg);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
