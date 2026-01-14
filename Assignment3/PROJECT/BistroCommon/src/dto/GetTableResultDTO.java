package dto;

import java.io.Serializable;

/**
 * Result object returned for "get table" flows.
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
