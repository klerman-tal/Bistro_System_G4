package logicControllers;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

import application.RestaurantServer;
import dbControllers.Notification_DB_Controller;
import dbControllers.Receipt_DB_Controller;
import dbControllers.Reservation_DB_Controller;
import dto.CreateReservationDTO;
import entities.Enums.ReservationStatus;
import entities.Reservation;
import entities.Restaurant;
import entities.Table;
import entities.User;
import entities.Notification;
import entities.Enums;
import logicControllers.WaitingController;
import dto.GetTableResultDTO;

/**
 * Handles reservation-related business logic and orchestration.
 * <p>
 * This controller acts as the main logic layer for creating, cancelling,
 * finishing, and querying reservations. It coordinates between:
 * <ul>
 * <li>{@link Reservation_DB_Controller} for reservation persistence</li>
 * <li>{@link RestaurantController} for table availability and slot
 * locking/releasing</li>
 * <li>{@link Notification_DB_Controller} for scheduling customer
 * notifications</li>
 * <li>{@link WaitingController} for waiting-list handling when a table becomes
 * available</li>
 * <li>{@link ReceiptController} for creating receipts at check-in time</li>
 * </ul>
 * </p>
 * <p>
 * The controller also manages pending check-ins: if a customer arrives within
 * the allowed check-in window but the assigned table is still occupied, the
 * check-in request is stored and the customer is notified when the table is
 * freed.
 * </p>
 */
public class ReservationController {

	private final Reservation_DB_Controller db;
	private final Notification_DB_Controller notificationDB;
	private final Restaurant restaurant;
	private final RestaurantServer server;
	private final RestaurantController restaurantController;
	private final ReceiptController receiptController;

	private WaitingController waitingController;
	private final Map<Integer, PendingReservationCheckin> pendingCheckins = new ConcurrentHashMap<>();

	/**
	 * Holds a pending check-in request for a reservation whose assigned table is
	 * currently occupied.
	 * <p>
	 * When the table is freed, the controller sends a notification to the user so
	 * they can proceed with check-in using their confirmation code.
	 * </p>
	 */
	private static class PendingReservationCheckin {
		final int reservationId;
		final int userId;
		final String confirmationCode;
		final int tableNumber;

		/**
		 * Constructs a pending check-in record.
		 *
		 * @param reservationId    reservation identifier
		 * @param userId           identifier of the user who created the reservation
		 * @param confirmationCode reservation confirmation code
		 * @param tableNumber      assigned table number
		 */
		PendingReservationCheckin(int reservationId, int userId, String confirmationCode, int tableNumber) {
			this.reservationId = reservationId;
			this.userId = userId;
			this.confirmationCode = confirmationCode;
			this.tableNumber = tableNumber;
		}
	}

	/**
	 * Constructs a ReservationController and connects it to the DB layer, server
	 * logger, and table availability logic.
	 *
	 * @param db                database controller used for reservation persistence
	 * @param notificationDB    database controller used for scheduling and
	 *                          persisting notifications
	 * @param server            server instance used for logging
	 * @param rc                restaurant logic controller used for availability
	 *                          and slot management
	 * @param receiptController logic controller used for receipt creation at
	 *                          check-in
	 */
	public ReservationController(Reservation_DB_Controller db, Notification_DB_Controller notificationDB,
			RestaurantServer server, RestaurantController rc, ReceiptController receiptController) {

		this.db = db;
		this.notificationDB = notificationDB;
		this.server = server;
		this.restaurant = Restaurant.getInstance();
		this.restaurantController = rc;
		this.receiptController = receiptController;
	}

	/**
	 * Connects the waiting controller after construction.
	 * <p>
	 * This setter exists to avoid circular constructor dependencies between
	 * controllers.
	 * </p>
	 *
	 * @param waitingController waiting-list controller to be used for "table freed"
	 *                          scenarios
	 */
	public void setWaitingController(WaitingController waitingController) {
		this.waitingController = waitingController;
	}

	// ====NOTIFICATIONS (SCHEDULED)====

	/**
	 * Schedules reminder notifications (SMS and Email) two hours before the
	 * reservation time.
	 * <p>
	 * The notification content follows the format:
	 * {@code "Reminder: Your reservation is in 2 hours. Confirmation code: <CODE>"}.
	 * </p>
	 * <p>
	 * If the scheduled time is already in the past, the reminder is not scheduled.
	 * </p>
	 *
	 * @param userId              the user who should receive the reminder
	 * @param reservationDateTime the reservation date and time
	 * @param confirmationCode    reservation confirmation code to include in the
	 *                            reminder
	 */
	private void scheduleReservationReminder2HoursBefore(int userId, LocalDateTime reservationDateTime,
			String confirmationCode) {
		try {
			if (notificationDB == null)
				return;
			if (reservationDateTime == null)
				return;
			if (confirmationCode == null || confirmationCode.isBlank())
				return;

			LocalDateTime scheduledFor = reservationDateTime.minusHours(2);

			// If it's already too late, skip scheduling
			if (scheduledFor.isBefore(LocalDateTime.now())) {
				return;
			}

			String smsBody = "Reminder: Your reservation is in 2 hours. Confirmation code: " + confirmationCode;
			String emailBody = "Reminder: Your reservation is in 2 hours. Confirmation code: " + confirmationCode;

			notificationDB.addNotification(new Notification(userId, Enums.Channel.SMS,
					Enums.NotificationType.RESERVATION_REMINDER_2H, smsBody, scheduledFor));

			notificationDB.addNotification(new Notification(userId, Enums.Channel.EMAIL,
					Enums.NotificationType.RESERVATION_REMINDER_2H, emailBody, scheduledFor));

		} catch (Exception e) {
			server.log("ERROR: Failed to schedule 2h reminder. UserId=" + userId + ", Msg=" + e.getMessage());
		}
	}

	// ====AVAILABILITY HELPERS====

