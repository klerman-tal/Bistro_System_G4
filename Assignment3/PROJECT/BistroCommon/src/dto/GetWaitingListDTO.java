package dto;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) used to request the complete waiting list from the
 * server.
 * <p>
 * This request is typically used by restaurant managers to retrieve all active
 * waiting list entries. The object does not contain any fields, as the request
 * itself is sufficient.
 * </p>
 */
public class GetWaitingListDTO implements Serializable {
	private static final long serialVersionUID = 1L;
}
