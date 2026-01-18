package entities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import entities.Enums.TypeOfPayment;
import entities.Enums.UserRole;

/**
 * Entity representing a receipt generated for a restaurant reservation.
 * <p>
 * A receipt is created at check-in time and may later be marked as paid. This
 * entity stores payment-related information, timestamps, and optional metadata
 * about the user who created the receipt.
 * </p>
 */
public class Receipt implements Serializable {

	private static final long serialVersionUID = 1L;

	// ===== Fields =====
	private int receiptId;
	private int reservationId;
	private LocalDateTime createdAt;
	private BigDecimal amount;
	private boolean paid;
	private LocalDateTime paidAt;
	private TypeOfPayment paymentType;
	private Integer createdByUserId;
	private UserRole createdByRole;

	/**
	 * Default constructor.
	 * <p>
	 * Initializes the receipt with default values and is typically used for
	 * framework support or database mapping.
	 * </p>
	 */
	public Receipt() {
		this.receiptId = 0;
		this.reservationId = 0;
		this.createdAt = null;
		this.amount = BigDecimal.ZERO;
		this.paid = false;
		this.paidAt = null;
		this.paymentType = null;
		this.createdByUserId = null;
		this.createdByRole = null;
	}

	// ===== Getters / Setters =====
	public int getReceiptId() {
		return receiptId;
	}

	public void setReceiptId(int receiptId) {
		this.receiptId = receiptId;
	}

	public int getReservationId() {
		return reservationId;
	}

	public void setReservationId(int reservationId) {
		this.reservationId = reservationId;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	/**
	 * Sets the receipt amount.
	 * <p>
	 * If a {@code null} value is provided, the amount is safely initialized to
	 * {@link BigDecimal#ZERO}.
	 * </p>
	 */
	public void setAmount(BigDecimal amount) {
		this.amount = (amount == null) ? BigDecimal.ZERO : amount;
	}

	public boolean isPaid() {
		return paid;
	}

	public void setPaid(boolean paid) {
		this.paid = paid;
	}

	public LocalDateTime getPaidAt() {
		return paidAt;
	}

	public void setPaidAt(LocalDateTime paidAt) {
		this.paidAt = paidAt;
	}

	public TypeOfPayment getPaymentType() {
		return paymentType;
	}

	public void setPaymentType(TypeOfPayment paymentType) {
		this.paymentType = paymentType;
	}

	public Integer getCreatedByUserId() {
		return createdByUserId;
	}

	public void setCreatedByUserId(Integer createdByUserId) {
		this.createdByUserId = createdByUserId;
	}

	public UserRole getCreatedByRole() {
		return createdByRole;
	}

	public void setCreatedByRole(UserRole createdByRole) {
		this.createdByRole = createdByRole;
	}
}
