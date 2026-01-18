package dto;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) used to request the list of all tables in the
 * restaurant.
 * <p>
 * This object does not contain any fields, as the request itself is sufficient
 * to retrieve the restaurant tables.
 * </p>
 */
public class GetTablesDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	public GetTablesDTO() {
		// empty on purpose
	}
}