	/**
	 * Fills an output list with available reservation start times for a specific
	 * day.
	 * <p>
	 * The method uses
	 * {@link RestaurantController#getOneAvailableTablePerSlot(LocalDate, int)} to
	 * get candidate slots, then filters them by:
	 * <ul>
	 * <li>optional {@code fromTime} constraint</li>
	 * <li>table existence per slot</li>
	 * <li>free-for-two-hours validation using
	 * {@link RestaurantController#isTableFreeForTwoHours(LocalDateTime, int)}</li>
	 * </ul>
	 * </p>
	 *
	 * @param date         the requested reservation date
	 * @param guestsNumber number of guests
	 * @param fromTime     optional lower bound for the returned times (inclusive);
	 *                     may be {@code null}
	 * @param out          list to populate with available times (cleared before
	 *                     use)
	 */
	private void fillAvailableTimesForDay(LocalDate date, int guestsNumber, LocalTime fromTime,
			ArrayList<LocalTime> out) {

		if (out == null)
			return;
		out.clear();

		try {
			Map<LocalDateTime, Table> map = restaurantController.getOneAvailableTablePerSlot(date, guestsNumber);

			ArrayList<LocalDateTime> times = new ArrayList<>(map.keySet());
			times.sort(Comparator.naturalOrder());

			for (LocalDateTime dt : times) {
				LocalTime t = dt.toLocalTime();

				if (fromTime != null && t.isBefore(fromTime))
					continue;

				Table table = map.get(dt);
				if (table == null)
					continue;

				if (restaurantController.isTableFreeForTwoHours(dt, table.getTableNumber())) {
					out.add(t);
				}
			}

		} catch (Exception e) {
			server.log("ERROR: Failed to load available times. " + e.getMessage());
		}
	}

	/**
	 * Attempts to reserve a two-hour window starting at {@code start} for the given
	 * table.
	 * <p>
	 * The two-hour window is represented by 4 consecutive 30-minute slots. If any
	 * slot cannot be locked, all previously locked slots are released.
	 * </p>
	 *
	 * @param start       start date-time of the reservation
	 * @param tableNumber table number to lock
	 * @return {@code true} if all 4 slots were successfully locked, {@code false}
	 *         otherwise
	 */
	private boolean tryReserveForTwoHours(LocalDateTime start, int tableNumber) {
		ArrayList<LocalDateTime> locked = new ArrayList<>();
		LocalDateTime slot = start;

		try {
			for (int i = 0; i < 4; i++) {
				boolean ok = restaurantController.tryReserveSlot(slot, tableNumber);
				if (!ok) {
					for (LocalDateTime s : locked) {
						try {
							restaurantController.releaseSlot(s, tableNumber);
						} catch (Exception ignore) {
						}
					}
					return false;
				}

				locked.add(slot);
				slot = slot.plusMinutes(30);
			}

			return true;

		} catch (Exception e) {
			for (LocalDateTime s : locked) {
				try {
					restaurantController.releaseSlot(s, tableNumber);
				} catch (Exception ignore) {
				}
			}

			server.log("ERROR: Failed to reserve 2 hours slots. " + e.getMessage());
			return false;
		}
	}

	// =====================================================
	// TABLE FREED -> WAITING HOOK
	// =====================================================

	/**
	 * After a reservation cancellation/finish releases a table, triggers
	 * waiting-list logic.
	 * <p>
	 * The method attempts to resolve the freed {@link Table} from the
	 * {@link Restaurant} cache. If not found, it falls back to a minimal
	 * {@link Table} instance (with a best-effort seats amount).
	 * </p>
	 *
	 * @param tableNumber freed table number
	 */
	private void notifyWaitingTableFreed(Integer tableNumber) {
		if (waitingController == null)
			return;
		if (tableNumber == null)
			return;

		try {
			// Try to find the real table object from Restaurant cache (so we have correct
			// seats_amount)
			Table freed = null;

			try {
				// If your Restaurant singleton keeps tables in memory, use it
				// (common in your project because you load tables into Restaurant cache).
				for (Table t : restaurant.getTables()) {
					if (t != null && t.getTableNumber() == tableNumber) {
						freed = t;
						break;
					}
				}
			} catch (Exception ignore) {
			}

			// Fallback: minimal table (won't crash, but seats may be missing if cache isn't
			// loaded)
			if (freed == null) {
				freed = new Table();
				freed.setTableNumber(tableNumber);
				// IMPORTANT: if you reach here and seats_amount is needed, ensure Restaurant
				// cache is loaded.
				freed.setSeatsAmount(Integer.MAX_VALUE);
			}

			waitingController.handleTableFreed(freed);

		} catch (Exception e) {
			server.log("ERROR: notifyWaitingTableFreed failed. Table=" + tableNumber + ", Msg=" + e.getMessage());
		}
	}

	// ====CREATE / CANCEL RESERVATION====

