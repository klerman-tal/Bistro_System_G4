package dto;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) used to return the result of a
 * table allocation request.
 * <p>
 * This object is sent from the server to the client and contains
 * information about whether the request was successful, whether
 * the client should be placed on a waiting list, the allocated
 * table number (if applicable), and an informational message.
 * </p>
 */
public class GetTableResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final boolean shouldWait;
    private final Integer tableNumber;
    private final String message;

    public GetTableResultDTO(boolean success, boolean shouldWait, Integer tableNumber, String message) {
        this.success = success;
        this.shouldWait = shouldWait;
        this.tableNumber = tableNumber;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isShouldWait() {
        return shouldWait;
    }

    public Integer getTableNumber() {
        return tableNumber;
    }

    public String getMessage() {
        return message;
    }
}
