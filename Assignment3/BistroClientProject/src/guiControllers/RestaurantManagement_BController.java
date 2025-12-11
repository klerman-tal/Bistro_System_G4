package guiControllers;

import java.io.IOException;

import entities.Restaurant;
import entities.RestaurantAgent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class RestaurantManagement_BController {

    private RestaurantAgent restaurantAgent;
    private Restaurant restaurant = Restaurant.getInstance();

    @FXML private BorderPane rootPane;

    @FXML private Button btnUpdateTables;
    @FXML private Button btnUpdateOpeningHours;
    @FXML private Button btnManageUsers;
    @FXML private Button btnManageReservation;
    @FXML private Button btnBack;

    @FXML private Label lblMessage;

    public void setRestaurantAgent(RestaurantAgent restaurantAgent) {
        this.restaurantAgent = restaurantAgent;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    private void showMessage(String text) {
        lblMessage.setText(text);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    private void clearMessage() {
        lblMessage.setText("");
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
    }

    @FXML
    private void onUpdateTablesClicked(ActionEvent event) {
        clearMessage();
        showMessage("UpdateTables() clicked (to be implemented).");
    }

    @FXML
    private void onUpdateOpeningHoursClicked(ActionEvent event) {
        clearMessage();
        showMessage("UpdateOpeningHours() clicked (to be implemented).");
    }

    @FXML
    private void onManageUsersClicked(ActionEvent event) {
        clearMessage();
        openWindow("ManageUsers.fxml", "Manage Users");
    }

    @FXML
    private void onManageReservationClicked(ActionEvent event) {
        clearMessage();
        openWindow("ManageReservations.fxml", "Manage Reservations");
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

    private void openWindow(String fxmlName, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
            Parent root = loader.load();

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Failed to open window: " + fxmlName);
        }
    }
}
