package guiControllers;

import java.io.IOException;
import java.util.List;

import application.ChatClient;
import dto.DeleteTableDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import dto.SaveTableDTO;
import entities.Table;
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
import network.ClientResponseHandler;
import protocol.Commands;

public class UpdateTablesController implements ClientResponseHandler {

    private ChatClient chatClient;

    @FXML private BorderPane rootPane;

    @FXML private TableView<Table> tblTables;
    @FXML private TableColumn<Table, Integer> colNumber;
    @FXML private TableColumn<Table, Integer> colSeats;

    @FXML private TextField txtTableNumber;
    @FXML private TextField txtSeats;

    @FXML private Label lblMsg;

    private final ObservableList<Table> tables =
            FXCollections.observableArrayList();

	private ClientActions clientActions;

    /* ================= INIT ================= */

    @FXML
    private void initialize() {

        colNumber.setCellValueFactory(
                new PropertyValueFactory<>("tableNumber"));
        colSeats.setCellValueFactory(
                new PropertyValueFactory<>("seatsAmount"));

        tblTables.setItems(tables);

        tblTables.getSelectionModel()
                 .selectedItemProperty()
                 .addListener((obs, o, n) -> {
            if (n != null) {
                txtTableNumber.setText(
                        String.valueOf(n.getTableNumber()));
                txtSeats.setText(
                        String.valueOf(n.getSeatsAmount()));
            }
        });
    }
    
    public void setClientActions(ClientActions clientActions) {
        this.clientActions = clientActions;
    }

    
    
    
    

    /**
     * חיבור ה־ChatClient למסך (כמו ב־ClientDetails)
     */
    public void setClient(ChatClient chatClient) {
        this.chatClient = chatClient;
        chatClient.setResponseHandler(this);
        requestTables();
    }

    /* ================= REQUESTS ================= */

    private void requestTables() {
        try {
            RequestDTO req =
                    new RequestDTO(Commands.GET_TABLES, null);
            chatClient.sendToServer(req);
            show("Loading tables...");
        } catch (IOException e) {
            show("Failed to load tables");
        }
    }

    @FXML
    private void onAddOrSave() {

        Integer num = parseInt(txtTableNumber.getText(), "Table #");
        Integer seats = parseInt(txtSeats.getText(), "Seats");
        if (num == null || seats == null) return;

        try {
            SaveTableDTO dto =
                    new SaveTableDTO(num, seats);

            RequestDTO req =
                    new RequestDTO(Commands.SAVE_TABLE, dto);

            chatClient.sendToServer(req);
            show("Saving table...");

        } catch (IOException e) {
            show("Failed to save table");
        }
    }

    @FXML
    private void onDeleteSelected() {

        Table selected =
                tblTables.getSelectionModel().getSelectedItem();

        if (selected == null) {
            show("Select a table first");
            return;
        }

        try {
            DeleteTableDTO dto =
                    new DeleteTableDTO(selected.getTableNumber());

            RequestDTO req =
                    new RequestDTO(Commands.DELETE_TABLE, dto);

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

    /* ================= RESPONSES ================= */

    @Override
    public void handleResponse(ResponseDTO response) {

        if (!response.isSuccess()) {
            Platform.runLater(() ->
                show(response.getMessage()));
            return;
        }

        // קיבלנו רשימת שולחנות
        if (response.getData() instanceof List<?> list &&
            !list.isEmpty() &&
            list.get(0) instanceof Table) {

            @SuppressWarnings("unchecked")
            List<Table> tableList = (List<Table>) list;

            Platform.runLater(() -> {
                tables.setAll(tableList);
                hide();
            });
            return;
        }

        // SAVE / DELETE הצליח → רענון
        Platform.runLater(() -> {
            hide();
            requestTables();
        });
    }

    @Override
    public void handleConnectionError(Exception e) {
        show("Connection error: " + e.getMessage());
    }

    @Override
    public void handleConnectionClosed() {
        show("Connection closed");
    }

    /* ================= NAV ================= */

    @FXML
    private void onBack() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass()
                            .getResource("/gui/RestaurantManagement_B.fxml"));

            Parent root = loader.load();

            Object controller = loader.getController();
            
            
            
 
            Stage stage =
                    (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Bistro - Restaurant Management");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================= HELPERS ================= */

    private Integer parseInt(String s, String field) {
        try {
            if (s == null || s.isBlank()) {
                show(field + " is required");
                return null;
            }
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

    private void hide() {
        lblMsg.setText("");
        lblMsg.setVisible(false);
        lblMsg.setManaged(false);
    }
}
