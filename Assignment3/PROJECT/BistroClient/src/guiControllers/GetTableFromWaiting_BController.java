package guiControllers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

import application.ChatClient;
import dto.ResponseDTO;
import entities.Enums.UserRole;
import entities.User;
import entities.Waiting;
import interfaces.ClientActions;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

public class GetTableFromWaiting_BController implements ClientResponseHandler {

    @FXML private BorderPane rootPane;
    @FXML private TextField txtCode;
    @FXML private Button btnConfirm;
    @FXML private Label lblInfo;
    @FXML private Label lblError;

    // ✅ Table (visible for: Subscriber / Agent / Manager; hidden only for RandomClient)
    @FXML private VBox boxMyActive;
    @FXML private TableView<Waiting> tblMyActive;
    @FXML private TableColumn<Waiting, LocalDate> colDate;
    @FXML private TableColumn<Waiting, LocalTime> colTime;
    @FXML private TableColumn<Waiting, String> colCode;

    private final ObservableList<Waiting> myActiveWaitings = FXCollections.observableArrayList();

    private User user;
    private ChatClient chatClient;
    private ClientActions clientActions;
    private ClientAPI api;

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
        this.api = new ClientAPI(chatClient);

        this.chatClient.setResponseHandler(this);

        initMyActiveTable();
        loadMyActiveIfAllowed();
    }

    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    private void initMyActiveTable() {
        if (tblMyActive == null) return;

        colDate.setCellValueFactory(cd -> {
            if (cd.getValue() == null || cd.getValue().getTableFreedTime() == null) {
                return new SimpleObjectProperty<>(null);
            }
            return new SimpleObjectProperty<>(cd.getValue().getTableFreedTime().toLocalDate());
        });

        colTime.setCellValueFactory(cd -> {
            if (cd.getValue() == null || cd.getValue().getTableFreedTime() == null) {
                return new SimpleObjectProperty<>(null);
            }
            return new SimpleObjectProperty<>(cd.getValue().getTableFreedTime().toLocalTime());
        });

        colCode.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue() == null ? "" : cd.getValue().getConfirmationCode()));

        tblMyActive.setItems(myActiveWaitings);

        // ✅ click row → fill confirmation code
        tblMyActive.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected != null && selected.getConfirmationCode() != null) {
                txtCode.setText(selected.getConfirmationCode());
            }
        });
    }

    /**
     * ✅ SHOW TABLE FOR:
     * Subscriber / RestaurantAgent / RestaurantManager
     * ❌ HIDE ONLY FOR:
     * RandomClient
     */
    private void loadMyActiveIfAllowed() {
        boolean show =
                user != null &&
                user.getUserRole() != UserRole.RandomClient;

        if (boxMyActive != null) {
            boxMyActive.setVisible(show);
            boxMyActive.setManaged(show);
        }

        if (!show) return;

        try {
            api.getMyActiveWaitings(user.getUserId());
        } catch (Exception e) {
            // table is optional – do not break screen
            e.printStackTrace();
        }
    }

    @FXML
    private void onConfirmClicked() {
        hideMessages();

        if (chatClient == null || api == null) {
            showError("Session error. Please login again.");
            return;
        }

        String code = txtCode.getText() == null ? "" : txtCode.getText().trim();

        if (!code.matches("\\d{6}")) {
            showError("Please enter a valid 6-digit confirmation code.");
            return;
        }

        try {
            btnConfirm.setDisable(true);
            api.confirmWaitingArrival(code);
            showInfo("Request sent...");
        } catch (Exception e) {
            btnConfirm.setDisable(false);
            showError("Failed to send request to server.");
            e.printStackTrace();
        }
    }

    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {
            btnConfirm.setDisable(false);

            if (response == null) return;

            // ✅ Case 1: list for table
            if (response.isSuccess() && response.getData() instanceof ArrayList<?> list) {
                myActiveWaitings.clear();
                for (Object o : list) {
                    if (o instanceof Waiting w) {
                        myActiveWaitings.add(w);
                    }
                }
                return;
            }

            // ✅ Case 2: confirm arrival
            if (response.isSuccess()) {

                Waiting w = null;
                try { w = (Waiting) response.getData(); } catch (Exception ignored) {}

                String tableMsg = "";
                if (w != null && w.getTableNumber() != null) {
                    tableMsg = "Your table number is: " + w.getTableNumber();
                }

                showSuccessAlert(
                        "You are seated!",
                        "Welcome 🎉\n" + (tableMsg.isEmpty() ? "" : tableMsg)
                );

                showInfo(tableMsg.isEmpty() ? "You are seated." : tableMsg);
                return;
            }

            String msg = response.getMessage() != null
                    ? response.getMessage()
                    : "Arrival confirm failed (expired / not ready / not found)";

            showError(msg);
        });
    }

    @Override
    public void handleConnectionError(Exception e) {
        Platform.runLater(() -> showError("Connection lost."));
    }

    @Override
    public void handleConnectionClosed() {}

    @FXML
    private void onBackClicked() {
        if (chatClient != null) chatClient.setResponseHandler(null);
        openWindow("GetTableChoice_B.fxml", "Get Table");
    }

    private void openWindow(String fxmlName, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
            Parent root = loader.load();

            Object controller = loader.getController();

            if (controller != null && clientActions != null) {
                try {
                    controller.getClass()
                            .getMethod("setClientActions", ClientActions.class)
                            .invoke(controller, clientActions);
                } catch (Exception ignored) {}
            }

            if (controller != null && user != null && chatClient != null) {
                try {
                    controller.getClass()
                            .getMethod("setClient", User.class, ChatClient.class)
                            .invoke(controller, user, chatClient);
                } catch (Exception ignored) {}
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = stage.getScene();

            // ✅ ניווט למסך גדול – בלי Scene חדשה
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }

            stage.setTitle("Bistro - " + title);
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ================= UI helpers =================

    private void hideMessages() {
        lblError.setVisible(false);
        lblError.setManaged(false);
        lblInfo.setVisible(false);
        lblInfo.setManaged(false);
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void showInfo(String msg) {
        lblInfo.setText(msg);
        lblInfo.setVisible(true);
        lblInfo.setManaged(true);
    }

    private void showSuccessAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}