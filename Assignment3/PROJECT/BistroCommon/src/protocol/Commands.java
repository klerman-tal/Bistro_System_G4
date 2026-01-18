package protocol;

/**
 * Enumeration of all protocol commands supported by the client-server system.
 * <p>
 * Each command represents a specific action or request that can be sent from
 * the client to the server. These commands are used to route requests to the
 * appropriate logic handlers on the server side.
 * </p>
 */
public enum Commands {

	/**
	 * Reservation-related commands.
	 */
	CREATE_RESERVATION, GET_RESERVATION_HISTORY, CANCEL_RESERVATION, GET_CURRENT_DINERS,

	/**
	 * Special opening hours management commands.
	 */
	UPDATE_SPECIAL_OPENING_HOURS, GET_SPECIAL_OPENING_HOURS,

	/**
	 * Reservation check-in command.
	 */
	CHECKIN_RESERVATION,

	/**
	 * Receipt and reservation retrieval commands.
	 */
	GET_ALL_RESERVATIONS, PAY_RECEIPT, GET_RECEIPT_BY_CODE,

	/**
	 * Availability queries for specific dates.
	 */
	GET_AVAILABLE_TIMES_FOR_DATE,

	/**
	 * Subscriber-specific active data queries.
	 */
	GET_MY_ACTIVE_RESERVATIONS, GET_MY_ACTIVE_WAITINGS,

	/**
	 * Waiting list management commands.
	 */
	JOIN_WAITING_LIST, GET_WAITING_STATUS, CANCEL_WAITING, CONFIRM_WAITING_ARRIVAL,

	/**
	 * Opening hours management commands.
	 */
	GET_OPENING_HOURS, UPDATE_OPENING_HOURS,

	/**
	 * Table management commands.
	 */
	GET_TABLES, SAVE_TABLE, DELETE_TABLE,

	/**
	 * User and subscriber management commands.
	 */
	SUBSCRIBER_LOGIN, GUEST_LOGIN, RECOVER_SUBSCRIBER_CODE, RECOVER_GUEST_CONFIRMATION_CODE, REGISTER_SUBSCRIBER,
	UPDATE_SUBSCRIBER_DETAILS, GET_TIME_REPORT, GET_SUBSCRIBERS_REPORT, GET_ALL_SUBSCRIBERS, DELETE_SUBSCRIBER,
	UPDATE_SUBSCRIBER, GET_WAITING_LIST, FIND_USER_BY_ID, BARCODE_LOGIN, CREATE_GUEST_BY_PHONE
}