	/**
	 * Creates a new table reservation for a specific date/time and guest count.
	 * <p>
	 * Rules enforced by this method:
	 * <ul>
	 * <li>Reservation time must be at least 1 hour from now</li>
	 * <li>Reservation time must be within the next month</li>
	 * <li>Assigned table must be available for a full two-hour window (4
	 * slots)</li>
	 * </ul>
	 * </p>
	 * <p>
	 * If the requested time is unavailable, the method can optionally fill
	 * {@code availableTimesOut} with alternative times on the same day.
	 * </p>
	 * <p>
	 * On successful creation, the method schedules a 2-hour reminder notification.
	 * </p>
	 *
	 * @param dto               request data for creating the reservation
	 * @param availableTimesOut optional output list that will be populated with
	 *                          alternative times on failure
	 * @return a created {@link Reservation} instance on success, or {@code null} on
	 *         failure
	 */
	public Reservation CreateTableReservation(CreateReservationDTO dto, ArrayList<LocalTime> availableTimesOut) {

		if (availableTimesOut != null)
			availableTimesOut.clear();

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime requested = LocalDateTime.of(dto.getDate(), dto.getTime());

		if (requested.isBefore(now.plusHours(1)) || requested.isAfter(now.plusMonths(1))) {
			server.log("WARN: Invalid reservation time. UserId=" + dto.getUserId());
			return null;
		}

		Table table;
		try {
			table = restaurantController.getOneAvailableTableAt(requested, dto.getGuests());
		} catch (Exception e) {
			server.log("ERROR: Failed to check availability. " + e.getMessage());
			return null;
		}

		if (table == null) {
			fillAvailableTimesForDay(dto.getDate(), dto.getGuests(), dto.getTime(), availableTimesOut);
			return null;
		}

		boolean locked2h = tryReserveForTwoHours(requested, table.getTableNumber());
		if (!locked2h) {
			fillAvailableTimesForDay(dto.getDate(), dto.getGuests(), dto.getTime(), availableTimesOut);
			return null;
		}

		Reservation res = new Reservation();
		res.setCreatedByUserId(dto.getUserId());
		res.setGuestAmount(dto.getGuests());
		res.setReservationTime(requested);
		res.setConfirmed(true);
		res.setTableNumber(table.getTableNumber());
		res.setActive(true);
		res.setReservationStatus(ReservationStatus.Active);
		res.setCreatedByRole(dto.getRole());
		res.generateAndSetConfirmationCode();

		try {
			int reservationId = db.addReservation(res.getReservationTime(), dto.getGuests(), res.getConfirmationCode(),
					dto.getUserId(), dto.getRole(), table.getTableNumber());

			if (reservationId == -1) {
				rollbackReservation(requested, table.getTableNumber());
				return null;
			}
			res.setReservationId(reservationId);

			// schedule reminder 2 hours before
			scheduleReservationReminder2HoursBefore(dto.getUserId(), requested, res.getConfirmationCode());

		} catch (SQLException e) {
			server.log("ERROR: DB Insert failed: " + e.getMessage());
			rollbackReservation(requested, table.getTableNumber());
			return null;
		}

		return res;
	}

	/**
	 * Relocates or cancels upcoming active reservations when a table is deleted.
	 * <p>
	 * For each active reservation of the deleted table within the next
	 * {@code days}:
	 * <ul>
	 * <li>Try to find an alternative available table (excluding the deleted
	 * one)</li>
	 * <li>If no alternative exists or locking fails, cancel the reservation</li>
	 * <li>If relocation succeeds, update the reservation table number in the
	 * database</li>
	 * </ul>
	 * </p>
	 *
	 * @param tableNumber deleted table number
	 * @param days        how many days ahead to scan for affected reservations
	 * @return number of reservations that were cancelled or successfully relocated
	 */
	public int relocateOrCancelReservationsForDeletedTable(int tableNumber, int days) {
		int affected = 0;

		try {
			ArrayList<Reservation> list = db.getActiveReservationsForTableInNextDays(tableNumber, days);

			for (Reservation r : list) {
				if (r == null || r.getReservationTime() == null)
					continue;

				LocalDateTime start = r.getReservationTime();
				int guests = r.getGuestAmount();

				Table newTable = restaurantController.getOneAvailableTableAtExcluding(start, guests, tableNumber);

				if (newTable == null) {
					server.log("DELETE TABLE: Cancelling reservation (no alternative table). Code="
							+ r.getConfirmationCode() + " Time=" + start + " OldTable=" + tableNumber);

					CancelReservation(r.getConfirmationCode()); // משחרר סלוטים + מעדכן DB
					affected++;
					continue;
				}

				int newTableNum = newTable.getTableNumber();

				boolean locked = tryReserveForTwoHours(start, newTableNum);
				if (!locked) {
					server.log("DELETE TABLE: Cancelling reservation (failed locking new table). Code="
							+ r.getConfirmationCode() + " Time=" + start + " NewTable=" + newTableNum);

					CancelReservation(r.getConfirmationCode());
					affected++;
					continue;
				}

				for (int i = 0; i < 4; i++) {
					try {
						restaurantController.releaseSlot(start.plusMinutes(30L * i), tableNumber);
					} catch (Exception ignore) {
					}
				}

				boolean updated = db.updateReservationTableNumber(r.getReservationId(), newTableNum);
				if (!updated) {
					for (int i = 0; i < 4; i++) {
						try {
							restaurantController.releaseSlot(start.plusMinutes(30L * i), newTableNum);
						} catch (Exception ignore) {
						}
					}
					for (int i = 0; i < 4; i++) {
						try {
							restaurantController.tryReserveSlot(start.plusMinutes(30L * i), tableNumber);
						} catch (Exception ignore) {
						}
					}

					server.log("DELETE TABLE: Failed updating reservation table_number -> kept original. Code="
							+ r.getConfirmationCode());
					continue;
				}

				server.log("DELETE TABLE: Reservation relocated. Code=" + r.getConfirmationCode() + " Time=" + start
						+ " " + tableNumber + " -> " + newTableNum);

				affected++;
			}

		} catch (Exception e) {
			server.log("ERROR: relocateOrCancelReservationsForDeletedTable failed. " + e.getMessage());
		}

		return affected;
	}

