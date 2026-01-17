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

/**
 * JavaFX controller that allows a user to load a receipt by confirmation code and complete a payment.
 *
 * <p>The flow is split into two steps:</p>
 * <ol>
 *   <li>Load a receipt by entering a confirmation code and requesting the amount from the server.</li>
 *   <li>Pay the loaded receipt using either credit card details or cash.</li>
 * </ol>
 *
 * <p>Server communication is performed through {@link ClientAPI}. This controller implements
 * {@link ClientResponseHandler} and uses a small internal state machine ({@link PendingAction})
 * to determine whether the current response belongs to a "load" request or a "pay" request.</p>
 *
 * <p>The controller stores and restores the previous response handler so it does not break
 * other screens when navigating away.</p>
 */
public class Payment_BController implements ClientResponseHandler {

    private User user;
    private ChatClient chatClient;
    private ClientAPI api;
    private ClientResponseHandler prevHandler;
    private Receipt loadedReceipt;
    private String loadedCode;

    /**
     * Identifies which request is currently awaiting a server response.
     */
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

    /**
     * Initializes UI defaults, input constraints, and listeners.
     * This method is invoked automatically by JavaFX after FXML injection.
     */
    @FXML
    private void initialize() {
        rbCreditCard.setSelected(true);

        addTextLimiter(txtLast4Digits, 4, false);
        addTextLimiter(txtCVV, 3, false);
        addTextLimiter(txtExpiryDate, 5, true);

        paymentTypeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            updateCardFieldsState();
            clearMessage();
        });

        updateCardFieldsState();
        setBillLoaded(false);
    }

    /**
     * Adds a real-time input sanitizer/limiter to a {@link TextField}.
     * The limiter enforces numeric input only, optionally allowing "/" for MM/YY fields,
     * and truncates input to the given maximum length.
     *
     * @param tf         the text field to limit
     * @param maxLength  maximum allowed length
     * @param allowSlash whether to allow "/" in the input
     */
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

    /**
     * Enables/disables credit card fields based on the selected payment type.
     */
    private void updateCardFieldsState() {
        boolean creditSelected = rbCreditCard.isSelected();
        txtLast4Digits.setDisable(!creditSelected);
        txtExpiryDate.setDisable(!creditSelected);
        txtCVV.setDisable(!creditSelected);
    }

    /**
     * Updates the UI state based on whether a receipt is currently loaded.
     * When not loaded, payment is disabled and cached receipt data is cleared.
     *
     * @param loaded {@code true} to enable payment actions; {@code false} to reset state
     */
    private void setBillLoaded(boolean loaded) {
        btnPay.setDisable(!loaded);
        if (!loaded) {
            loadedReceipt = null;
            loadedCode = null;
            lblAmount.setText("—");
        }
    }

    /**
     * Injects the session context and sets up the API.
     * Saves the previously active response handler and replaces it with this controller.
     *
     * @param user       the current logged-in user
     * @param chatClient the active client connection used to communicate with the server
     */
    public void setClient(User user, ChatClient chatClient) {
        this.user = user;
        this.chatClient = chatClient;
        this.api = new ClientAPI(chatClient);
        this.prevHandler = chatClient.getResponseHandler();
        chatClient.setResponseHandler(this);
    }

    /**
     * Loads a receipt from the server using the confirmation code entered by the user.
     * Disables payment until a receipt is successfully loaded.
     */
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

    /**
     * Validates user input according to the selected payment type and sends a payment request.
     * Requires a previously loaded receipt.
     */
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

    /**
     * Validates that the provided MM/YY string is well-formed and represents
     * the current month or a future month.
     *
     * @param dateStr expiry date in the format MM/YY
     * @return {@code true} if the date is valid and not in the past; otherwise {@code false}
     */
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

    /**
     * Sends the payment request to the server and marks the pending action as {@link PendingAction#PAY}.
     *
     * @param dto the payment DTO to send
     */
    private void sendPayment(PayReceiptDTO dto) {
        try {
            pending = PendingAction.PAY;
            api.payReceipt(dto);
        } catch (Exception e) {
            pending = PendingAction.NONE;
            showMessage("Failed to send payment request.");
        }
    }

    /**
     * Routes the server response to the appropriate handler based on {@link #pending}.
     *
     * @param response the response received from the server
     */
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

    /**
     * Handles the response of a "load receipt" request. On success, caches the receipt and enables payment.
     *
     * @param response server response containing the loaded {@link Receipt} on success
     */
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

    /**
     * Handles the response of a payment request. On success, clears the loaded receipt state.
     *
     * @param response server response indicating payment success/failure
     */
    private void handlePayResponse(ResponseDTO response) {
        if (!response.isSuccess()) {
            showMessage(response.getMessage());
        } else {
            showMessage("Payment completed successfully.");
            setBillLoaded(false);
        }
    }

    /**
     * Restores the previous response handler (if available) and navigates back to the "My Visit" menu.
     */
    @FXML
    private void onBackClicked() {
        if (chatClient != null) chatClient.setResponseHandler(prevHandler);
        openWindow("MyVisitMenu_B.fxml", "My Visit");
    }

    /**
     * Loads the given FXML and navigates by swapping the current scene root.
     * Attempts to inject (User, ChatClient) to the destination controller via reflection.
     *
     * @param fxmlName the FXML file name located under /gui/
     * @param title    the screen title suffix
     */
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

    /**
     * Safely returns a trimmed text value from a {@link TextInputControl}.
     *
     * @param c the control to read from
     * @return trimmed text, or an empty string if null/empty
     */
    private String safeText(TextInputControl c) {
        return (c == null || c.getText() == null) ? "" : c.getText().trim();
    }

    /**
     * Displays a message label with the given text.
     *
     * @param text message to display
     */
    private void showMessage(String text) {
        lblMessage.setText(text);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    /**
     * Hides the message label.
     */
    private void clearMessage() {
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
    }

    @Override public void handleConnectionError(Exception e) { showMessage("Connection error"); }
    @Override public void handleConnectionClosed() {}
}
