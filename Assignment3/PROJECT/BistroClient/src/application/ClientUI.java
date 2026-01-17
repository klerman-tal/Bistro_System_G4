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

public class ClientUI extends Application implements ChatIF, ClientActions {

    public static final int DEFAULT_PORT = 5556;

    public static ChatClient client;
    private static ChatIF activeController;

    // ✅ Stage אחד לכל האפליקציה
    public static Stage primaryStage;

    public static void setActiveController(ChatIF controller) {
        activeController = controller;
    }

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        TextInputDialog dialog = new TextInputDialog("localhost");
        dialog.setTitle("Server IP Required");
        dialog.setHeaderText("Enter Bistro server IP");
        dialog.setContentText("Server IP:");

        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent() || result.get().trim().isEmpty()) {
            Platform.exit();
            return;
        }
//test
        String host = result.get().trim();

        try {
            client = new ChatClient(host, DEFAULT_PORT, this);
        } catch (IOException e) {
            e.printStackTrace();
            Platform.exit();
            return;
        }

        // ✅ נטען מסך ראשון פעם אחת, עם Scene אחת
        Parent root = loadFxmlAndBindController("/gui/Login_B.fxml");

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);

        // ✅ סטנדרט תצוגה אחיד לכל המסכים
        primaryStage.setTitle("Bistro Management System");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/gui/BistroLogo.PNG")));
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(700);
        primaryStage.setMaximized(true);

        primaryStage.show();
    }

    // ✅ מעבר מסך אחיד: מחליפים Root בלבד (בלי new Stage / בלי new Scene)
    public static void switchTo(String fxmlPath) {
        try {
            Parent root = loadFxmlAndBindController(fxmlPath);

            Scene scene = primaryStage.getScene();
            if (scene == null) {
                primaryStage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }

            // שומר על Maximized גם אם מישהו שיחק עם החלון
            primaryStage.setMaximized(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Parent loadFxmlAndBindController(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(ClientUI.class.getResource(fxmlPath));
        Parent root = loader.load();

        Object controller = loader.getController();
        if (controller instanceof ChatIF chatIF) {
            setActiveController(chatIF);
        }

        return root;
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
