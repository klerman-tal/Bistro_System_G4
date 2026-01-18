package network;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import application.ChatClient;
import dto.CreateReservationDTO;
import dto.FindUserByIdDTO;
import dto.GuestLoginDTO;
import dto.JoinWaitingDTO;
import dto.RegisterSubscriberDTO;
import dto.RequestDTO;
import dto.WaitingCodeDTO;
import entities.Enums.UserRole;
import entities.User;
import protocol.Commands;

/**
 * Provides a client-side API for sending typed requests to the server via
 * {@link ChatClient}.
 *
 * <p>
 * This class is a thin wrapper around the networking layer that: builds the
 * appropriate DTO payloads, validates basic input constraints, wraps them into
 * {@link RequestDTO} objects with the correct {@link Commands}, and sends them
 * to the server.
 * </p>
 *
 * <p>
 * Validation in this class focuses on preventing clearly invalid requests from
 * being sent (e.g., missing confirmation codes, null dates/times, or invalid
 * reservation time windows).
 * </p>
 */
public class ClientAPI {

	private final ChatClient client;

	/**
	 * Creates a new {@code ClientAPI} that sends requests using the provided
	 * {@link ChatClient}.
	 *
	 * @param client the client transport used to send requests to the server
	 * @throws IllegalArgumentException if {@code client} is {@code null}
	 */
	public ClientAPI(ChatClient client) {
		if (client == null) {
			throw new IllegalArgumentException("ChatClient must not be null");
		}
		this.client = client;
	}

	// =========================
	// RESERVATIONS
	// =========================

	/**
	 * Sends a request to create a new reservation.
	 *
	 * <p>
	 * Rules enforced by this method: the reservation must be at least 1 hour in
	 * advance and no more than 1 month in advance.
	 * </p>
	 *
	 * @param date   the reservation date
	 * @param time   the reservation time
	 * @param guests the number of diners
	 * @param user   the user creating the reservation
	 * @throws IllegalArgumentException if input data is missing/invalid or violates
	 *                                  time rules
	 * @throws IOException              if sending the request to the server fails
	 */
	public void createReservation(LocalDate date, LocalTime time, int guests, User user) throws IOException {
		if (date == null || time == null || user == null)
			throw new IllegalArgumentException("Invalid reservation data");

		LocalDateTime requested = LocalDateTime.of(date, time);
		if (requested.isBefore(LocalDateTime.now().plusHours(1))) {
			throw new IllegalArgumentException("Reservation must be at least 1 hour in advance.");
		}
		if (requested.isAfter(LocalDateTime.now().plusMonths(1))) {
			throw new IllegalArgumentException("Reservation cannot be more than 1 month in advance.");
		}

		int userId = user.getUserId();
		UserRole userRole = user.getUserRole();

		CreateReservationDTO data = new CreateReservationDTO(date, time, guests, userId, userRole);
		RequestDTO request = new RequestDTO(Commands.CREATE_RESERVATION, data);

		client.sendToServer(request);
	}

	/**
	 * Requests the reservation history for a given subscriber.
	 *
	 * @param subscriberId the subscriber identifier
	 * @throws IOException if sending the request to the server fails
	 */
	public void getReservationHistory(int subscriberId) throws IOException {
		dto.GetReservationHistoryDTO data = new dto.GetReservationHistoryDTO(subscriberId);
		RequestDTO request = new RequestDTO(Commands.GET_RESERVATION_HISTORY, data);
		client.sendToServer(request);
	}

	/**
	 * Sends a request to cancel a reservation by its confirmation code.
	 *
	 * @param confirmationCode the reservation confirmation code
	 * @throws IllegalArgumentException if {@code confirmationCode} is null/blank
	 * @throws IOException              if sending the request to the server fails
	 */
	public void cancelReservation(String confirmationCode) throws IOException {

		if (confirmationCode == null || confirmationCode.isBlank()) {
			throw new IllegalArgumentException("Confirmation code is required");
		}

		dto.CancelReservationDTO data = new dto.CancelReservationDTO(confirmationCode, null);
		RequestDTO request = new RequestDTO(Commands.CANCEL_RESERVATION, data);
		client.sendToServer(request);
	}

	// =========================
	// OPENING HOURS
	// =========================

