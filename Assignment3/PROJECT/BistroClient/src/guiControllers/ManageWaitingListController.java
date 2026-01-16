package guiControllers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import application.ChatClient;
import dto.ResponseDTO;
import entities.User;
import entities.Waiting;
import entities.Enums.UserRole;
import entities.Enums.WaitingStatus;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

public class ManageWaitingListController {

    @FXML private BorderPane rootPane;
    @FXML private TableView<Waiting> tblWaitingList;
    @FXML private TableColumn<Waiting, Integer> colWaitingId;
    @FXML private TableColumn<Waiting, Integer> colCreatedBy;
    @FXML private TableColumn<Waiting, UserRole> colRole;
    @FXML private TableColumn<Waiting, Integer> colGuests;
    @FXML private TableColumn<Waiting, String> colCode;
    @FXML private TableColumn<Waiting, LocalDateTime> colFreedTime;
    @FXML private TableColumn<Waiting, Integer> colTableNum;
    @FXML private TableColumn<Waiting, WaitingStatus> colStatus;

    private User user;
    private ChatClient chatClient;
    private ClientAPI clientAPI;
    private final ObservableList<Waiting> waitingData = FXCollections.observableArrayList();

    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
        this.clientAPI = new ClientAPI(chatClient);

        chatClient.setResponseHandler(new ClientResponseHandler() {
            @Override
            public void handleResponse(ResponseDTO response) {
                if (response.isSuccess() && response.getData() instanceof List<?>) {
                    Platform.runLater(() -> {
                        @SuppressWarnings("unchecked")
                        List<Waiting> list = (List<Waiting>) response.getData();
                        waitingData.setAll(list);
                        tblWaitingList.refresh();
                    });
                }
            }
            @Override public void handleConnectionError(Exception e) {}
            @Override public void handleConnectionClosed() {}
        });

        setupTable();
        loadWaitingList();
    }

    private void setupTable() {
        colWaitingId.setCellValueFactory(new PropertyValueFactory<>("waitingId"));
        colCreatedBy.setCellValueFactory(new PropertyValueFactory<>("createdByUserId"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("createdByRole"));
        colGuests.setCellValueFactory(new PropertyValueFactory<>("guestAmount"));
        colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        colFreedTime.setCellValueFactory(new PropertyValueFactory<>("tableFreedTime"));
        colTableNum.setCellValueFactory(new PropertyValueFactory<>("tableNumber"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("waitingStatus"));

        // הוספת צביעת שורות לפי סטטוס
        tblWaitingList.setRowFactory(tv -> new TableRow<Waiting>() {
            @Override
            protected void updateItem(Waiting item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("row-cancelled", "row-seated");
                if (item != null && !empty) {
                    if (item.getWaitingStatus() == WaitingStatus.Cancelled) {
                        getStyleClass().add("row-cancelled");
                    } else if (item.getWaitingStatus() == WaitingStatus.Seated) {
                        getStyleClass().add("row-seated");
                    }
                }
            }
        });

        tblWaitingList.setItems(waitingData);
    }

    @FXML
    private void onAddWaitingClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/JoinWaiting_B.fxml")); 
            Parent root = loader.load();
            JoinWaiting_BController controller = loader.getController();
            controller.setClient(user, chatClient);
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void onCancelWaitingClicked() {
        Waiting selected = tblWaitingList.getSelectionModel().getSelectedItem();
        if (selected == null) return; 

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/CancelWaiting_B.fxml"));
            Parent root = loader.load();
            CancelWaiting_BController controller = loader.getController();
            
            // הקריאה למתודה החדשה שיצרנו ב-CancelWaiting_BController
            controller.setClient(user, chatClient, selected.getConfirmationCode());
            
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    @FXML
    private void onRefreshClicked() {
        waitingData.clear();
        loadWaitingList();
    }

    private void loadWaitingList() {
        try { clientAPI.getWaitingList(); } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void onBackClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/RestaurantManagement_B.fxml"));
            Parent root = loader.load();
            RestaurantManagement_BController controller = loader.getController();
            controller.setClient(user, chatClient);
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }
}