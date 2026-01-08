package logicControllers;

import java.time.LocalDateTime;

import entities.Enums;
import entities.Reservation;

public class PaymentController {

    private static final int ORDER_PRICE = 200;

    private final ReservationController reservationController;
    private final RestaurantController restaurantController;

    /**
     * Injected once when server starts.
     */
    public PaymentController(ReservationController reservationController,
                             RestaurantController restaurantController) {
        this.reservationController = reservationController;
        this.restaurantController = restaurantController;
    }

    /**
     * Full payment flow:
     * - calculate amount
     * - update reservation status + checkout in DB
     * - free table if paid before 2 hours passed
     */
    public double processPayment(Reservation reservation) {

        if (reservation == null) {
            return 0;
        }

        // 1️⃣ Calculate final amount
        double finalAmount;
        if (reservation.getCreatedByRole() == Enums.UserRole.Subscriber) {
            finalAmount = ORDER_PRICE * 0.9;
        } else {
            finalAmount = ORDER_PRICE;
        }

        // 2️⃣ Update checkout time
        LocalDateTime checkoutTime = LocalDateTime.now();
        reservation.setCheckoutTime(checkoutTime);

        reservationController.updateCheckoutTime(
                reservation.getReservationId(),
                checkoutTime
        );

        // 3️⃣ Update reservation status to FINISHED
        reservation.setReservationStatus(Enums.ReservationStatus.Finished);

        reservationController.updateReservationStatus(
                reservation.getReservationId(),
                Enums.ReservationStatus.Finished
        );

        // 4️⃣ Free table if paid before 2 hours passed
        freeTableIfPaidEarly(reservation, checkoutTime);

        return finalAmount;
    }

    /**
     * Frees future slots only if payment happened
     * before the original 2-hour reservation window ended.
     */
    private void freeTableIfPaidEarly(Reservation reservation, LocalDateTime checkoutTime) {

        LocalDateTime checkinTime = reservation.getCheckinTime();
        Integer tableNumber = reservation.getTableNumber();

        if (checkinTime == null || tableNumber == null) {
            return;
        }

        // If already passed 2 hours – do nothing
        if (checkoutTime.isAfter(checkinTime.plusHours(2))) {
            return;
        }

        // Release only future slots (up to 2 hours from check-in)
        for (int i = 0; i < 4; i++) {
            LocalDateTime slot = checkinTime.plusMinutes(30L * i);

            if (slot.isAfter(checkoutTime)) {
                try {
                    restaurantController.releaseSlot(slot, tableNumber);
                } catch (Exception ignore) {
                    // as requested – no logs, no handling
                }
            }
        }
    }
}