	/**
	 * Requests the regular opening hours configuration from the server.
	 *
	 * @throws IOException if sending the request to the server fails
	 */
	public void getOpeningHours() throws IOException {
		RequestDTO request = new RequestDTO(Commands.GET_OPENING_HOURS, null);
		client.sendToServer(request);
	}

	/**
	 * Requests the special opening hours configuration (date-based overrides) from
	 * the server.
	 *
	 * @throws IOException if sending the request to the server fails
	 */
	public void getSpecialOpeningHours() throws IOException {
		RequestDTO request = new RequestDTO(Commands.GET_SPECIAL_OPENING_HOURS, null);
		client.sendToServer(request);
	}

	/**
	 * Updates the special opening hours for a specific date.
	 *
	 * <p>
	 * If {@code isClosed} is {@code true}, the server may treat
	 * {@code openTime}/{@code closeTime} as irrelevant depending on business rules.
	 * </p>
	 *
	 * @param date      the date to update
	 * @param openTime  opening time for the given date (may be null depending on
	 *                  server rules)
	 * @param closeTime closing time for the given date (may be null depending on
	 *                  server rules)
	 * @param isClosed  whether the restaurant is closed on the given date
	 * @throws IllegalArgumentException if {@code date} is {@code null}
	 * @throws IOException              if sending the request to the server fails
	 */
	public void updateSpecialOpeningHours(LocalDate date, java.sql.Time openTime, java.sql.Time closeTime,
			boolean isClosed) throws IOException {

		if (date == null) {
			throw new IllegalArgumentException("Date must be provided for special opening hours.");
		}

		dto.SpecialOpeningHoursDTO data = new dto.SpecialOpeningHoursDTO(date, openTime, closeTime, isClosed);
		RequestDTO request = new RequestDTO(Commands.UPDATE_SPECIAL_OPENING_HOURS, data);
		client.sendToServer(request);
	}

	// =========================
	// LOGIN
	// =========================

	/**
	 * Sends a subscriber login request.
	 *
	 * @param subscriberId the subscriber identifier
	 * @param username     the subscriber username
	 * @throws IOException if sending the request to the server fails
	 */
	public void loginSubscriber(int subscriberId, String username) throws IOException {
		dto.SubscriberLoginDTO data = new dto.SubscriberLoginDTO(subscriberId, username);
		RequestDTO request = new RequestDTO(Commands.SUBSCRIBER_LOGIN, data);
		client.sendToServer(request);
	}

	/**
	 * Sends a guest login request.
	 *
	 * @param phone guest phone number (may be required by server rules)
	 * @param email guest email address (may be required by server rules)
	 * @throws IOException if sending the request to the server fails
	 */
	public void loginGuest(String phone, String email) throws IOException {
		dto.GuestLoginDTO data = new dto.GuestLoginDTO(phone, email);
		RequestDTO request = new RequestDTO(Commands.GUEST_LOGIN, data);
		client.sendToServer(request);
	}

	// =========================
	// RECOVERY
	// =========================

	/**
	 * Requests recovery of a subscriber code using user-identifying details.
	 *
	 * @param username the subscriber username
	 * @param phone    the subscriber phone
	 * @param email    the subscriber email
	 * @throws IOException if sending the request to the server fails
	 */
	public void recoverSubscriberCode(String username, String phone, String email) throws IOException {
		dto.RecoverSubscriberCodeDTO data = new dto.RecoverSubscriberCodeDTO(username, phone, email);
		RequestDTO request = new RequestDTO(Commands.RECOVER_SUBSCRIBER_CODE, data);
		client.sendToServer(request);
	}

	/**
	 * Requests recovery of a guest reservation confirmation code.
	 *
	 * @param phone               guest phone number (optional if email is provided)
	 * @param email               guest email address (optional if phone is
	 *                            provided)
	 * @param reservationDateTime the reservation date and time
	 * @throws IllegalArgumentException if both phone and email are missing/blank,
	 *                                  or if date/time is missing
	 * @throws IOException              if sending the request to the server fails
	 */
	public void recoverGuestConfirmationCode(String phone, String email, LocalDateTime reservationDateTime)
			throws IOException {
		if ((phone == null || phone.isBlank()) && (email == null || email.isBlank()))
			throw new IllegalArgumentException("Please enter a phone number or an email.");

		if (reservationDateTime == null)
			throw new IllegalArgumentException("Please select reservation date and time.");

		dto.RecoverGuestConfirmationCodeDTO data = new dto.RecoverGuestConfirmationCodeDTO(
				phone == null ? "" : phone.trim(), email == null ? "" : email.trim(), reservationDateTime);

		RequestDTO request = new RequestDTO(Commands.RECOVER_GUEST_CONFIRMATION_CODE, data);
		client.sendToServer(request);
	}

