package dto;

import java.io.Serializable;
import entities.Enums;

/**
 * Data Transfer Object (DTO) used to submit payment information for a
 * reservation receipt.
 * <p>
 * This object contains the reservation confirmation code, the selected payment
 * type, and optional payment details required to process the transaction.
 * </p>
 */
public class PayReceiptDTO implements Serializable {
	private static final long serialVersionUID = 1L;

	private final String confirmationCode;
	private final Enums.TypeOfPayment paymentType;

	private final Integer last4Digits;
	private final String expiryDate;
	private final Integer cvv;

	public PayReceiptDTO(String confirmationCode, Enums.TypeOfPayment paymentType, Integer last4Digits,
			String expiryDate, Integer cvv) {
		this.confirmationCode = confirmationCode;
		this.paymentType = paymentType;
		this.last4Digits = last4Digits;
		this.expiryDate = expiryDate;
		this.cvv = cvv;
	}

	public String getConfirmationCode() {
		return confirmationCode;
	}

	public Enums.TypeOfPayment getPaymentType() {
		return paymentType;
	}

	public Integer getLast4Digits() {
		return last4Digits;
	}

	public String getExpiryDate() {
		return expiryDate;
	}

	public Integer getCvv() {
		return cvv;
	}
}