	/**
	 * Creates a reservation directly from the waiting list flow.
	 * <p>
	 * The method locks a two-hour window for the specified table, inserts a
	 * reservation row, and schedules the 2-hour reminder notification.
	 * </p>
	 *
	 * @param confirmationCode reservation confirmation code
	 * @param start            reservation start time
	 * @param guests           number of guests
	 * @param user             user creating the reservation
	 * @param tableNumber      table number to assign
	 * @return {@code true} if the reservation was created successfully,
	 *         {@code false} otherwise
	 */
	public boolean createReservationFromWaiting(String confirmationCode, LocalDateTime start, int guests, User user,
			int tableNumber) {

		if (confirmationCode == null || confirmationCode.isBlank())
			return false;
		if (start == null || user == null)
			return false;

		boolean locked2h = tryReserveForTwoHours(start, tableNumber);
		if (!locked2h)
			return false;

		try {
			int reservationId = db.addReservation(start, guests, confirmationCode.trim(), user.getUserId(),
					user.getUserRole(), tableNumber);

			if (reservationId == -1) {
				rollbackReservation(start, tableNumber);
				return false;
			}

			db.updateCheckinTime(reservationId, LocalDateTime.now());

			scheduleReservationReminder2HoursBefore(user.getUserId(), start, confirmationCode.trim());

			server.log("Reservation created from waiting. Code=" + confirmationCode + ", Table=" + tableNumber
					+ ", ReservationId=" + reservationId);
			return true;

		} catch (Exception e) {
			rollbackReservation(start, tableNumber);
			server.log(
					"ERROR: createReservationFromWaiting failed. Code=" + confirmationCode + ", Msg=" + e.getMessage());
			return false;
		}
	}

	/**
	 * Cancels an active reservation by its confirmation code.
	 * <p>
	 * The cancellation process:
	 * <ul>
	 * <li>Validates that the reservation exists and is active</li>
	 * <li>Releases the two-hour slot window (4 slots) for the assigned table</li>
	 * <li>Updates the database to mark the reservation as cancelled</li>
	 * <li>Schedules a cancellation notification and triggers
	 * waiting/pending-checkin handlers</li>
	 * </ul>
	 * </p>
	 *
	 * @param confirmationCode reservation confirmation code
	 * @return {@code true} if successfully cancelled, {@code false} otherwise
	 */
	public boolean CancelReservation(String confirmationCode) {

		try {
			Reservation r = db.getReservationByConfirmationCode(confirmationCode);
			if (r == null || !r.isActive() || r.getReservationStatus() != ReservationStatus.Active) {
				server.log("WARN: Cancel request invalid/inactive. Code=" + confirmationCode);
				return false;
			}

			Integer tableNum = r.getTableNumber();
			LocalDateTime start = r.getReservationTime();

			if (tableNum == null || start == null) {
				server.log("ERROR: Cancel failed - missing table/reservation time. Code=" + confirmationCode);
				return false;
			}

			// 1) release 2 hours (4 slots)
			for (int i = 0; i < 4; i++) {
				try {
					restaurantController.releaseSlot(start.plusMinutes(30L * i), tableNum);
				} catch (Exception e) {
					server.log("ERROR: Failed releasing slot during cancel. Slot=" + start.plusMinutes(30L * i)
							+ ", Table=" + tableNum + ", Msg=" + e.getMessage());
				}
			}

			// 2) update DB
			boolean cancelled = db.cancelReservationByConfirmationCode(confirmationCode);

			if (cancelled) {
				scheduleReservationCancelledPopupForLogin(r, "Your reservation was cancelled.");
				server.log("Reservation canceled. Code=" + confirmationCode);

				notifyWaitingTableFreed(tableNum);
				notifyPendingReservationCheckins(tableNum);
				return true;
			}

			server.log("WARN: Cancel DB update did not affect row. Code=" + confirmationCode);
			return false;

		} catch (SQLException e) {
			server.log("ERROR: Failed to cancel reservation. Code=" + confirmationCode + ", Message=" + e.getMessage());
			return false;
		}
	}

	/**
	 * Schedules an immediate cancellation notification (shown on user login) for a
	 * cancelled reservation.
	 *
	 * @param r      cancelled reservation
	 * @param reason optional cancellation reason message
	 */
	private void scheduleReservationCancelledPopupForLogin(Reservation r, String reason) {
		if (notificationDB == null || r == null)
			return;

		try {
			LocalDateTime now = LocalDateTime.now();

			String smsBody = "Your reservation was cancelled. Confirmation code: " + r.getConfirmationCode()
					+ (reason != null && !reason.isBlank() ? " Reason: " + reason : "");

			notificationDB.addNotification(new Notification(r.getCreatedByUserId(), Enums.Channel.SMS,
					Enums.NotificationType.RESERVATION_CANCELLED, // תוסיפי ENUM כזה
					smsBody, now));

		} catch (Exception e) {
			server.log("ERROR: scheduleReservationCancelledPopupForLogin failed: " + e.getMessage());
		}
	}

	/**
	 * Rolls back a reservation attempt by releasing the two-hour slot window (4
	 * slots).
	 *
	 * @param requested   requested reservation time
	 * @param tableNumber table number whose slots should be released
	 */
	private void rollbackReservation(LocalDateTime requested, int tableNumber) {
		server.log("Rolling back: releasing 2 hours (4 slots) for table " + tableNumber);
		for (int i = 0; i < 4; i++) {
			try {
				restaurantController.releaseSlot(requested.plusMinutes(30L * i), tableNumber);
			} catch (Exception ignore) {
			}
		}
	}

	/**
	 * Returns current diners (checked-in but not checked-out).
	 *
	 * @return list of current diners; empty list on error
	 */
	public ArrayList<Reservation> getCurrentDiners() {
		try {
			return db.getCurrentDiners();
		} catch (SQLException e) {
			server.log("ERROR: Failed to load current diners. Msg=" + e.getMessage());
			return new ArrayList<>();
		}
	}

	// ====READ (QUERIES)====

	/**
	 * Retrieves all reservations created by a specific user.
	 *
	 * @param userId user identifier
	 * @return list of reservations for the user; empty list on error
	 */
	public ArrayList<Reservation> getReservationsForUser(int userId) {
		try {
			return db.getReservationsByUser(userId);
		} catch (SQLException e) {
			server.log("ERROR: Failed to load reservations for user. UserId=" + userId + ", Message=" + e.getMessage());
			return new ArrayList<>();
		}
	}