	// =========================
	// SUBSCRIBERS
	// =========================

	/**
	 * Sends a request to register a new subscriber.
	 *
	 * @param username  the subscriber username
	 * @param firstName subscriber first name
	 * @param lastName  subscriber last name
	 * @param phone     subscriber phone number
	 * @param email     subscriber email address
	 * @param role      the assigned user role
	 * @throws IOException if sending the request to the server fails
	 */
	public void registerSubscriber(String username, String firstName, String lastName, String phone, String email,
			UserRole role) throws IOException {

		RegisterSubscriberDTO data = new dto.RegisterSubscriberDTO(username, firstName, lastName, phone, email, role);
		RequestDTO request = new RequestDTO(Commands.REGISTER_SUBSCRIBER, data);
		client.sendToServer(request);
	}

	/**
	 * Sends a request to update an existing subscriber's details.
	 *
	 * @param subscriberId the subscriber identifier
	 * @param username     updated username
	 * @param firstName    updated first name
	 * @param lastName     updated last name
	 * @param phone        updated phone number
	 * @param email        updated email address
	 * @throws IOException if sending the request to the server fails
	 */
	public void updateSubscriberDetails(int subscriberId, String username, String firstName, String lastName,
			String phone, String email) throws IOException {

		dto.UpdateSubscriberDetailsDTO data = new dto.UpdateSubscriberDetailsDTO(subscriberId, username, firstName,
				lastName, phone, email);

		RequestDTO request = new RequestDTO(Commands.UPDATE_SUBSCRIBER_DETAILS, data);
		client.sendToServer(request);
	}

	/**
	 * Sends a request to delete a subscriber by id.
	 *
	 * @param subscriberId the subscriber identifier
	 * @throws IOException if sending the request to the server fails
	 */
	public void deleteSubscriber(int subscriberId) throws IOException {

		dto.DeleteSubscriberDTO data = new dto.DeleteSubscriberDTO(subscriberId);
		RequestDTO request = new RequestDTO(Commands.DELETE_SUBSCRIBER, data);
		client.sendToServer(request);
	}

	// =========================
	// TABLES
	// =========================

	/**
	 * Requests the current restaurant tables list from the server.
	 *
	 * @throws IOException if sending the request to the server fails
	 */
	public void getTables() throws IOException {
		RequestDTO request = new RequestDTO(Commands.GET_TABLES, null);
		client.sendToServer(request);
	}

	/**
	 * Sends a request to create or update a table definition.
	 *
	 * @param tableNumber the table number
	 * @param seatsAmount the number of seats
	 * @throws IOException if sending the request to the server fails
	 */
	public void saveTable(int tableNumber, int seatsAmount) throws IOException {
		dto.SaveTableDTO data = new dto.SaveTableDTO(tableNumber, seatsAmount);
		RequestDTO request = new RequestDTO(Commands.SAVE_TABLE, data);
		client.sendToServer(request);
	}

	/**
	 * Sends a request to delete a table by table number.
	 *
	 * @param tableNumber the table number
	 * @throws IOException if sending the request to the server fails
	 */
	public void deleteTable(int tableNumber) throws IOException {
		dto.DeleteTableDTO data = new dto.DeleteTableDTO(tableNumber);
		RequestDTO request = new RequestDTO(Commands.DELETE_TABLE, data);
		client.sendToServer(request);
	}

	// =========================
	// WAITING LIST
	// =========================

