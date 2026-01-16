package guiControllers;

import java.io.IOException;
import java.util.List;

import application.ChatClient;
import dto.DeleteTableDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import dto.SaveTableDTO;
import entities.Table;
import entities.User;
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
import network.ClientResponseHandler;
import protocol.Commands;

public class UpdateTablesController implements ClientResponseHandler {

    private ChatClient chatClient;
    private User user;

    @FXML private BorderPane rootPane;
    @FXML private TableView<Table> tblTables;
    @FXML private TableColumn<Table, Integer> colNumber;
    @FXML private TableColumn<Table, Integer> colSeats;
    @FXML private TextField txtTableNumber;
    @FXML private TextField txtSeats;
    @FXML private Label lblMsg;

    private final ObservableList<Table> tables = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colNumber.setCellValueFactory(new PropertyValueFactory<>("tableNumber"));
        colSeats.setCellValueFactory(new PropertyValueFactory<>("seatsAmount"));
        tblTables.setItems(tables);

        // הגבלת קלט למספרים בלבד בזמן ההקלדה
        addNumericLimiter(txtTableNumber);
        addNumericLimiter(txtSeats);

        tblTables.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                txtTableNumber.setText(String.valueOf(n.getTableNumber()));
                txtSeats.setText(String.valueOf(n.getSeatsAmount()));
            }
        });
    }

    /**
     * פונקציית עזר המאפשרת הקלדת ספרות בלבד בשדות הטקסט
     */
    private void addNumericLimiter(TextField tf) {
        tf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            if (!newVal.matches("\\d*")) {
                tf.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
    }

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        setClient(chatClient);
    }

    public void setClient(ChatClient chatClient) {
        this.chatClient = chatClient;
        chatClient.setResponseHandler(this);
        requestTables();
    }

    private void requestTables() {
        try {
            RequestDTO req = new RequestDTO(Commands.GET_TABLES, null);
            chatClient.sendToServer(req);
            show("Loading tables...");
        } catch (IOException e) {
            show("Failed to load tables");
        }
    }

    @FXML
    private void onAddOrSave() {
        hide(); // ניקוי הודעות קודמות

        Integer num = parseInt(txtTableNumber.getText(), "Table #");
        Integer seats = parseInt(txtSeats.getText(), "Seats");

        // אם אחד השדות לא תקין, parseInt כבר הציג הודעה
        if (num == null || seats == null) return;

        // בדיקה שהמספרים חיוביים וגדולים מ-0
        if (num <= 0) {
            show("Table number must be greater than 0");
            return;
        }
        if (seats <= 0) {
            show("Seats amount must be greater than 0");
            return;
        }

        try {
            SaveTableDTO dto = new SaveTableDTO(num, seats);
            RequestDTO req = new RequestDTO(Commands.SAVE_TABLE, dto);
            chatClient.sendToServer(req);
            show("Saving table...");
        } catch (IOException e) {
            show("Failed to save table");
        }
    }

    @FXML
    private void onDeleteSelected() {
        Table selected = tblTables.getSelectionModel().getSelectedItem();
        if (selected == null) {
            show("Select a table first");
            return;
        }
        try {
            DeleteTableDTO dto = new DeleteTableDTO(selected.getTableNumber());
            RequestDTO req = new RequestDTO(Commands.DELETE_TABLE, dto);
            chatClient.sendToServer(req);
            show("Deleting table...");
        } catch (IOException e) {
            show("Failed to delete table");
        }
    }

    @FXML
    private void onRefresh() {
        requestTables();
    }

    @Override
    public void handleResponse(ResponseDTO response) {
        if (!response.isSuccess()) {
            Platform.runLater(() -> show(response.getMessage()));
            return;
        }

        Object data = response.getData();
        if (data instanceof List<?> list) {
            if (!list.isEmpty() && !(list.get(0) instanceof Table)) {
                Platform.runLater(() -> show("Unexpected data type from server"));
                return;
            }
            @SuppressWarnings("unchecked")
            List<Table> tableList = (List<Table>) list;
            Platform.runLater(() -> {
                tables.setAll(tableList);
                hide();
            });
            return;
        }

        Platform.runLater(() -> {
            hide();
            requestTables();
        });
    }

    @Override
    public void handleConnectionError(Exception e) {
        Platform.runLater(() -> show("Connection error: " + e.getMessage()));
    }

    @Override
    public void handleConnectionClosed() {
        Platform.runLater(() -> show("Connection closed"));
    }

    @FXML
    private void onBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/RestaurantManagement_B.fxml"));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof RestaurantManagement_BController rm) {
                rm.setClient(user, chatClient);
            }
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Bistro - Restaurant Management");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Integer parseInt(String s, String field) {
        try {
            if (s == null || s.isBlank()) {
                show(field + " is required");
                return null;
            }
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            show(field + " must be a valid number");
            return null;
        }
    }

    private void show(String msg) {
        lblMsg.setText(msg);
        lblMsg.setVisible(true);
        lblMsg.setManaged(true);
    }

    private void hide() {
        lblMsg.setText("");
        lblMsg.setVisible(false);
        lblMsg.setManaged(false);
    }
}