	/**
	 * Retrieves all active reservations.
	 *
	 * @return list of active reservations, or {@code null} on database error
	 */
	public ArrayList<Reservation> getAllActiveReservations() {
		try {
			return db.getActiveReservations();
		} catch (SQLException e) {
			server.log("ERROR: Failed to load active reservations. Message=" + e.getMessage());
			return null;
		}
	}

	/**
	 * Retrieves full reservations history from the database.
	 *
	 * @return list of all reservations, or {@code null} on database error
	 */
	public ArrayList<Reservation> getAllReservationsHistory() {
		try {
			return db.getAllReservations();
		} catch (SQLException e) {
			server.log("ERROR: Failed to load reservations history. Message=" + e.getMessage());
			return null;
		}
	}

	/**
	 * Finds a reservation by its confirmation code.
	 *
	 * @param confirmationCode reservation confirmation code
	 * @return matching reservation, or {@code null} if not found / on error
	 */
	public Reservation getReservationByConfirmationCode(String confirmationCode) {
		try {
			return db.getReservationByConfirmationCode(confirmationCode);
		} catch (SQLException e) {
			server.log("ERROR: Failed to find reservation by confirmation code. Code=" + confirmationCode + ", Message="
					+ e.getMessage());
			return null;
		}
	}

	// ====UPDATE====

	/**
	 * Updates the reservation status in the database.
	 *
	 * @param reservationId reservation identifier
	 * @param status        new reservation status
	 * @return {@code true} if updated successfully, {@code false} otherwise
	 */
	public boolean updateReservationStatus(int reservationId, ReservationStatus status) {
		try {
			boolean updated = db.updateReservationStatus(reservationId, status);

			if (!updated) {
				server.log("WARN: Reservation not found. Status not updated. ReservationId=" + reservationId);
				return false;
			}

			server.log("Reservation status updated. ReservationId=" + reservationId + ", Status=" + status);
			return true;

		} catch (Exception e) {
			server.log("ERROR: Failed to update reservation status. ReservationId=" + reservationId + ", Message="
					+ e.getMessage());
			return false;
		}
	}

	/**
	 * Updates the confirmation flag ({@code is_confirmed}) for a reservation.
	 *
	 * @param reservationId reservation identifier
	 * @param isConfirmed   new confirmation value
	 * @return {@code true} if updated successfully, {@code false} otherwise
	 */
	public boolean updateReservationConfirmation(int reservationId, boolean isConfirmed) {
		try {
			boolean updated = db.updateIsConfirmed(reservationId, isConfirmed);

			if (!updated) {
				server.log("WARN: Reservation not found. is_confirmed not updated. ReservationId=" + reservationId);
				return false;
			}

			server.log("Reservation confirmation updated. ReservationId=" + reservationId + ", isConfirmed="
					+ isConfirmed);
			return true;

		} catch (Exception e) {
			server.log("ERROR: Failed to update reservation confirmation. ReservationId=" + reservationId + ", Message="
					+ e.getMessage());
			return false;
		}
	}

	/**
	 * Updates the check-in time for a reservation.
	 *
	 * @param reservationId reservation identifier
	 * @param checkinTime   check-in timestamp to set
	 * @return {@code true} if updated successfully, {@code false} otherwise
	 */
	public boolean updateCheckinTime(int reservationId, LocalDateTime checkinTime) {
		try {
			boolean updated = db.updateCheckinTime(reservationId, checkinTime);

			if (!updated) {
				server.log("WARN: Reservation not found. Check-in not updated. ReservationId=" + reservationId);
				return false;
			}

			server.log("Check-in time updated. ReservationId=" + reservationId + ", Checkin=" + checkinTime);
			return true;

		} catch (Exception e) {
			server.log("ERROR: Failed to update check-in time. ReservationId=" + reservationId + ", Message="
					+ e.getMessage());
			return false;
		}
	}

	/**
	 * Updates the check-out time for a reservation.
	 *
	 * @param reservationId reservation identifier
	 * @param checkoutTime  check-out timestamp to set
	 * @return {@code true} if updated successfully, {@code false} otherwise
	 */
	public boolean updateCheckoutTime(int reservationId, LocalDateTime checkoutTime) {
		try {
			boolean updated = db.updateCheckoutTime(reservationId, checkoutTime);

			if (!updated) {
				server.log("WARN: Reservation not found. Check-out not updated. ReservationId=" + reservationId);
				return false;
			}

			server.log("Check-out time updated. ReservationId=" + reservationId + ", Checkout=" + checkoutTime);
			return true;

		} catch (Exception e) {
			server.log("ERROR: Failed to update check-out time. ReservationId=" + reservationId + ", Message="
					+ e.getMessage());
			return false;
		}
	}

	/**
	 * Marks an active reservation as finished and releases its reserved table
	 * slots.
	 * <p>
	 * The finish process:
	 * <ul>
	 * <li>Validates that the reservation exists and is active</li>
	 * <li>Releases the two-hour slot window (4 slots)</li>
	 * <li>Updates the database (finished + checkout time + inactive)</li>
	 * <li>Triggers waiting-list notification for freed table</li>
	 * </ul>
	 * </p>
	 *
	 * @param confirmationCode reservation confirmation code
	 * @return {@code true} if finished successfully, {@code false} otherwise
	 */
	public boolean FinishReservation(String confirmationCode) {

		try {
			Reservation r = db.getReservationByConfirmationCode(confirmationCode);
			if (r == null || !r.isActive() || r.getReservationStatus() != ReservationStatus.Active) {
				server.log("WARN: Finish request invalid/inactive. Code=" + confirmationCode);
				return false;
			}

			Integer tableNum = r.getTableNumber();
			LocalDateTime start = r.getReservationTime();

			if (tableNum == null || start == null) {
				server.log("ERROR: Finish failed - missing table/reservation time. Code=" + confirmationCode);
				return false;
			}

			// 1) Release 2 hours (4 slots)
			for (int i = 0; i < 4; i++) {
				LocalDateTime slot = start.plusMinutes(30L * i);
				try {
					restaurantController.releaseSlot(slot, tableNum);
				} catch (Exception e) {
					server.log("ERROR: Failed releasing slot during finish. Slot=" + slot + ", Table=" + tableNum
							+ ", Msg=" + e.getMessage());
				}
			}

			// 2) Update DB: Finished + checkout + inactive
			LocalDateTime checkoutTime = LocalDateTime.now();
			boolean finished = db.finishReservationByConfirmationCode(confirmationCode, checkoutTime);

			if (finished) {
				server.log("Reservation finished. Code=" + confirmationCode + ", Checkout=" + checkoutTime);

				// after payment/finish -> try to notify waiting list
				notifyWaitingTableFreed(tableNum);

				return true;
			}

			server.log("WARN: Finish DB update did not affect row. Code=" + confirmationCode);
			return false;

		} catch (SQLException e) {
			server.log("ERROR: Failed to finish reservation. Code=" + confirmationCode + ", Message=" + e.getMessage());
			return false;
		}
	}

