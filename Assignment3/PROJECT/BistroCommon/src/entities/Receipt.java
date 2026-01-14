package entities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import entities.Enums.TypeOfPayment;
import entities.Enums.UserRole;

/**
 * Receipt entity: created at check-in and later marked as paid at payment time.
 */
public class Receipt implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===== Fields =====
    private int receiptId;                 // DB AUTO_INCREMENT
    private int reservationId;             // FK to reservations.reservation_id
    private LocalDateTime createdAt;       // when receipt was created (check-in)
    private BigDecimal amount;             // total amount
    private boolean paid;                  // is_paid
    private LocalDateTime paidAt;          // paid_at (nullable)
    private TypeOfPayment paymentType;     // nullable until payment
    private Integer createdByUserId;       // who created the receipt (optional)
    private UserRole createdByRole;        // optional

    // ===== Constructor =====
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
