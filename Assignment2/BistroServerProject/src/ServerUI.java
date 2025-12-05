import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import gui.ServerGUIController;

public class ServerUI extends Application {

    private static ServerGUIController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ServerGUI.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        primaryStage.setTitle("Restaurant Server Log");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        // הפעלת השרת ב-thread נפרד
        new Thread(() -> {
            RestaurantServer server = new RestaurantServer(5556);
            server.setUiController(controller);
            try {
                server.listen();
            } catch (Exception e) {
                controller.addLog("ERROR: " + e.getMessage());
            }
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
