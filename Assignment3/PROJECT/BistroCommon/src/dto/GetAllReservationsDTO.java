package dto;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) used to request all reservations from the server.
 * <p>
 * This object does not contain any fields as it serves as a marker for the
 * request.
 * </p>
 */
public class GetAllReservationsDTO implements Serializable {
	private static final long serialVersionUID = 1L;
}