	// ========= recovery helpers (kept as-is) =========

	/**
	 * Attempts to find a guest reservation by contact details and reservation time.
	 *
	 * @param phone    guest phone number (optional)
	 * @param email    guest email address (optional)
	 * @param dateTime reservation date-time to match
	 * @return matching reservation, or {@code null} if not found / on error
	 */
	public Reservation findGuestReservationByContactAndTime(String phone, String email, LocalDateTime dateTime) {

		if (((phone == null || phone.isBlank()) && (email == null || email.isBlank())) || dateTime == null) {
			return null;
		}

		try {
			return db.findGuestReservationByContactAndTime(phone, email, dateTime);
		} catch (Exception e) {
			server.log("ERROR: Recover guest confirmation failed. " + e.getMessage());
			return null;
		}
	}

	/**
	 * Recovers a guest reservation confirmation code using contact details and
	 * reservation date-time.
	 *
	 * @param phone               guest phone number (optional)
	 * @param email               guest email address (optional)
	 * @param reservationDateTime reservation date-time to match
	 * @return confirmation code if found, or {@code null} otherwise
	 */
	public String recoverGuestConfirmationCode(String phone, String email,
			java.time.LocalDateTime reservationDateTime) {

		if ((phone == null || phone.isBlank()) && (email == null || email.isBlank())) {
			return null;
		}
		if (reservationDateTime == null) {
			return null;
		}

		try {
			java.util.ArrayList<Integer> guestIds = db.getGuestIdsByContact(phone, email);

			if (guestIds == null || guestIds.isEmpty()) {
				return null;
			}

			return db.findGuestConfirmationCodeByDateTimeAndGuestIds(reservationDateTime, guestIds);

		} catch (Exception e) {
			server.log("ERROR: recoverGuestConfirmationCode failed: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Creates an immediate reservation from the waiting list at the current time.
	 * <p>
	 * The method searches for an available table while excluding tables currently
	 * locked by waiting logic. If a suitable table is found, a two-hour window is
	 * locked, the reservation is inserted, and a reminder notification is
	 * scheduled.
	 * </p>
	 *
	 * @param start            reservation start time
	 * @param guests           number of guests
	 * @param user             user creating the reservation
	 * @param confirmationCode confirmation code to assign
	 * @return created reservation, or {@code null} on failure
	 */
	public Reservation createNowReservationFromWaiting(LocalDateTime start, int guests, User user,
			String confirmationCode) {
		if (start == null || user == null || confirmationCode == null || confirmationCode.isBlank())
			return null;

		Table table;
		try {
			ArrayList<Integer> lockedTables = (waitingController == null) ? new ArrayList<>()
					: waitingController.getLockedTableNumbersNow();

			table = restaurantController.getOneAvailableTableAtExcludingTables(start, guests, lockedTables);

		} catch (Exception e) {
			server.log("ERROR: Waiting -> check availability failed. " + e.getMessage());
			return null;
		}

		if (table == null)
			return null;

		boolean locked2h = tryReserveForTwoHours(start, table.getTableNumber());
		if (!locked2h)
			return null;

		Reservation res = new Reservation();
		res.setCreatedByUserId(user.getUserId());
		res.setCreatedByRole(user.getUserRole());
		res.setGuestAmount(guests);
		res.setReservationTime(start);
		res.setConfirmed(true);
		res.setActive(true);
		res.setTableNumber(table.getTableNumber());
		res.setReservationStatus(ReservationStatus.Active);
		res.setConfirmationCode(confirmationCode);

		try {
			int reservationId = db.addReservation(start, guests, confirmationCode, user.getUserId(), user.getUserRole(),
					table.getTableNumber());

			if (reservationId == -1) {
				rollbackReservation(start, table.getTableNumber());
				return null;
			}

			res.setReservationId(reservationId);

			// ===============================
			// ✅ AUTO CHECK-IN (IMMEDIATE SEAT)
			// ===============================
			LocalDateTime now = LocalDateTime.now();

			db.updateCheckinTime(reservationId, now);

			// bill due = check-in + 2 hours
			try {
				db.setBillDueAt(reservationId, now.plusHours(2));
			} catch (Exception ignore) {
			}

			// create receipt immediately (if configured)
			try {
				if (receiptController != null) {
					receiptController.createReceiptIfMissingForCheckin(res, now);
				}
			} catch (Exception ignore) {
			}

			scheduleReservationReminder2HoursBefore(user.getUserId(), start, confirmationCode);

			server.log("Reservation created from waiting (immediate) & checked-in. " + "Code=" + confirmationCode
					+ ", Table=" + table.getTableNumber() + ", ReservationId=" + reservationId);

			return res;

		} catch (SQLException e) {
			server.log("ERROR: Waiting -> addReservation failed. " + e.getMessage());
			rollbackReservation(start, table.getTableNumber());
			return null;
		}
	}

	// ====CHECK-IN (GET TABLE) FROM RESERVATION====
	// ====Rule: allowed only from reservation time until +15 minutes====

	/**
	 * Performs check-in by confirmation code and returns the assigned table number
	 * when allowed.
	 * <p>
	 * Check-in rules:
	 * <ul>
	 * <li>Allowed starting at reservation time (not earlier)</li>
	 * <li>Expires 15 minutes after reservation time</li>
	 * <li>If the assigned table is occupied, check-in is marked as pending and a
	 * notification is scheduled later</li>
	 * </ul>
	 * </p>
	 * <p>
	 * If check-in succeeds, the method:
	 * <ul>
	 * <li>Updates check-in time in the database</li>
	 * <li>Sets bill due time to check-in + 2 hours</li>
	 * <li>Creates a receipt via {@link ReceiptController} (if configured)</li>
	 * </ul>
	 * </p>
	 *
	 * @param confirmationCode reservation confirmation code
	 * @return a {@link GetTableResultDTO} describing the result and (when allowed)
	 *         the table number
	 */
	public GetTableResultDTO checkinReservationByCode(String confirmationCode) {

		if (confirmationCode == null || confirmationCode.isBlank()) {
			return new GetTableResultDTO(false, false, null, "Confirmation code is required.");
		}

		String code = confirmationCode.trim();

		try {
			Reservation r = db.getReservationByConfirmationCode(code);

			if (r == null) {
				return new GetTableResultDTO(false, false, null, "Reservation not found.");
			}

			if (!r.isActive() || r.getReservationStatus() != ReservationStatus.Active) {
				return new GetTableResultDTO(false, false, null, "Reservation is not active.");
			}

			LocalDateTime now = LocalDateTime.now();
			LocalDateTime resTime = r.getReservationTime();

			if (resTime == null) {
				return new GetTableResultDTO(false, false, null, "Reservation time is missing.");
			}

			// TOO EARLY -> DO NOT EXPOSE TABLE NUMBER
			if (now.isBefore(resTime)) {
				return new GetTableResultDTO(false, false, null,
						"It is too early to check in. Please arrive at your reservation time.");
			}

			// TOO LATE -> DO NOT EXPOSE TABLE NUMBER
			if (now.isAfter(resTime.plusMinutes(15))) {
				return new GetTableResultDTO(false, false, null,
						"Check-in time has expired (15 minutes after reservation time).");
			}

			Integer tableNumber = r.getTableNumber();
			if (tableNumber == null) {
				return new GetTableResultDTO(false, false, null,
						"No table has been assigned yet. Please contact the restaurant.");
			}

			// prevent double check-in
			if (r.getCheckinTime() != null) {
				return new GetTableResultDTO(true, false, tableNumber,
						"You are already checked-in. Your table number is: " + tableNumber);
			}

			boolean occupied = db.isTableOccupiedNow(tableNumber, r.getReservationId());

			// If table is free -> do real check-in + schedule bill time + create receipt
			if (!occupied) {

				boolean updated = db.updateCheckinTime(r.getReservationId(), now);
				if (!updated) {
					return new GetTableResultDTO(false, false, null, "Failed to update check-in time.");
				}

				// mark bill due = checkin + 2 hours
				try {
					db.setBillDueAt(r.getReservationId(), now.plusHours(2));
				} catch (Exception ignore) {
				}

				// create Receipt now (random amount) - via ReceiptController
				try {
					if (receiptController != null) {
						receiptController.createReceiptIfMissingForCheckin(r, now);
					}
				} catch (Exception ignore) {
				}

				return new GetTableResultDTO(true, false, tableNumber,
						"Checked-in successfully. Your table number is: " + tableNumber);
			}

			pendingCheckins.put(tableNumber, new PendingReservationCheckin(r.getReservationId(), r.getCreatedByUserId(),
					r.getConfirmationCode(), tableNumber));

			return new GetTableResultDTO(false, true, tableNumber,
					"Your table is not ready yet. Please wait for a notification.");

		} catch (Exception e) {
			server.log("ERROR: checkinReservationByCode failed. Code=" + code + ", Msg=" + e.getMessage());
			return new GetTableResultDTO(false, false, null, "Server error. Please try again.");
		}
	}

	/**
	 * Notifies a user with a pending check-in that their reserved table is now
	 * available.
	 * <p>
	 * This method is triggered when a table is freed (after cancellation/finish)
	 * and a pending check-in exists for that table number.
	 * </p>
	 *
	 * @param tableNumber table number that has become available
	 */
	private void notifyPendingReservationCheckins(Integer tableNumber) {
		if (tableNumber == null)
			return;

		PendingReservationCheckin pending = pendingCheckins.remove(tableNumber);
		if (pending == null)
			return;

		try {
			LocalDateTime now = LocalDateTime.now();

			if (notificationDB != null) {
				String body = "Your reserved table is now available. Please check in with your confirmation code: "
						+ pending.confirmationCode;

				notificationDB.addNotification(new Notification(pending.userId, Enums.Channel.SMS,
						Enums.NotificationType.TABLE_AVAILABLE, body, now));

				notificationDB.addNotification(new Notification(pending.userId, Enums.Channel.EMAIL,
						Enums.NotificationType.TABLE_AVAILABLE, body, now));
			}

			server.log("Pending reservation notified. Code=" + pending.confirmationCode + ", Table=" + tableNumber
					+ ", UserId=" + pending.userId);

		} catch (Exception e) {
			server.log(
					"ERROR: notifyPendingReservationCheckins failed. Table=" + tableNumber + ", Msg=" + e.getMessage());
		}
	}

	/**
	 * Updates full reservation details based on manager input.
	 *
	 * @param res reservation instance containing updated fields
	 * @return {@code true} if the update succeeded, {@code false} otherwise
	 */
	public boolean updateReservationFromManager(Reservation res) {
		if (res == null || res.getReservationId() <= 0)
			return false;

		try {
			boolean success = db.updateFullReservationDetails(res);
			if (success) {
				server.log("Reservation updated by manager. ID=" + res.getReservationId());
			}
			return success;
		} catch (SQLException e) {
			server.log("ERROR: Failed to update reservation. ID=" + res.getReservationId() + ", Msg=" + e.getMessage());
			return false;
		}
	}

	/**
	 * Retrieves available reservation times for a specific date and guest count.
	 * <p>
	 * If {@code date} is today, returned times are constrained to start at least
	 * one hour from now, rounded up to the next half-hour slot.
	 * </p>
	 *
	 * @param date   reservation date
	 * @param guests number of guests
	 * @return list of available start times for the given day (may be empty)
	 */
	public ArrayList<LocalTime> getAvailableTimesForDay(LocalDate date, int guests) {
		ArrayList<LocalTime> out = new ArrayList<>();
		if (date == null || guests <= 0)
			return out;

		LocalTime fromTime = null;

		if (date.equals(LocalDate.now())) {
			LocalDateTime rounded = restaurantController.roundUpToNextHalfHour(LocalDateTime.now().plusHours(1));
			fromTime = rounded.toLocalTime();
		}

		fillAvailableTimesForDay(date, guests, fromTime, out);
		return out;
	}

	// ====AUTO-CANCEL: reservations without check-in (15 min)=====

	/**
	 * Automatically cancels reservations that were not checked-in within the grace
	 * period.
	 * <p>
	 * The grace period is 15 minutes after the reservation time. For each expired
	 * reservation, {@link #CancelReservation(String)} is invoked (which releases
	 * table slots and updates the DB).
	 * </p>
	 *
	 * @return number of reservations cancelled due to missing check-in after 15
	 *         minutes
	 */
	public int cancelReservationsWithoutCheckinAfterGracePeriod() {

		int cancelledCount = 0;

		try {
			LocalDateTime now = LocalDateTime.now();

			ArrayList<String> expiredCodes = db.getReservationsWithoutCheckinExpired(now.minusMinutes(15));

			for (String code : expiredCodes) {
				if (code == null)
					continue;

				boolean cancelled = CancelReservation(code);
				if (cancelled) {
					cancelledCount++;

					server.log("Reservation auto-cancelled (no check-in after 15 min): " + code);
				}
			}

			return cancelledCount;

		} catch (Exception e) {
			server.log("ERROR: cancelReservationsWithoutCheckinAfterGracePeriod failed. " + e.getMessage());
			return 0;
		}
	}

	/**
	 * Cancels active reservations that become invalid due to an opening-hours
	 * change.
	 * <p>
	 * The method scans active reservations on the specified {@code date} (limited
	 * to one month ahead), determines whether each reservation time is outside the
	 * new opening hours, and cancels invalid reservations. When cancellation
	 * occurs, the customer is notified via SMS and Email.
	 * </p>
	 *
	 * @param date     date whose reservations should be evaluated
	 * @param newOpen  new opening time (ignored if {@code isClosed} is
	 *                 {@code true})
	 * @param newClose new closing time (ignored if {@code isClosed} is
	 *                 {@code true})
	 * @param isClosed whether the restaurant is closed for the specified date
	 * @return number of reservations cancelled due to opening-hours change
	 */
	public int cancelReservationsDueToOpeningHoursChange(LocalDate date, LocalTime newOpen, LocalTime newClose,
			boolean isClosed) {

		int cancelledCount = 0;

		try {
			if (date.isAfter(LocalDate.now().plusMonths(1))) {
				server.log("Skip cancel: date beyond 1 month: " + date);
				return 0;
			}

			ArrayList<Reservation> reservations = db.getActiveReservationsByDate(date);

			for (Reservation r : reservations) {

				LocalTime resTime = r.getReservationTime().toLocalTime();

				boolean invalid;

				if (isClosed) {
					invalid = true;
				} else {
					invalid = resTime.isBefore(newOpen) || resTime.isAfter(newClose.minusHours(2));
				}

				if (!invalid)
					continue;

				boolean cancelled = CancelReservation(r.getConfirmationCode());

				if (cancelled) {
					cancelledCount++;

					notifyReservationCancelledOpeningHours(r);
				}
			}

		} catch (Exception e) {
			server.log("ERROR: cancelReservationsDueToOpeningHoursChange failed: " + e.getMessage());
		}

		return cancelledCount;
	}

	/**
	 * Sends cancellation notifications (SMS and Email) for a reservation cancelled
	 * due to opening-hours changes.
	 *
	 * @param r cancelled reservation
	 */
	private void notifyReservationCancelledOpeningHours(Reservation r) {

		if (notificationDB == null || r == null)
			return;

		LocalDateTime now = LocalDateTime.now();

		String body = "Your reservation on " + r.getReservationTime().toLocalDate() + " at "
				+ r.getReservationTime().toLocalTime().toString().substring(0, 5)
				+ " was cancelled due to a change in opening hours.";

		try {
			notificationDB.addNotification(new Notification(r.getCreatedByUserId(), Enums.Channel.SMS,
					Enums.NotificationType.RESERVATION_CANCELLED_OPENING_HOURS, body, now));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			notificationDB.addNotification(new Notification(r.getCreatedByUserId(), Enums.Channel.EMAIL,
					Enums.NotificationType.RESERVATION_CANCELLED_OPENING_HOURS, body, now));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Retrieves all active reservations for a specific user.
	 *
	 * @param userId user identifier
	 * @return list of active reservations for the user; empty list on error
	 */
	public ArrayList<Reservation> getActiveReservationsForUser(int userId) {
		try {
			return db.getActiveReservationsByUser(userId);
		} catch (Exception e) {
			server.log("ERROR: getActiveReservationsForUser failed. UserId=" + userId + ", Msg=" + e.getMessage());
			return new ArrayList<>();
		}
	}

}