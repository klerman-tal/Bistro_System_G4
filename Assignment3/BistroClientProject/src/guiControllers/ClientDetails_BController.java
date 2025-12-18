package guiControllers;

import java.time.format.DateTimeFormatter;

import entities.Reservation;
import entities.Subscriber;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientDetails_BController {

    private Subscriber subscriber;

    @FXML private BorderPane rootPane;

    @FXML private TextField txtSubscriberNumber;
    @FXML private TextField txtUserName;
    @FXML private TextArea txtPersonalDetails;

    @FXML private Label lblMessage;

    @FXML private TableView<Reservation> tblReservationHistory;
    @FXML private TableColumn<Reservation, String> colDateTime;
    @FXML private TableColumn<Reservation, String> colGuests;
    @FXML private TableColumn<Reservation, String> colCode;
    @FXML private TableColumn<Reservation, String> colStatus;

    private final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {

        colDateTime.setCellValueFactory(res ->
                new SimpleStringProperty(
                        res.getValue().getReservationTime() != null
                                ? res.getValue().getReservationTime().format(dateTimeFormatter)
                                : ""
                ));

        colGuests.setCellValueFactory(res ->
                new SimpleStringProperty(
                        String.valueOf(res.getValue().getGuestAmount())
                ));

        colCode.setCellValueFactory(res ->
                new SimpleStringProperty(
                        String.valueOf(res.getValue().CreateConfirmationCode())
                ));

        colStatus.setCellValueFactory(res ->
                new SimpleStringProperty(
                        res.getValue().isConfirmed() ? "Confirmed" : "Pending"
                ));
    }

    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
        viewDetails();
        viewReservationHistory();
    }

    private void viewDetails() {
        clearMessage();

        if (subscriber == null) {
            showMessage("No subscriber found.");
            return;
        }

        txtSubscriberNumber.setText(
                String.valueOf(subscriber.getSubscriberNumber())
        );

        txtUserName.setText(subscriber.getUserName());
        txtPersonalDetails.setText(subscriber.getPersonalDetails());
    }

    @FXML
    private void onUpdateDetailsClicked() {
        updateDetails();
    }

    private void updateDetails() {
        clearMessage();

        if (subscriber == null) {
            showMessage("No subscriber found.");
            return;
        }

        subscriber.setPersonalDetails(
                txtPersonalDetails.getText().trim()
        );

        showMessage("Details updated locally.");
    }

    private void viewReservationHistory() {
        clearMessage();

        if (subscriber == null || subscriber.getReservationHistory() == null) {
            tblReservationHistory.getItems().clear();
            return;
        }

        tblReservationHistory.getItems()
                .setAll(subscriber.getReservationHistory());
    }

    @FXML
    private void onBackToMenuClicked() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/Menu_B.fxml"));

            Parent root = loader.load();

            Stage stage =
                    (Stage) rootPane.getScene().getWindow();

            stage.setTitle("Bistro - Main Menu");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Failed to open main menu.");
        }
    }

    private void showMessage(String msg) {
        lblMessage.setText(msg);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    private void clearMessage() {
        lblMessage.setText("");
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
    }
}
