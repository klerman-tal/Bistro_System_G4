package guiControllers;

import java.io.IOException;
import java.util.ArrayList;

import application.ClientUI;
import entities.Notification;
import entities.Table;
import interfaces.ChatIF;
import interfaces.ClientActions;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class UpdateTablesController implements ChatIF {

    @FXML private BorderPane rootPane;

    @FXML private TableView<Table> tblTables;
    @FXML private TableColumn<Table, Integer> colNumber;
    @FXML private TableColumn<Table, Integer> colSeats;

    @FXML private TextField txtTableNumber;
    @FXML private TextField txtSeats;

    @FXML private Label lblMsg;

    private ClientActions clientActions;
    private final ObservableList<Table> tables = FXCollections.observableArrayList();

    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
        onRefresh(); // מרענן רק אחרי שה-clientActions קיים
    }

    @FXML
    public void initialize() {
        ClientUI.setActiveController(this);

        colNumber.setCellValueFactory(new PropertyValueFactory<>("tableNumber"));
        colSeats.setCellValueFactory(new PropertyValueFactory<>("seatsAmount"));

        tblTables.setItems(tables);

        tblTables.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                txtTableNumber.setText(String.valueOf(newV.getTableNumber()));
                txtSeats.setText(String.valueOf(newV.getSeatsAmount()));
            }
        });
    }

    @FXML
    private void onAddOrSave() {
        Integer num = parseInt(txtTableNumber.getText(), "Table #");
        Integer seats = parseInt(txtSeats.getText(), "Seats");
        if (num == null || seats == null) return;

        if (clientActions == null) { show("clientActions not set"); return; }

        ArrayList<String> msg = new ArrayList<>();
        msg.add("RM_SAVE_TABLE");
        msg.add(String.valueOf(num));
        msg.add(String.valueOf(seats));
        clientActions.sendToServer(msg);

        show("Sent RM_SAVE_TABLE");
    }

    @FXML
    private void onDeleteSelected() {
        Table selected = tblTables.getSelectionModel().getSelectedItem();
        if (selected == null) { show("Select a table first"); return; }
        if (clientActions == null) { show("clientActions not set"); return; }

        ArrayList<String> msg = new ArrayList<>();
        msg.add("RM_DELETE_TABLE");
        msg.add(String.valueOf(selected.getTableNumber()));
        clientActions.sendToServer(msg);

        show("Sent RM_DELETE_TABLE");
    }

    @FXML
    private void onRefresh() {
        if (clientActions == null) { show("clientActions not set"); return; }

        ArrayList<String> msg = new ArrayList<>();
        msg.add("RM_GET_TABLES");
        clientActions.sendToServer(msg);

        show("Sent RM_GET_TABLES");
    }

    @FXML
    private void onBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/RestaurantManagement_B.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof RestaurantManagement_BController) {
                ((RestaurantManagement_BController) controller).setClientActions(clientActions);
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle("Bistro - Restaurant Management");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    
    public void display(String message) {
        if (message == null) return;

        if (message.startsWith("RM_ERROR|")) {
            String err = message.substring("RM_ERROR|".length());
            Platform.runLater(() -> show(err));
            return;
        }

        if (message.startsWith("RM_OK") || message.startsWith("RM_OK|")) {
            Platform.runLater(() -> {
                hideMsg();
                onRefresh();
            });
            return;
        }

        if (message.startsWith("RM_TABLES|")) {
            String data = message.substring("RM_TABLES|".length());
            ArrayList<Table> newTables = new ArrayList<>();

            if (!data.isBlank()) {
                String[] rows = data.split(";");
                for (String row : rows) {
                    if (row == null || row.isBlank()) continue;

                    String[] parts = row.split(",");
                    if (parts.length < 2) continue;

                    try {
                        int tableNum = Integer.parseInt(parts[0].trim());
                        int seats = Integer.parseInt(parts[1].trim());

                        Table t = new Table();
                        t.setTableNumber(tableNum);
                        t.setSeatsAmount(seats);

                        newTables.add(t);
                    } catch (Exception ignored) {}
                }
            }

            Platform.runLater(() -> {
                tables.setAll(newTables);
                hideMsg();
            });
            return;
        }

        System.out.println("UpdateTables got: " + message);
    }



    private Integer parseInt(String s, String field) {
        try {
            if (s == null || s.isBlank()) { show(field + " is required"); return null; }
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            show(field + " must be a number");
            return null;
        }
    }

    private void show(String msg) {
        lblMsg.setText(msg);
        lblMsg.setVisible(true);
        lblMsg.setManaged(true);
    }

    private void hideMsg() {
        lblMsg.setText("");
        lblMsg.setVisible(false);
        lblMsg.setManaged(false);
    }
    private void showNotification(Notification n) {
        lblMsg.setText(n.getMessage());
        lblMsg.setVisible(true);
        lblMsg.setManaged(true);

        switch (n.getType()) {
            case SUCCESS -> lblMsg.setStyle(
                "-fx-background-color:#d6ffe1; -fx-text-fill:#0b5a1a; -fx-padding:8; -fx-background-radius:6;"
            );
            case ERROR -> lblMsg.setStyle(
                "-fx-background-color:#ffd6d6; -fx-text-fill:#7a0000; -fx-padding:8; -fx-background-radius:6;"
            );
            case WARNING -> lblMsg.setStyle(
                "-fx-background-color:#fff3cd; -fx-text-fill:#856404; -fx-padding:8; -fx-background-radius:6;"
            );
            case INFO -> lblMsg.setStyle(
                "-fx-background-color:#e7f1ff; -fx-text-fill:#084298; -fx-padding:8; -fx-background-radius:6;"
            );
        }
    }


}
