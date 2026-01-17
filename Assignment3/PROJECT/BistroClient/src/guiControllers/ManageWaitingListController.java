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

    private final ObservableList<Waiting> waitingData =
            FXCollections.observableArrayList();

    /* ================= SESSION ================= */

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

    /* ================= TABLE ================= */

    private void setupTable() {
        colWaitingId.setCellValueFactory(new PropertyValueFactory<>("waitingId"));
        colCreatedBy.setCellValueFactory(new PropertyValueFactory<>("createdByUserId"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("createdByRole"));
        colGuests.setCellValueFactory(new PropertyValueFactory<>("guestAmount"));
        colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        colFreedTime.setCellValueFactory(new PropertyValueFactory<>("tableFreedTime"));
        colTableNum.setCellValueFactory(new PropertyValueFactory<>("tableNumber"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("waitingStatus"));

        // Row coloring by status
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

    /* ================= ACTIONS ================= */

    @FXML
    private void onAddWaitingClicked() {
        openScreen("/gui/JoinWaiting_B.fxml", controller -> {
            JoinWaiting_BController c = (JoinWaiting_BController) controller;
            c.setClient(user, chatClient);
            c.setBackFxml("/gui/ManageWaitingList.fxml");
        });
    }


    @FXML
    private void onCancelWaitingClicked() {
        Waiting selected = tblWaitingList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        openScreen("/gui/CancelWaiting_B.fxml", controller -> {
            CancelWaiting_BController c = (CancelWaiting_BController) controller;
            c.setClient(user, chatClient, selected.getConfirmationCode());
            c.setBackFxml("/gui/ManageWaitingList.fxml");
        });
    }


    @FXML
    private void onRefreshClicked() {
        waitingData.clear();
        loadWaitingList();
    }

    private void loadWaitingList() {
        try {
            clientAPI.getWaitingList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onBackClicked() {
        openScreen("/gui/RestaurantManagement_B.fxml", controller ->
                ((RestaurantManagement_BController) controller)
                        .setClient(user, chatClient)
        );
    }

    /* ================= NAVIGATION ================= */

    private void openScreen(String fxmlPath, java.util.function.Consumer<Object> injector) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller != null && injector != null) {
                injector.accept(controller);
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = stage.getScene();

            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }

            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
