package guiControllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import application.ChatClient;
import entities.User;
import interfaces.ClientActions;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class ManageReservationController implements Initializable {

    @FXML private BorderPane rootPane;

    @FXML private TableView<?> tblReservations;

    @FXML private TableColumn<?, ?> colReservationId;
    @FXML private TableColumn<?, ?> colDate;
    @FXML private TableColumn<?, ?> colTime;
    @FXML private TableColumn<?, ?> colGuests;
    @FXML private TableColumn<?, ?> colCode;
    @FXML private TableColumn<?, ?> colCreatedBy;
    @FXML private TableColumn<?, ?> colConfirmed;
    @FXML private TableColumn<?, ?> colTableNumber;

    @FXML private TextField txtReservationId;
    @FXML private TextField txtConfirmationCode;
    @FXML private DatePicker dpDate;
    @FXML private TextField txtTime;
    @FXML private TextField txtGuests;
    @FXML private TextField txtCreatedBy;
    @FXML private CheckBox chkConfirmed;
    @FXML private TextField txtTableNumber;

    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;
    @FXML private Button btnBack;

    @FXML private Label lblStatus;

    private ClientActions clientActions;

    // ✅ session
    private User user;
    private ChatClient chatClient;

    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    // ✅ חדש
    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        hideStatus();
    }

    @FXML
    private void onAddClicked() {
        showStatus("Add clicked (not wired yet)");
    }

    @FXML
    private void onUpdateClicked() {
        showStatus("Update clicked (not wired yet)");
    }

    @FXML
    private void onDeleteClicked() {
        showStatus("Delete clicked (not wired yet)");
    }

    @FXML
    private void onBackClicked() {
        openWindow("RestaurantManagement_B.fxml", "Restaurant Management");
    }

    private void openWindow(String fxmlName, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
            Parent root = loader.load();

            Object controller = loader.getController();

            // להעביר clientActions אם יש
            if (controller != null && clientActions != null) {
                try {
                    controller.getClass()
                              .getMethod("setClientActions", ClientActions.class)
                              .invoke(controller, clientActions);
                } catch (Exception ignored) {}
            }

            // ✅ להעביר session אם יש setClient
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

        } catch (IOException e) {
            e.printStackTrace();
            showStatus("Failed to open: " + fxmlName);
        }
    }

    private void hideStatus() {
        if (lblStatus != null) {
            lblStatus.setText("");
            lblStatus.setVisible(false);
            lblStatus.setManaged(false);
        }
    }

    private void showStatus(String msg) {
        if (lblStatus != null) {
            lblStatus.setText(msg);
            lblStatus.setVisible(true);
            lblStatus.setManaged(true);
        }
    }
}
