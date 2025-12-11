package guiControllers;

import java.io.IOException;

import entities.Restaurant;
import entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class TableReservation_BController {

    private User user;
    private Restaurant restaurant = Restaurant.getInstance();

    @FXML private BorderPane rootPane;

    @FXML private DatePicker datePicker;
    @FXML private TextField txtHour;
    @FXML private TextField txtGuests;
    @FXML private Label lblMessage;

    public void setClient(User client) {
        this.user = client;
    }

    @FXML
    private void onCreateClicked() {
        lblMessage.setVisible(false);

        if (!validateInputs())
            return;

        showMessage("Reservation created (logic coming soon).");
    }

    @FXML
    private void onJoinWaitingListClicked() {
        lblMessage.setVisible(false);

        if (!validateInputs())
            return;

        showMessage("Added to waiting list (logic coming soon).");
    }

    @FXML
    private void onBackToMenuClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Menu_B.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle("Bistro - Main Menu");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Failed to open main menu.");
        }
    }

    private boolean validateInputs() {
        if (datePicker.getValue() == null) {
            showMessage("Please select a date.");
            return false;
        }

        if (!txtHour.getText().matches("\\d{2}:\\d{2}")) {
            showMessage("Hour must be in HH:MM format.");
            return false;
        }

        try {
            Integer.parseInt(txtGuests.getText());
        } catch (NumberFormatException e) {
            showMessage("Number of guests must be a number.");
            return false;
        }

        return true;
    }

    private void showMessage(String msg) {
        lblMessage.setText(msg);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }
}
