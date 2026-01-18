package guiControllers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import application.ChatClient;
import dto.ResponseDTO;
import entities.Reservation;
import entities.User;
import entities.Enums.ReservationStatus;
import interfaces.ClientActions;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

public class ManageReservationController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private Label lblStatus;

    @FXML private TextField txtFilter;
    @FXML private ComboBox<String> cmbMatchMode;

    @FXML private TableView<Reservation> tblReservations;
    @FXML private TableColumn<Reservation, Integer> colReservationId;
    @FXML private TableColumn<Reservation, LocalDateTime> colDateTime;
    @FXML private TableColumn<Reservation, Integer> colGuests;
    @FXML private TableColumn<Reservation, String> colCode;
    @FXML private TableColumn<Reservation, Integer> colCreatedBy;
    @FXML private TableColumn<Reservation, ReservationStatus> colStatus;
    @FXML private TableColumn<Reservation, Integer> colTableNumber;

    @FXML private Button btnAdd;
    @FXML private Button btnDelete;
    @FXML private Button btnBack;

    private User user;
    private ChatClient chatClient;
    private ClientAPI clientAPI;

    private final ObservableList<Reservation> reservationsList =
            FXCollections.observableArrayList();

    private FilteredList<Reservation> filteredReservations;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {}

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;

        this.clientAPI = new ClientAPI(chatClient);
        chatClient.setResponseHandler(new ClientResponseHandler() {
            @Override
            public void handleResponse(ResponseDTO response) {
                Platform.runLater(() -> handleServerResponse(response));
            }
            @Override public void handleConnectionError(Exception e) {}
            @Override public void handleConnectionClosed() {}
        });

        initializeTable();
        loadReservations();
    }

    private void initializeTable() {
        colReservationId.setCellValueFactory(new PropertyValueFactory<>("reservationId"));
        colDateTime.setCellValueFactory(new PropertyValueFactory<>("reservationTime"));
        colGuests.setCellValueFactory(new PropertyValueFactory<>("guestAmount"));
        colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        colCreatedBy.setCellValueFactory(new PropertyValueFactory<>("createdByUserId"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("reservationStatus"));
        colTableNumber.setCellValueFactory(new PropertyValueFactory<>("tableNumber"));

        filteredReservations = new FilteredList<>(reservationsList, r -> true);

        SortedList<Reservation> sorted = new SortedList<>(filteredReservations);
        sorted.comparatorProperty().bind(tblReservations.comparatorProperty());
        tblReservations.setItems(sorted);

        cmbMatchMode.getItems().setAll("Contains", "Exact");
        cmbMatchMode.getSelectionModel().select("Exact");

        txtFilter.textProperty().addListener((o, oldV, newV) -> applyFilter());
        cmbMatchMode.valueProperty().addListener((o, oldV, newV) -> applyFilter());
    }

    private void applyFilter() {
        String text = txtFilter.getText() == null ? "" : txtFilter.getText().trim().toLowerCase();
        boolean exact = "Exact".equals(cmbMatchMode.getValue());

        if (text.isEmpty()) {
            filteredReservations.setPredicate(r -> true);
            return;
        }

        filteredReservations.setPredicate(r -> {
            String[] fields = {
                String.valueOf(r.getReservationId()),
                r.getConfirmationCode(),
                String.valueOf(r.getCreatedByUserId()),
                r.getReservationStatus() == null ? "" : r.getReservationStatus().toString(),
                r.getTableNumber() == null ? "" : String.valueOf(r.getTableNumber())
            };

            for (String f : fields) {
                if (f == null) continue;
                String val = f.toLowerCase();
                if (exact && val.equals(text)) return true;
                if (!exact && val.contains(text)) return true;
            }
            return false;
        });
    }

    @FXML
    private void onClearFilter() {
        txtFilter.clear();
        cmbMatchMode.getSelectionModel().select("Exact");
    }

    private void loadReservations() {
        try {
            clientAPI.getAllReservations();
        } catch (IOException e) {
            showMessage("Failed to load reservations");
        }
    }

    private void handleServerResponse(ResponseDTO response) {
        if (response.isSuccess() && response.getData() instanceof List<?>) {
            reservationsList.setAll((List<Reservation>) response.getData());
            hideMessage();
        }
    }

    @FXML private void onAddClicked() {}
    @FXML private void onDeleteClicked() {}
    @FXML private void onBackClicked() {}

    private void showMessage(String msg) {
        lblStatus.setText(msg);
        lblStatus.setVisible(true);
        lblStatus.setManaged(true);
    }

    private void hideMessage() {
        lblStatus.setVisible(false);
        lblStatus.setManaged(false);
    }
}
