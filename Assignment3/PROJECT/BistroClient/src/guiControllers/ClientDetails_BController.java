package guiControllers;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import application.ChatClient;
import dto.GetReservationHistoryDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import dto.UpdateSubscriberDetailsDTO;
import entities.Reservation;
import entities.Subscriber;
import entities.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import network.ClientResponseHandler;
import protocol.Commands;

public class ClientDetails_BController implements ClientResponseHandler {

    private Subscriber subscriber;
    private ChatClient chatClient;

    @FXML private BorderPane rootPane;

    @FXML private TextField txtSubscriberNumber;
    @FXML private TextField txtUserName;
    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtPhoneNumber;
    @FXML private TextField txtEmail;

    @FXML private Label lblMessage;

    @FXML private TableView<Reservation> tblReservationHistory;
    @FXML private TableColumn<Reservation, String> colDateTime;
    @FXML private TableColumn<Reservation, String> colGuests;
    @FXML private TableColumn<Reservation, String> colCode;
    @FXML private TableColumn<Reservation, String> colStatus;

    private final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {

        colDateTime.setCellValueFactory(r ->
                new SimpleStringProperty(
                        r.getValue().getReservationTime() != null
                                ? r.getValue().getReservationTime().format(formatter)
                                : ""
                ));

        colGuests.setCellValueFactory(r ->
                new SimpleStringProperty(
                        String.valueOf(r.getValue().getGuestAmount())
                ));

        colCode.setCellValueFactory(r ->
                new SimpleStringProperty(
                        r.getValue().getConfirmationCode()
                ));

        colStatus.setCellValueFactory(r ->
                new SimpleStringProperty(
                        r.getValue().getReservationStatus().name()
                ));
    }

    public void setClient(User user, ChatClient chatClient) {

        if (!(user instanceof Subscriber)) {
            showMessage("Only subscribers can view this screen.");
            return;
        }

        this.subscriber = (Subscriber) user;
        this.chatClient = chatClient;

        if (this.chatClient != null) {
            this.chatClient.setResponseHandler(this);
        }

        loadSubscriberDetails();
        requestReservationHistory();
    }

    private void requestReservationHistory() {
        try {
            GetReservationHistoryDTO data =
                    new GetReservationHistoryDTO(subscriber.getUserId());

            RequestDTO request =
                    new RequestDTO(Commands.GET_RESERVATION_HISTORY, data);

            chatClient.sendToServer(request);

        } catch (IOException e) {
            showMessage("Failed to load reservation history");
        }
    }

    @Override
    public void handleResponse(ResponseDTO response) {

        if (!response.isSuccess()) {
            showMessage(response.getMessage());
            return;
        }

        if ("Details updated successfully".equals(response.getMessage())) {
            showMessage("✔ Details updated successfully");
            return;
        }

        if (response.getData() instanceof List<?> list &&
            !list.isEmpty() &&
            list.get(0) instanceof Reservation) {

            @SuppressWarnings("unchecked")
            List<Reservation> history = (List<Reservation>) list;

            Platform.runLater(() ->
                tblReservationHistory.getItems().setAll(history)
            );
        }
    }

    @Override
    public void handleConnectionError(Exception e) {
        showMessage("Connection error: " + e.getMessage());
    }

    @Override
    public void handleConnectionClosed() {
        showMessage("Connection closed.");
    }

    private void loadSubscriberDetails() {
        txtSubscriberNumber.setText(String.valueOf(subscriber.getUserId()));
        txtUserName.setText(subscriber.getUsername());
        txtFirstName.setText(subscriber.getFirstName());
        txtLastName.setText(subscriber.getLastName());
        txtPhoneNumber.setText(subscriber.getPhoneNumber());
        txtEmail.setText(subscriber.getEmail());
    }

    @FXML
    private void onUpdateDetailsClicked() {
        String username = txtUserName.getText().trim();
        String firstName = txtFirstName.getText().trim();
        String lastName = txtLastName.getText().trim();
        String phone = txtPhoneNumber.getText().trim();
        String email = txtEmail.getText().trim();

        if (!phone.matches("^05\\d{8}$")) {
            showMessage("Phone number must start with 05 and contain 10 digits.");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            showMessage("Please enter a valid email address (e.g., name@domain.com).");
            return;
        }

        if (username.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            showMessage("All fields must be filled.");
            return;
        }

        try {
            UpdateSubscriberDetailsDTO dto =
                    new UpdateSubscriberDetailsDTO(
                            subscriber.getUserId(),
                            username,
                            firstName,
                            lastName,
                            phone,
                            email
                    );

            RequestDTO request =
                    new RequestDTO(Commands.UPDATE_SUBSCRIBER_DETAILS, dto);

            chatClient.sendToServer(request);

        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Failed to update details.");
        }
    }

    @FXML
    private void onRefreshClicked() {
        requestReservationHistory();
        showMessage("✔ Data refreshed");
    }

    @FXML
    private void onBackToMenuClicked() {

        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/Menu_B.fxml"));

            Parent root = loader.load();

            Menu_BController menu = loader.getController();
            if (menu != null) {
                menu.setClient(
                        application.ClientSession.getLoggedInUser(),
                        chatClient
                );
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();

            stage.setTitle("Bistro - Main Menu");

            // ✅ שינוי תצוגה בלבד – בלי Scene חדשה
            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }

            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Failed to return to menu.");
        }
    }

    private void showMessage(String msg) {
        lblMessage.setText(msg);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }
}
