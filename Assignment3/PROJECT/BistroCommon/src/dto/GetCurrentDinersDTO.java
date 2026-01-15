package dto;

import java.io.Serializable;

/**
 * DTO for requesting the list of diners currently seated in the restaurant.
 * No parameters are required.
 */
public class GetCurrentDinersDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    public GetCurrentDinersDTO() {
        // Empty DTO – request only
    }
}
