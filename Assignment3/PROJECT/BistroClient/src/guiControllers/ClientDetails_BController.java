package guiControllers;

import java.time.format.DateTimeFormatter;
import java.util.List;

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
    private List<Reservation> reservationHistory;

    @FXML private BorderPane rootPane;

    @FXML private TextField txtSubscriberNumber;
    @FXML private TextField txtUserName;
    @FXML private TextArea txtContactDetails;

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
        new SimpleStringProperty(res.getValue().getConfirmationCode())
);

        colStatus.setCellValueFactory(res ->
                new SimpleStringProperty(
                        res.getValue().isConfirmed() ? "Confirmed" : "Pending"
                ));
    }

    /* =========================
       DATA INJECTION FROM SERVER
       ========================= */

    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
        viewSubscriberDetails();
    }

    public void setReservationHistory(List<Reservation> history) {
        this.reservationHistory = history;
        viewReservationHistory();
    }

    /* =========================
       VIEW METHODS
       ========================= */

    private void viewSubscriberDetails() {
        clearMessage();

        if (subscriber == null) {
            showMessage("No subscriber data.");
            return;
        }

        txtSubscriberNumber.setText(
                String.valueOf(subscriber.getSubscriberId())
        );

        txtUserName.setText(
                subscriber.getFirstName() + " " + subscriber.getLastName()
        );

        txtContactDetails.setText(
                "Phone: " + subscriber.getPhone() + "\n" +
                "Email: " + subscriber.getEmail()
        );
    }

    private void viewReservationHistory() {

        if (reservationHistory == null) {
            tblReservationHistory.getItems().clear();
            return;
        }

        tblReservationHistory.getItems().setAll(reservationHistory);
    }

    /* =========================
       NAVIGATION
       ========================= */

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

    /* =========================
       UI HELPERS
       ========================= */

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
