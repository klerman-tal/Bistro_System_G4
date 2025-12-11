package guiControllers;

import java.io.IOException;

import entities.Enums;
import entities.Recipt;
import entities.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Payment_BController {

    private User user;
    private Recipt recipt;
    private Enums.TypeOfPayment typeOfPayment;
    private int last4DigitsCard;
    private String expiryDateCard;
    private int CVVCard;

    @FXML private BorderPane rootPane;

    @FXML private RadioButton rbCreditCard;
    @FXML private RadioButton rbCash;
    @FXML private ToggleGroup paymentTypeGroup;

    @FXML private TextField txtLast4Digits;
    @FXML private TextField txtExpiryDate;
    @FXML private PasswordField txtCVV;

    @FXML private Button btnPay;
    @FXML private Button btnCancel;
    @FXML private Button btnBack;

    @FXML private Label lblMessage;

    @FXML
    private void initialize() {
        rbCreditCard.setSelected(true);
        updateCardFieldsState();

        paymentTypeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            updateCardFieldsState();
            clearMessage();
        });
    }

    private void updateCardFieldsState() {
        boolean creditSelected = rbCreditCard.isSelected();
        txtLast4Digits.setDisable(!creditSelected);
        txtExpiryDate.setDisable(!creditSelected);
        txtCVV.setDisable(!creditSelected);
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

    public void setClientAndRecipt(User user, Recipt recipt) {
        this.user = user;
        this.recipt = recipt;
    }

    @FXML
    private void onPayClicked(ActionEvent event) {
        clearMessage();

        if (rbCreditCard.isSelected()) {
            PayWithCreditCard();
        } else {
            PayWithCash();
        }
    }

    @FXML
    private void onCancelClicked(ActionEvent event) {
        // כרגע אפשר גם פשוט לחזור לתפריט כמו Back, או להשאיר ללוגיקה עתידית
        onBackToMenuClicked();
    }

    @FXML
    private void onBackToMenuClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Menu_B.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle("Bistro - Main Menu");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Failed to open main menu.");
        }
    }

    public void PayWithCreditCard() {
        String last4Text = txtLast4Digits.getText().trim();
        String expiryText = txtExpiryDate.getText().trim();
        String cvvText = txtCVV.getText().trim();

        if (last4Text.length() != 4 || !last4Text.matches("\\d{4}")) {
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

        last4DigitsCard = Integer.parseInt(last4Text);
        CVVCard = Integer.parseInt(cvvText);
        expiryDateCard = expiryText;
        typeOfPayment = Enums.TypeOfPayment.CreditCard;

        showMessage("Payment with credit card was sent.");
    }

    public void PayWithCash() {
        typeOfPayment = Enums.TypeOfPayment.Cash;
        showMessage("Cash payment was registered.");
    }
}
