package dto;

import java.io.Serializable;
import entities.Enums.WaitingStatus;

/**
 * Data Transfer Object (DTO) used to return the result of a
 * join waiting list request.
 * <p>
 * This object is sent from the server to the client and contains
 * the waiting confirmation code, the current waiting status,
 * and an optional table number if a table was assigned immediately.
 * </p>
 */
public class JoinWaitingResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String waitingConfirmationCode;
    private final WaitingStatus status;
    private final Integer tableNumber;

    public JoinWaitingResponseDTO(String waitingConfirmationCode, WaitingStatus status, Integer tableNumber) {
        this.waitingConfirmationCode = waitingConfirmationCode;
        this.status = status;
        this.tableNumber = tableNumber;
    }

    public String getWaitingConfirmationCode() { return waitingConfirmationCode; }
    public WaitingStatus getStatus() { return status; }
    public Integer getTableNumber() { return tableNumber; }
}
