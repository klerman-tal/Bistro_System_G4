package guiControllers;

import java.io.IOException;
import java.util.List;

import application.ChatClient;
import dto.ResponseDTO;
import entities.Reservation;
import entities.User;
import entities.Enums.UserRole;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

public class ManageCurrentDiners_BController implements ClientResponseHandler {

    @FXML private BorderPane rootPane;
    @FXML private TableView<Reservation> tblCurrentDiners;
    @FXML private TableColumn<Reservation, Integer> colCreatedBy;
    @FXML private TableColumn<Reservation, UserRole> colRole;
    @FXML private TableColumn<Reservation, Integer> colTableNum;

    private User user;
    private ChatClient chatClient;
    private ClientAPI api;
    private final ObservableList<Reservation> dinersData = FXCollections.observableArrayList();

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
        this.api = new ClientAPI(chatClient);

        if (chatClient != null) {
            chatClient.setResponseHandler(this);
        }

        setupTable();
        loadCurrentDiners();
    }

    private void setupTable() {
        colCreatedBy.setCellValueFactory(new PropertyValueFactory<>("createdByUserId"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("createdByRole"));
        colTableNum.setCellValueFactory(new PropertyValueFactory<>("tableNumber"));

        tblCurrentDiners.setItems(dinersData);
    }

    private void loadCurrentDiners() {
        try {
            api.getCurrentDiners();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onRefreshClicked() {
        dinersData.clear();
        loadCurrentDiners();
    }

    @FXML
    private void onBackClicked() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/RestaurantManagement_B.fxml"));
            Parent root = loader.load();

            RestaurantManagement_BController controller = loader.getController();
            controller.setClient(user, chatClient);

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ======================
    // ClientResponseHandler
    // ======================

    @Override
    public void handleResponse(ResponseDTO response) {

        System.out.println("DEBUG: Response arrived to ManageCurrentDiners");

        if (!response.isSuccess()) {
            System.out.println("ERROR: " + response.getMessage());
            return;
        }

        if (!(response.getData() instanceof List<?>)) {
            System.out.println("DEBUG: Response data is not a list");
            return;
        }

        @SuppressWarnings("unchecked")
        List<Reservation> list = (List<Reservation>) response.getData();

        System.out.println("DEBUG: Received " + list.size() + " diners from server");

        Platform.runLater(() -> {
            dinersData.setAll(list);
            tblCurrentDiners.refresh();
        });
    }

    @Override
    public void handleConnectionError(Exception e) {
        e.printStackTrace();
    }

    @Override
    public void handleConnectionClosed() {
        System.out.println("Connection closed.");
    }
}
