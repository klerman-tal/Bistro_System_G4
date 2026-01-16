package logicControllers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dbControllers.Receipt_DB_Controller;
import entities.Enums;
import entities.Receipt;
import entities.Reservation;

public class ReceiptController {

    private final Receipt_DB_Controller db;

    public ReceiptController(Receipt_DB_Controller db) {
        this.db = db;
    }

    /**
     * Creates a receipt at check-in if missing (idempotent).
     * Uses a simple random amount (50..300) for the project.
     */
    public Receipt createReceiptIfMissingForCheckin(Reservation reservation, LocalDateTime checkinTime) {

        if (reservation == null) return null;
        if (reservation.getReservationId() <= 0) return null;

        try {
            Receipt existing = db.getReceiptByReservationId(reservation.getReservationId());
            if (existing != null) return existing;

            Receipt r = new Receipt();
            r.setReservationId(reservation.getReservationId());
            r.setCreatedAt(checkinTime != null ? checkinTime : LocalDateTime.now());

            BigDecimal amount = BigDecimal.valueOf(50 + (int)(Math.random() * 251)); // 50..300
            r.setAmount(amount);

            r.setCreatedByUserId(reservation.getCreatedByUserId());
            r.setCreatedByRole(reservation.getCreatedByRole());

            int id = db.createReceiptIfNotExists(r);
            if (id > 0) r.setReceiptId(id);

            return db.getReceiptByReservationId(reservation.getReservationId());

        } catch (Exception e) {
            return null;
        }
    }

    public Receipt getReceiptByReservationId(int reservationId) {
        try {
            return db.getReceiptByReservationId(reservationId);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean markPaid(int reservationId, Enums.TypeOfPayment type, LocalDateTime paidAt) {
        try {
            return db.markReceiptPaid(reservationId, type, paidAt);
        } catch (Exception e) {
            return false;
        }
    }
}
