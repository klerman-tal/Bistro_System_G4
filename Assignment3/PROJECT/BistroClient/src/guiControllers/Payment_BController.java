package guiControllers;

import java.io.IOException;

import application.ChatClient;
import dto.PayReceiptDTO;
import dto.ResponseDTO;
import entities.Enums;
import entities.Receipt;
import entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

public class Payment_BController implements ClientResponseHandler {

    private User user;
    private ChatClient chatClient;
    private ClientAPI api;

    private ClientResponseHandler prevHandler;

    private Receipt loadedReceipt;
    private String loadedCode;

    private enum PendingAction { NONE, LOAD, PAY }
    private PendingAction pending = PendingAction.NONE;

    @FXML private BorderPane rootPane;

    @FXML private RadioButton rbCreditCard;
    @FXML private RadioButton rbCash;
    @FXML private ToggleGroup paymentTypeGroup;

    @FXML private TextField txtConfirmationCode;
    @FXML private Button btnLoad;

    @FXML private Label lblAmount;

    @FXML private TextField txtLast4Digits;
    @FXML private TextField txtExpiryDate;
    @FXML private PasswordField txtCVV;

    @FXML private Button btnPay;
    @FXML private Button btnBack;

    @FXML private Label lblMessage;

    @FXML
    private void initialize() {
        System.out.println("Payment init ✅");
        rbCreditCard.setSelected(true);

        paymentTypeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            updateCardFieldsState();
            clearMessage();
        });

        updateCardFieldsState();
        setBillLoaded(false);
    }

    private void updateCardFieldsState() {
        boolean creditSelected = rbCreditCard.isSelected();
        txtLast4Digits.setDisable(!creditSelected);
        txtExpiryDate.setDisable(!creditSelected);
        txtCVV.setDisable(!creditSelected);
    }

    private void setBillLoaded(boolean loaded) {
        btnPay.setDisable(!loaded);
        if (!loaded) {
            loadedReceipt = null;
            loadedCode = null;
            lblAmount.setText("—");
        }
    }

    // ✅ same signature as all other screens
    public void setClient(User user, ChatClient chatClient) {

        this.user = user;
        this.chatClient = chatClient;
        this.api = new ClientAPI(chatClient);

        this.prevHandler = chatClient.getResponseHandler();
        chatClient.setResponseHandler(this);
    }

    @FXML
    private void onLoadClicked() {

        clearMessage();

        String code = safeText(txtConfirmationCode);
        if (code.isEmpty()) {
            showMessage("Please enter confirmation code.");
            setBillLoaded(false);
            return;
        }

        lblAmount.setText("Loading...");
        setBillLoaded(false);

        try {
            pending = PendingAction.LOAD;
            api.getReceiptByCode(code);
        } catch (Exception e) {
            pending = PendingAction.NONE;
            lblAmount.setText("—");
            showMessage("Failed to send load request.");
        }
    }

    @FXML
    private void onPayClicked() {
        clearMessage();

        if (loadedReceipt == null || loadedCode == null) {
            showMessage("Please load the receipt first.");
            return;
        }

        String codeNow = safeText(txtConfirmationCode);
        if (codeNow.isEmpty() || !codeNow.equals(loadedCode)) {
            showMessage("Confirmation code does not match the loaded receipt.");
            return;
        }

        Enums.TypeOfPayment type = rbCreditCard.isSelected()
                ? Enums.TypeOfPayment.CreditCard
                : Enums.TypeOfPayment.Cash;

        PayReceiptDTO dto;

        if (type == Enums.TypeOfPayment.CreditCard) {

            String last4Text = safeText(txtLast4Digits);
            String expiryText = safeText(txtExpiryDate);
            String cvvText = safeText(txtCVV);

            if (!last4Text.matches("\\d{4}")) {
                showMessage("Please enter valid last 4 digits.");
                return;
            }

            if (!cvvText.matches("\\d{3}")) {
                showMessage("Please enter valid 3-digit CVV.");
                return;
            }

            if (expiryText.isEmpty()) {
                showMessage("Please enter card expiry date.");
                return;
            }

            dto = new PayReceiptDTO(codeNow, type,
                    Integer.parseInt(last4Text),
                    expiryText,
                    Integer.parseInt(cvvText));

        } else {
            dto = new PayReceiptDTO(codeNow, type, null, null, null);
        }

        try {
            pending = PendingAction.PAY;
            api.payReceipt(dto);
            showMessage("Payment request was sent...");
        } catch (Exception e) {
            pending = PendingAction.NONE;
            showMessage("Failed to send payment request.");
        }
    }

    @FXML
    private void onBackClicked() {
        restorePrevHandler();
        openWindow("MyVisitMenu_B.fxml", "My Visit");
    }

    private void restorePrevHandler() {
        if (chatClient != null) {
            chatClient.setResponseHandler(prevHandler);
        }
    }

    @Override
    public void handleResponse(ResponseDTO response) {
        if (response == null) return;

        if (pending == PendingAction.LOAD) {
            handleLoadResponse(response);
            pending = PendingAction.NONE;
            return;
        }

        if (pending == PendingAction.PAY) {
            handlePayResponse(response);
            pending = PendingAction.NONE;
            return;
        }

        if (!response.isSuccess()) {
            showMessage(response.getMessage());
        }
    }

    private void handleLoadResponse(ResponseDTO response) {
        if (!response.isSuccess()) {
            lblAmount.setText("—");
            setBillLoaded(false);
            showMessage(response.getMessage());
            return;
        }

        Object data = response.getData();
        if (!(data instanceof Receipt receipt)) {
            lblAmount.setText("—");
            setBillLoaded(false);
            showMessage("Invalid receipt data from server.");
            return;
        }

        this.loadedReceipt = receipt;
        this.loadedCode = safeText(txtConfirmationCode);

        lblAmount.setText("₪ " + receipt.getAmount());
        setBillLoaded(true);
        showMessage("Receipt loaded.");
    }

    private void handlePayResponse(ResponseDTO response) {
        if (!response.isSuccess()) {
            showMessage(response.getMessage());
            return;
        }

        showMessage("Payment completed successfully.");
        setBillLoaded(false);
    }

    @Override
    public void handleConnectionError(Exception exception) {
        showMessage("Connection error: " + (exception != null ? exception.getMessage() : ""));
        pending = PendingAction.NONE;
    }

    @Override
    public void handleConnectionClosed() {
        // optional
    }

    private void openWindow(String fxmlName, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
            Parent root = loader.load();

            Object controller = loader.getController();

            if (controller != null && user != null && chatClient != null) {
                try {
                    controller.getClass()
                            .getMethod("setClient", User.class, ChatClient.class)
                            .invoke(controller, user, chatClient);
                } catch (Exception ignored) {}
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle("Bistro - " + title);
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Failed to open " + title + ".");
        }
    }

    private String safeText(TextInputControl c) {
        return (c == null || c.getText() == null) ? "" : c.getText().trim();
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
}
