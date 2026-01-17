package guiControllers;

import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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
    @FXML private TextField txtCVV;
    @FXML private Button btnPay;
    @FXML private Button btnBack;
    @FXML private Label lblMessage;

    @FXML
    private void initialize() {
        rbCreditCard.setSelected(true);

        // הגבלת אורך תווים ומספרים בלבד בזמן אמת
        addTextLimiter(txtLast4Digits, 4, false);
        addTextLimiter(txtCVV, 3, false);
        addTextLimiter(txtExpiryDate, 5, true); // תומך בלוכסן

        paymentTypeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            updateCardFieldsState();
            clearMessage();
        });

        updateCardFieldsState();
        setBillLoaded(false);
    }

    private void addTextLimiter(TextField tf, int maxLength, boolean allowSlash) {
        tf.textProperty().addListener((ov, oldValue, newValue) -> {
            if (newValue == null) return;
            String regex = allowSlash ? "[^\\d/]" : "[^\\d]";
            String cleanValue = newValue.replaceAll(regex, "");
            if (cleanValue.length() > maxLength) {
                tf.setText(cleanValue.substring(0, maxLength));
            } else {
                tf.setText(cleanValue);
            }
        });
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

        if (rbCreditCard.isSelected()) {
            String last4 = safeText(txtLast4Digits);
            String expiry = safeText(txtExpiryDate);
            String cvv = safeText(txtCVV);

            if (last4.length() != 4) {
                showMessage("Last 4 digits must be exactly 4 numbers.");
                return;
            }
            if (!isValidFutureDate(expiry)) {
                showMessage("Enter a valid future date (MM/YY).");
                return;
            }
            if (cvv.length() != 3) {
                showMessage("CVV must be exactly 3 numbers.");
                return;
            }

            sendPayment(new PayReceiptDTO(loadedCode, Enums.TypeOfPayment.CreditCard,
                    Integer.parseInt(last4), expiry, Integer.parseInt(cvv)));
        } else {
            sendPayment(new PayReceiptDTO(loadedCode, Enums.TypeOfPayment.Cash, null, null, null));
        }
    }

    private boolean isValidFutureDate(String dateStr) {
        if (!dateStr.matches("(0[1-9]|1[0-2])/[0-9]{2}")) return false;
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yy");
            YearMonth expDate = YearMonth.parse(dateStr, fmt);
            return !expDate.isBefore(YearMonth.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private void sendPayment(PayReceiptDTO dto) {
        try {
            pending = PendingAction.PAY;
            api.payReceipt(dto);
        } catch (Exception e) {
            pending = PendingAction.NONE;
            showMessage("Failed to send payment request.");
        }
    }

    @Override
    public void handleResponse(ResponseDTO response) {
        if (response == null) return;
        if (pending == PendingAction.LOAD) {
            handleLoadResponse(response);
        } else if (pending == PendingAction.PAY) {
            handlePayResponse(response);
        }
        pending = PendingAction.NONE;
    }

    private void handleLoadResponse(ResponseDTO response) {
        if (!response.isSuccess() || !(response.getData() instanceof Receipt receipt)) {
            lblAmount.setText("—");
            setBillLoaded(false);
            showMessage(response.getMessage());
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
        } else {
            showMessage("Payment completed successfully.");
            setBillLoaded(false);
        }
    }

    @FXML
    private void onBackClicked() {
        if (chatClient != null) chatClient.setResponseHandler(prevHandler);
        openWindow("MyVisitMenu_B.fxml", "My Visit");
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
            Scene scene = stage.getScene();

            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }

            stage.setTitle("Bistro - " + title);
            stage.setMaximized(true);
            stage.show();

        } catch (IOException e) {
            showMessage("Failed to open " + title);
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
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
    }

    @Override public void handleConnectionError(Exception e) { showMessage("Connection error"); }
    @Override public void handleConnectionClosed() {}
}
