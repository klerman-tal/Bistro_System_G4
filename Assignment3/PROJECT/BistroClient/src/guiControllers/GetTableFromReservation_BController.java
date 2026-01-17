package guiControllers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

import application.ChatClient;
import dto.GetTableResultDTO;
import dto.ResponseDTO;
import entities.Enums.UserRole;
import entities.Reservation;
import entities.User;
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

public class GetTableFromReservation_BController implements ClientResponseHandler {

    @FXML private BorderPane rootPane;
    @FXML private TextField txtCode;
    @FXML private Button btnGetTable;
    @FXML private Label lblInfo;
    @FXML private Label lblError;

    // ✅ Table (visible for: Subscriber/Agent/Manager; hidden for: RandomClient)
    @FXML private VBox boxMyActive;
    @FXML private TableView<Reservation> tblMyActive;
    @FXML private TableColumn<Reservation, LocalDate> colDate;
    @FXML private TableColumn<Reservation, LocalTime> colTime;
    @FXML private TableColumn<Reservation, String> colCode;

    private final ObservableList<Reservation> myActiveReservations = FXCollections.observableArrayList();

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
            if (cd.getValue() == null || cd.getValue().getReservationTime() == null) {
                return new SimpleObjectProperty<>(null);
            }
            return new SimpleObjectProperty<>(cd.getValue().getReservationTime().toLocalDate());
        });

        colTime.setCellValueFactory(cd -> {
            if (cd.getValue() == null || cd.getValue().getReservationTime() == null) {
                return new SimpleObjectProperty<>(null);
            }
            return new SimpleObjectProperty<>(cd.getValue().getReservationTime().toLocalTime());
        });

        colCode.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue() == null ? "" : cd.getValue().getConfirmationCode()));

        tblMyActive.setItems(myActiveReservations);

        tblMyActive.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected != null && selected.getConfirmationCode() != null) {
                txtCode.setText(selected.getConfirmationCode());
            }
        });
    }

    private void loadMyActiveIfAllowed() {
        boolean show = user != null && user.getUserRole() != UserRole.RandomClient;

        if (boxMyActive != null) {
            boxMyActive.setVisible(show);
            boxMyActive.setManaged(show);
        }

        if (!show) return;

        try {
            api.getMyActiveReservations(user.getUserId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onGetTableClicked() {
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
            btnGetTable.setDisable(true);
            api.checkinReservation(code);
            showInfo("Checking your reservation...");
        } catch (Exception e) {
            btnGetTable.setDisable(false);
            showError("Failed to send request to server.");
            e.printStackTrace();
        }
    }

    @Override
    public void handleResponse(ResponseDTO response) {
        Platform.runLater(() -> {
            btnGetTable.setDisable(false);

            if (response == null) {
                showError("No response from server.");
                return;
            }

            // ✅ List for the table
            if (response.isSuccess() && response.getData() instanceof ArrayList<?> list) {
                myActiveReservations.clear();
                for (Object o : list) {
                    if (o instanceof Reservation r) {
                        myActiveReservations.add(r);
                    }
                }
                return;
            }

            // ✅ Check-in result
            GetTableResultDTO res = null;
            try { res = (GetTableResultDTO) response.getData(); } catch (Exception ignored) {}

            if (res != null) {

                if (res.isSuccess() && res.getTableNumber() != null) {
                    String msg = "Welcome 🎉\nYour table number is: " + res.getTableNumber();
                    showSuccessAlert("You are checked-in!", msg);
                    showInfo("Your table number is: " + res.getTableNumber());
                    return;
                }

                if (res.isShouldWait()) {
                    showInfo(res.getMessage() != null
                            ? res.getMessage()
                            : "Please wait. We will notify you when your table is ready.");
                    return;
                }

                showError(res.getMessage() != null ? res.getMessage() : "Check-in failed.");
                return;
            }

            if (response.isSuccess()) {
                showInfo(response.getMessage() != null ? response.getMessage() : "Done.");
            } else {
                showError(response.getMessage() != null ? response.getMessage() : "Request failed.");
            }
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
            stage.setTitle("Bistro - " + title);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideMessages() {
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);

        lblInfo.setText("");
        lblInfo.setVisible(false);
        lblInfo.setManaged(false);
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);

        lblInfo.setText("");
        lblInfo.setVisible(false);
        lblInfo.setManaged(false);
    }

    private void showInfo(String msg) {
        lblInfo.setText(msg);
        lblInfo.setVisible(true);
        lblInfo.setManaged(true);

        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    private void showSuccessAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}