	/**
	 * Sends a request to join the waiting list.
	 *
	 * @param guests the number of diners to place in the waiting list
	 * @param user   the user requesting to join
	 * @throws IllegalArgumentException if {@code guests <= 0} or {@code user} is
	 *                                  {@code null}
	 * @throws IOException              if sending the request to the server fails
	 */
	public void joinWaitingList(int guests, User user) throws IOException {
		if (guests <= 0 || user == null) {
			throw new IllegalArgumentException("Invalid waiting data");
		}

		JoinWaitingDTO data = new JoinWaitingDTO(guests, user.getUserId(), user.getUserRole());
		RequestDTO request = new RequestDTO(Commands.JOIN_WAITING_LIST, data);
		client.sendToServer(request);
	}

	/**
	 * Requests the current waiting status using a waiting confirmation code.
	 *
	 * @param confirmationCode the waiting confirmation code
	 * @throws IOException if sending the request to the server fails
	 */
	public void getWaitingStatus(String confirmationCode) throws IOException {
		WaitingCodeDTO data = new WaitingCodeDTO(confirmationCode);
		RequestDTO req = new RequestDTO(Commands.GET_WAITING_STATUS, data);
		client.sendToServer(req);
	}

	/**
	 * Sends a check-in request for a reservation by its confirmation code.
	 *
	 * @param confirmationCode the reservation confirmation code
	 * @throws IllegalArgumentException if {@code confirmationCode} is null/blank
	 * @throws IOException              if sending the request to the server fails
	 */
	public void checkinReservation(String confirmationCode) throws IOException {
		if (confirmationCode == null || confirmationCode.isBlank()) {
			throw new IllegalArgumentException("Confirmation code is required");
		}

		dto.CheckinReservationDTO data = new dto.CheckinReservationDTO(confirmationCode.trim());
		RequestDTO request = new RequestDTO(Commands.CHECKIN_RESERVATION, data);
		client.sendToServer(request);
	}

	/**
	 * Sends a request to cancel a waiting list entry by its confirmation code.
	 *
	 * @param confirmationCode the waiting confirmation code
	 * @throws IOException if sending the request to the server fails
	 */
	public void cancelWaiting(String confirmationCode) throws IOException {
		WaitingCodeDTO data = new WaitingCodeDTO(confirmationCode);
		RequestDTO req = new RequestDTO(Commands.CANCEL_WAITING, data);
		client.sendToServer(req);
	}

	/**
	 * Confirms arrival for a waiting list entry by its confirmation code.
	 *
	 * @param confirmationCode the waiting confirmation code
	 * @throws IOException if sending the request to the server fails
	 */
	public void confirmWaitingArrival(String confirmationCode) throws IOException {
		WaitingCodeDTO data = new WaitingCodeDTO(confirmationCode);
		RequestDTO req = new RequestDTO(Commands.CONFIRM_WAITING_ARRIVAL, data);
		client.sendToServer(req);
	}

	/**
	 * Requests a list of all subscribers (typically for administrative views).
	 *
	 * @throws IOException if sending the request to the server fails
	 */
	public void getAllSubscribers() throws IOException {
		RequestDTO request = new RequestDTO(Commands.GET_ALL_SUBSCRIBERS, null);
		client.sendToServer(request);
	}

	/**
	 * Requests a list of all reservations (typically for administrative views).
	 *
	 * @throws IOException if sending the request to the server fails
	 */
	public void getAllReservations() throws IOException {
		RequestDTO request = new RequestDTO(Commands.GET_ALL_RESERVATIONS, null);
		client.sendToServer(request);
	}

	/**
	 * Requests the entire waiting list (typically for administrative views).
	 *
	 * @throws IOException if sending the request to the server fails
	 */
	public void getWaitingList() throws IOException {
		RequestDTO request = new RequestDTO(Commands.GET_WAITING_LIST, null);
		client.sendToServer(request);
	}

	/**
	 * Requests available reservation times for a given date and party size.
	 *
	 * <p>
	 * If {@code guests <= 0}, this method defaults to 1 guest.
	 * </p>
	 *
	 * @param date   the requested date
	 * @param guests party size
	 * @throws IllegalArgumentException if {@code date} is {@code null}
	 * @throws IOException              if sending the request to the server fails
	 */
	public void getAvailableTimesForDate(LocalDate date, int guests) throws IOException {
		if (date == null)
			throw new IllegalArgumentException("Date is required");
		if (guests <= 0)
			guests = 1;

		dto.GetAvailableTimesDTO data = new dto.GetAvailableTimesDTO(date, guests);
		RequestDTO request = new RequestDTO(Commands.GET_AVAILABLE_TIMES_FOR_DATE, data);
		client.sendToServer(request);
	}

