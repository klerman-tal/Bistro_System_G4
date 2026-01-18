package logicControllers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dbControllers.Receipt_DB_Controller;
import entities.Enums;
import entities.Receipt;
import entities.Reservation;

/**
 * Handles all receipt-related business logic.
 * <p>
 * This controller is responsible for creating receipts when needed, retrieving
 * existing receipts, and marking receipts as paid.
 * </p>
 * <p>
 * It acts as a thin logic layer above the {@link Receipt_DB_Controller}, adding
 * validation, idempotency, and business rules.
 * </p>
 */
public class ReceiptController {

	private final Receipt_DB_Controller db;

	/**
	 * Constructs a ReceiptController with the given database controller.
	 *
	 * @param db database controller used for receipt persistence
	 */
	public ReceiptController(Receipt_DB_Controller db) {
		this.db = db;
	}

	/**
	 * Creates a receipt for a reservation at check-in time if it does not already
	 * exist.
	 * <p>
	 * This method is idempotent: if a receipt already exists for the reservation,
	 * the existing receipt is returned without creating a new one.
	 * </p>
	 * <p>
	 * For project purposes, the receipt amount is generated randomly in the range
	 * of 50 to 300.
	 * </p>
	 *
	 * @param reservation the reservation associated with the receipt
	 * @param checkinTime the check-in time used as the receipt creation time
	 * @return the existing or newly created receipt, or {@code null} on failure
	 */
	public Receipt createReceiptIfMissingForCheckin(Reservation reservation, LocalDateTime checkinTime) {

		if (reservation == null)
			return null;
		if (reservation.getReservationId() <= 0)
			return null;

		try {
			Receipt existing = db.getReceiptByReservationId(reservation.getReservationId());
			if (existing != null)
				return existing;

			Receipt r = new Receipt();
			r.setReservationId(reservation.getReservationId());
			r.setCreatedAt(checkinTime != null ? checkinTime : LocalDateTime.now());

			BigDecimal amount = BigDecimal.valueOf(50 + (int) (Math.random() * 251)); // 50..300
			r.setAmount(amount);

			r.setCreatedByUserId(reservation.getCreatedByUserId());
			r.setCreatedByRole(reservation.getCreatedByRole());

			int id = db.createReceiptIfNotExists(r);
			if (id > 0)
				r.setReceiptId(id);

			return db.getReceiptByReservationId(reservation.getReservationId());

		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Retrieves a receipt by its reservation ID.
	 *
	 * @param reservationId the reservation identifier
	 * @return the corresponding receipt, or {@code null} if not found or on error
	 */
	public Receipt getReceiptByReservationId(int reservationId) {
		try {
			return db.getReceiptByReservationId(reservationId);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Marks a receipt as paid in the database.
	 *
	 * @param reservationId the reservation associated with the receipt
	 * @param type          the payment method used
	 * @param paidAt        the payment timestamp
	 * @return {@code true} if the receipt was successfully marked as paid,
	 *         {@code false} otherwise
	 */
	public boolean markPaid(int reservationId, Enums.TypeOfPayment type, LocalDateTime paidAt) {
		try {
			return db.markReceiptPaid(reservationId, type, paidAt);
		} catch (Exception e) {
			return false;
		}
	}
}
