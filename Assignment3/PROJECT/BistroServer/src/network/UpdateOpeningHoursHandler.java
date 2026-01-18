package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import dto.UpdateOpeningHoursDTO;
import entities.OpeningHouers;
import logicControllers.ReservationController;
import logicControllers.RestaurantController;
import ocsf.server.ConnectionToClient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Server-side request handler responsible for updating the restaurant's weekly
 * opening hours.
 * <p>
 * This handler performs a multi-step update process:
 * <ul>
 * <li>Updates the weekly opening hours definition in the database</li>
 * <li>Synchronizes availability grids for the next 30 days</li>
 * <li>Cancels existing reservations that conflict with the new hours</li>
 * <li>Returns the updated opening hours list to refresh the client UI</li>
 * </ul>
 * </p>
 * <p>
 * The logic ensures data consistency between weekly rules, special dates,
 * availability grids, and existing reservations.
 * </p>
 */
public class UpdateOpeningHoursHandler implements RequestHandler {

	private final RestaurantController restaurantController;
	private final ReservationController reservationController;

	/**
	 * Constructs a handler with required controller dependencies.
	 *
	 * @param restaurantController  controller responsible for opening hours and
	 *                              availability
	 * @param reservationController controller responsible for reservation
	 *                              management
	 */
	public UpdateOpeningHoursHandler(RestaurantController restaurantController,
			ReservationController reservationController) {
		this.restaurantController = restaurantController;
		this.reservationController = reservationController;
	}

	/**
	 * Handles an opening-hours update request.
	 * <p>
	 * The method executes a complex update flow:
	 * <ol>
	 * <li>Validates the incoming update payload</li>
	 * <li>Updates weekly opening hours in the database</li>
	 * <li>Applies the change to all matching weekdays in the next 30 days</li>
	 * <li>Synchronizes availability grids and cancels conflicting reservations</li>
	 * <li>Returns the refreshed opening hours list to the client</li>
	 * </ol>
	 * </p>
	 */
	@Override
	public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

		if (request == null || request.getData() == null) {
			client.sendToClient(new ResponseDTO(false, "Missing update data", null));
			return;
		}

		if (!(request.getData() instanceof UpdateOpeningHoursDTO data)) {
			client.sendToClient(new ResponseDTO(false, "Invalid update payload", null));
			return;
		}

		// 1) Update weekly hours in DB
		OpeningHouers oh = new OpeningHouers();
		oh.setDayOfWeek(data.getDayOfWeek());
		oh.setOpenTime(data.getOpenTime()); // null/blank => CLOSED
		oh.setCloseTime(data.getCloseTime()); // null/blank => CLOSED

		try {
			restaurantController.updateOpeningHours(oh);
		} catch (Exception e) {
			client.sendToClient(new ResponseDTO(false, "Failed to update opening hours: " + e.getMessage(), null));
			return;
		}

		// 2) Apply the change for all matching weekdays in the next 30 days
		int syncedDays = 0;
		int cancelledReservations = 0;
		int errors = 0;

		try {
			List<LocalDate> dates = restaurantController.getDatesForWeekdayNext30Days(data.getDayOfWeek());

			for (LocalDate d : dates) {
				try {
					restaurantController.syncGridForSpecialDate(d);
					syncedDays++;

					OpeningHouers eff = restaurantController.getEffectiveOpeningHoursForDatePublic(d);

					boolean isClosed = (eff == null) || isBlank(eff.getOpenTime()) || isBlank(eff.getCloseTime());

					LocalTime open = null;
					LocalTime close = null;

					if (!isClosed) {
						open = LocalTime.parse(restaurantController.toHHMM(eff.getOpenTime()));
						close = LocalTime.parse(restaurantController.toHHMM(eff.getCloseTime()));
					}

					cancelledReservations += reservationController.cancelReservationsDueToOpeningHoursChange(d, open,
							close, isClosed);

				} catch (Exception perDateEx) {
					errors++;
				}
			}
		} catch (Exception loopEx) {
			client.sendToClient(new ResponseDTO(false,
					"Opening hours updated, but failed applying changes to next 30 days: " + loopEx.getMessage(),
					null));
			return;
		}

		// 3) Return updated opening hours list so GUI refreshes immediately
		ArrayList<OpeningHouers> updatedList;
		try {
			updatedList = restaurantController.getOpeningHours();
		} catch (Exception e) {
			String msg = "Opening hours updated. Synced days=" + syncedDays + ", Cancelled reservations="
					+ cancelledReservations + (errors > 0 ? (", Errors=" + errors) : "") + ". (Failed to reload list: "
					+ e.getMessage() + ")";
			client.sendToClient(new ResponseDTO(true, msg, null));
			return;
		}

		String msg = "Opening hours updated. Synced days=" + syncedDays + ", Cancelled reservations="
				+ cancelledReservations + (errors > 0 ? (", Errors=" + errors) : "");

		client.sendToClient(new ResponseDTO(true, msg, updatedList));
	}

	/**
	 * Utility method used to determine whether a string is null, empty, or contains
	 * only whitespace.
	 *
	 * @param s the string to check
	 * @return {@code true} if the string is blank or null
	 */
	private boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}
}
