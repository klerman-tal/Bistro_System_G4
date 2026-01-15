package guiControllers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import application.ChatClient;
import dto.ResponseDTO;
import entities.User;
import entities.Waiting; // שימוש במחלקה הקיימת שלך
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
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

public class ManageWaitingListController {

    @FXML private BorderPane rootPane;
    @FXML private TableView<Waiting> tblWaitingList;
    
    // התאמת העמודות לשמות המשתנים ב-Waiting.java
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
                        tblWaitingList.refresh(); // מוודא שהטבלה מציירת מחדש את השורות שנוספו
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
        // waitingId -> מחפש getWaitingId()
        colWaitingId.setCellValueFactory(new PropertyValueFactory<>("waitingId"));
        
        // createdByUserId -> מחפש getCreatedByUserId()
        colCreatedBy.setCellValueFactory(new PropertyValueFactory<>("createdByUserId"));
        
        // createdByRole -> מחפש getCreatedByRole()
        colRole.setCellValueFactory(new PropertyValueFactory<>("createdByRole"));
        
        // guestAmount -> מחפש getGuestAmount()
        colGuests.setCellValueFactory(new PropertyValueFactory<>("guestAmount"));
        
        // confirmationCode -> מחפש getConfirmationCode()
        colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        
        // tableFreedTime -> מחפש getTableFreedTime()
        colFreedTime.setCellValueFactory(new PropertyValueFactory<>("tableFreedTime"));
        
        // tableNumber -> מחפש getTableNumber()
        colTableNum.setCellValueFactory(new PropertyValueFactory<>("tableNumber"));
        
        // waitingStatus -> מחפש getWaitingStatus()
        colStatus.setCellValueFactory(new PropertyValueFactory<>("waitingStatus"));

        tblWaitingList.setItems(waitingData);
    }
    
    @FXML
    private void onRefreshClicked() {
        // ניקוי זמני של הרשימה נותן אינדיקציה ויזואלית למשתמש שהנתונים נטענים מחדש
        waitingData.clear();
        
        // קריאה למתודה שקיימת כבר אצלך בקוד ושולחת בקשה לשרת
        loadWaitingList();
        
        System.out.println("DEBUG: Waiting list refresh requested by manager.");
    }

    private void loadWaitingList() {
        try {
            // וודא שקיימת מתודה כזו ב-ClientAPI שלך
            clientAPI.getWaitingList(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}