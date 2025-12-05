package gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;

import common.ClientActions;

public class MainGUI implements Initializable {

    // ===== Reference to client side actions (set from ClientUI) =====
    private ClientActions clientActions;

    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    // ==== FXML controls ====

    @FXML
    private TableView<OrderRow> tblOrders;

    @FXML
    private TableColumn<OrderRow, Integer> colOrderNumber;

    @FXML
    private TableColumn<OrderRow, String> colOrderDate;

    @FXML
    private TableColumn<OrderRow, Integer> colNumGuests;

    @FXML
    private TableColumn<OrderRow, String> colCode;

    @FXML
    private TableColumn<OrderRow, String> colSubscriberId;

    @FXML
    private TableColumn<OrderRow, String> colPlacingDate;

    @FXML
    private TableColumn<OrderRow, String> colHiddenSubscriberId;

    @FXML
    private DatePicker dateNewDate;

    @FXML
    private TextField txtNewGuests;

    @FXML
    private Button btnUpdateDate;

    @FXML
    private Button btnUpdateGuests;

    // רשימת ההזמנות שמוצגת בטבלה
    private final ObservableList<OrderRow> orders = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // חיבור עמודות לשדות במחלקה OrderRow
        colOrderNumber.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
        colOrderDate.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        colNumGuests.setCellValueFactory(new PropertyValueFactory<>("numGuests"));
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colSubscriberId.setCellValueFactory(new PropertyValueFactory<>("subscriberId"));
        colPlacingDate.setCellValueFactory(new PropertyValueFactory<>("placingDate"));
        colHiddenSubscriberId.setCellValueFactory(new PropertyValueFactory<>("subscriberId"));

        tblOrders.setItems(orders);

        // ---- הגבלת לוח השנה: מהיום ועד חודש קדימה ----
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusMonths(1);

        // ערך ברירת מחדל
        dateNewDate.setValue(today);

        dateNewDate.setDayCellFactory(new Callback<DatePicker, DateCell>() {
            @Override
            public DateCell call(DatePicker picker) {
                return new DateCell() {
                    @Override
                    public void updateItem(LocalDate item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty || item.isBefore(today) || item.isAfter(maxDate)) {
                            setDisable(true);
                            setStyle("-fx-background-color: #dddddd;");
                        }
                    }
                };
            }
        });
    }

    // ===== אירועים מהכפתורים =====

    @FXML
    private void onUpdateDate(ActionEvent event) {

        OrderRow selected = tblOrders.getSelectionModel().getSelectedItem();
        if (selected == null) {
            System.out.println("No order selected");
            return;
        }

        LocalDate newDate = dateNewDate.getValue();
        if (newDate == null) {
            System.out.println("New date is empty");
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusMonths(1);
        if (newDate.isBefore(today) || newDate.isAfter(maxDate)) {
            System.out.println("Date out of allowed range");
            return;
        }

        String newDateStr = newDate.toString(); // yyyy-MM-dd

        if (clientActions != null) {
            ArrayList<String> msg = new ArrayList<>();
            msg.add("UPDATE_DATE");
            msg.add(String.valueOf(selected.getOrderNumber()));
            msg.add(newDateStr);
            clientActions.sendToServer(msg);
        }

        selected.setOrderDate(newDateStr);
        tblOrders.refresh();

        System.out.println("Requested UPDATE_DATE for order " +
                selected.getOrderNumber() + " -> " + newDateStr);
    }

    @FXML
    private void onUpdateGuests(ActionEvent event) {

        OrderRow selected = tblOrders.getSelectionModel().getSelectedItem();
        if (selected == null) {
            System.out.println("No order selected");
            return;
        }

        String txt = txtNewGuests.getText().trim();
        if (txt.isEmpty()) {
            System.out.println("New guests value is empty");
            return;
        }

        int newGuests;
        try {
            newGuests = Integer.parseInt(txt);
        } catch (NumberFormatException e) {
            System.out.println("Invalid guests number");
            return;
        }

        if (clientActions != null) {
            ArrayList<String> msg = new ArrayList<>();
            msg.add("UPDATE_GUESTS");
            msg.add(String.valueOf(selected.getOrderNumber()));
            msg.add(String.valueOf(newGuests));
            clientActions.sendToServer(msg);
        }

        selected.setNumGuests(newGuests);
        tblOrders.refresh();

        System.out.println("Requested UPDATE_GUESTS for order " +
                selected.getOrderNumber() + " -> " + newGuests);
    }

    // ===== כאן מגיעות כל ההודעות מהשרת =====
    public void displayMessageFromServer(String message) {
        System.out.println("SERVER: " + message);

        // שורת כותרת מתחילה את הטבלה מחדש
        if (message.startsWith("Order number")) {
            orders.clear();
            return;
        }

        // שורת מפריד / שגיאה / אישורי עדכון – לא שורות נתונים
        if (message.startsWith("-")) return;
        if (message.startsWith("ERROR")) return;
        if (message.startsWith("Order ") && message.contains("updated")) {
            return;
        }

        // מנסים לפרסר שורה בפורמט:
        // orderNumber | orderDate | numGuests | code | subscriberId | placingDate
        String[] parts = message.split("\\|");
        if (parts.length != 6) {
            return; // לא הפורמט הצפוי
        }

        try {
            int orderNumber = Integer.parseInt(parts[0].trim());
            String orderDate = parts[1].trim();
            int numGuests = Integer.parseInt(parts[2].trim());
            String code = parts[3].trim();
            String subscriberId = parts[4].trim();
            String placingDate = parts[5].trim();

            OrderRow row = new OrderRow(orderNumber, orderDate, numGuests,
                                        code, subscriberId, placingDate);

            orders.add(row);
        } catch (NumberFormatException e) {
            System.out.println("Could not parse order line: " + message);
        }
    }

    // ===== מודל פשוט לשורה בטבלה =====
    public static class OrderRow {
        private int orderNumber;
        private String orderDate;
        private int numGuests;
        private String code;
        private String subscriberId;
        private String placingDate;

        public OrderRow(int orderNumber, String orderDate, int numGuests,
                        String code, String subscriberId, String placingDate) {
            this.orderNumber = orderNumber;
            this.orderDate = LocalDate.parse(orderDate).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            this.numGuests = numGuests;
            this.code = code;
            this.subscriberId = subscriberId;
            this.placingDate = placingDate;
        }

        public int getOrderNumber() { return orderNumber; }
        public void setOrderNumber(int orderNumber) { this.orderNumber = orderNumber; }

        public String getOrderDate() { return orderDate; }
        public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

        public int getNumGuests() { return numGuests; }
        public void setNumGuests(int numGuests) { this.numGuests = numGuests; }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getSubscriberId() { return subscriberId; }
        public void setSubscriberId(String subscriberId) { this.subscriberId = subscriberId; }

        public String getPlacingDate() { return placingDate; }
        public void setPlacingDate(String placingDate) { this.placingDate = placingDate; }
    }
}