	/**
	 * Requests a receipt by reservation confirmation code.
	 *
	 * @param confirmationCode the reservation confirmation code
	 * @throws IllegalArgumentException if {@code confirmationCode} is null/blank
	 * @throws IOException              if sending the request to the server fails
	 */
	public void getReceiptByCode(String confirmationCode) throws IOException {
		if (confirmationCode == null || confirmationCode.isBlank())
			throw new IllegalArgumentException("Confirmation code is required");

		dto.GetReceiptByCodeDTO data = new dto.GetReceiptByCodeDTO(confirmationCode.trim());
		RequestDTO request = new RequestDTO(Commands.GET_RECEIPT_BY_CODE, data);
		client.sendToServer(request);
	}

	/**
	 * Sends a payment request for a receipt.
	 *
	 * @param data the payment DTO payload
	 * @throws IllegalArgumentException if {@code data} is {@code null}
	 * @throws IOException              if sending the request to the server fails
	 */
	public void payReceipt(dto.PayReceiptDTO data) throws IOException {
		if (data == null)
			throw new IllegalArgumentException("Payment data is required");
		RequestDTO request = new RequestDTO(Commands.PAY_RECEIPT, data);
		client.sendToServer(request);
	}

	/**
	 * Requests the current number (or list) of diners from the server (depending on
	 * server implementation).
	 *
	 * @throws IOException if sending the request to the server fails
	 */
	public void getCurrentDiners() throws IOException {
		System.out.println("Sending GET_CURRENT_DINERS...");
		RequestDTO request = new RequestDTO(Commands.GET_CURRENT_DINERS, null);
		client.sendToServer(request);

	}

	/**
	 * Requests user details by user id.
	 *
	 * @param userId the user identifier
	 * @throws IOException if sending the request to the server fails
	 */
	public void findUserById(int userId) throws IOException {
		FindUserByIdDTO data = new FindUserByIdDTO(userId);
		RequestDTO request = new RequestDTO(Commands.FIND_USER_BY_ID, data);
		client.sendToServer(request);
	}

	/**
	 * Requests creation of a guest user record based on phone number.
	 *
	 * @param phone the guest phone number
	 * @throws IOException if sending the request to the server fails
	 */
	public void createGuestByPhone(String phone) throws IOException {
		GuestLoginDTO data = new GuestLoginDTO(phone, null);
		RequestDTO request = new RequestDTO(Commands.CREATE_GUEST_BY_PHONE, data);
		client.sendToServer(request);
	}

	/**
	 * Requests all active reservations for a specific user.
	 *
	 * @param userId the user identifier
	 * @throws IOException if sending the request to the server fails
	 */
	public void getMyActiveReservations(int userId) throws IOException {
		dto.GetMyActiveReservationsDTO data = new dto.GetMyActiveReservationsDTO(userId);
		RequestDTO request = new RequestDTO(Commands.GET_MY_ACTIVE_RESERVATIONS, data);
		client.sendToServer(request);
	}

	/**
	 * Requests all active waiting list entries for a specific user.
	 *
	 * @param userId the user identifier
	 * @throws IOException if sending the request to the server fails
	 */
	public void getMyActiveWaitings(int userId) throws IOException {
		dto.GetMyActiveWaitingsDTO data = new dto.GetMyActiveWaitingsDTO(userId);
		RequestDTO request = new RequestDTO(Commands.GET_MY_ACTIVE_WAITINGS, data);
		client.sendToServer(request);
	}

	/**
	 * Sends a login request using a scanned barcode subscriber identifier.
	 *
	 * @param subscriberId the scanned subscriber id value
	 * @throws IllegalArgumentException if {@code subscriberId} is null/blank
	 * @throws IOException              if sending the request to the server fails
	 */
	public void loginByBarcode(String subscriberId) throws IOException {
		if (subscriberId == null || subscriberId.isBlank()) {
			throw new IllegalArgumentException("Scanned ID cannot be empty");
		}

		dto.BarcodeScanDTO data = new dto.BarcodeScanDTO(subscriberId);

		RequestDTO request = new RequestDTO(Commands.BARCODE_LOGIN, data);

		client.sendToServer(request);
	}
}
