package logicControllers;

import java.time.LocalDateTime;

import entities.Enums;
import entities.Reservation;

/**
 * Handles the business logic related to reservation payments.
 * <p>
 * This controller is responsible for processing a full payment flow, including
 * price calculation, reservation state updates, checkout handling, and
 * conditional release of table availability.
 * </p>
 * <p>
 * The payment logic is independent of the actual payment method (credit card /
 * cash) and focuses only on server-side state management.
 * </p>
 */
public class PaymentController {

	/**
	 * Fixed base price for a single reservation order.
	 */
	private static final int ORDER_PRICE = 200;

	private final ReservationController reservationController;
	private final RestaurantController restaurantController;

	/**
	 * Constructs a PaymentController with required controller dependencies.
	 * <p>
	 * This controller is typically instantiated once during server startup and
	 * reused for all payment operations.
	 * </p>
	 *
	 * @param reservationController controller responsible for reservation updates
	 * @param restaurantController  controller responsible for table availability
	 */
	public PaymentController(ReservationController reservationController, RestaurantController restaurantController) {
		this.reservationController = reservationController;
		this.restaurantController = restaurantController;
	}

	/**
	 * Processes the full payment flow for a reservation.
	 * <p>
	 * The method performs the following steps:
	 * <ol>
	 * <li>Calculates the final payment amount (including subscriber discount)</li>
	 * <li>Updates the reservation checkout time</li>
	 * <li>Marks the reservation as finished</li>
	 * <li>Releases future table slots if payment occurred before the 2-hour window
	 * ended</li>
	 * </ol>
	 * </p>
	 *
	 * @param reservation the reservation being paid
	 * @return the final amount charged for the reservation
	 */
	public double processPayment(Reservation reservation) {

		if (reservation == null) {
			return 0;
		}

		// 1) Calculate final amount
		double finalAmount;
		if (reservation.getCreatedByRole() != Enums.UserRole.RandomClient) {
			finalAmount = ORDER_PRICE * 0.9;
		} else {
			finalAmount = ORDER_PRICE;
		}

		// 2) Update checkout time
		LocalDateTime checkoutTime = LocalDateTime.now();
		reservation.setCheckoutTime(checkoutTime);

		reservationController.updateCheckoutTime(reservation.getReservationId(), checkoutTime);

		// 3) Update reservation status to FINISHED
		reservation.setReservationStatus(Enums.ReservationStatus.Finished);

		reservationController.updateReservationStatus(reservation.getReservationId(), Enums.ReservationStatus.Finished);

		// 4) Free table slots if payment was completed early
		freeTableIfPaidEarly(reservation, checkoutTime);

		return finalAmount;
	}

	/**
	 * Releases future table availability slots if the reservation was paid before
	 * the original 2-hour reservation window ended.
	 * <p>
	 * Only slots occurring after the checkout time and within the original
	 * reservation duration are released.
	 * </p>
	 *
	 * @param reservation  the reservation being checked out
	 * @param checkoutTime the actual checkout time
	 */
	private void freeTableIfPaidEarly(Reservation reservation, LocalDateTime checkoutTime) {

		LocalDateTime checkinTime = reservation.getCheckinTime();
		Integer tableNumber = reservation.getTableNumber();

		if (checkinTime == null || tableNumber == null) {
			return;
		}

		// If already passed the 2-hour window – do nothing
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
					// intentionally ignored as per business requirements
				}
			}
		}
	}
}
