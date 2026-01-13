package dto;

import java.io.Serializable;

import entities.Enums.WaitingStatus;

public class JoinWaitingResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String waitingConfirmationCode;
    private final WaitingStatus status;
    private final Integer tableNumber; // אם מיידי - יהיה מספר שולחן

    public JoinWaitingResponseDTO(String waitingConfirmationCode, WaitingStatus status, Integer tableNumber) {
        this.waitingConfirmationCode = waitingConfirmationCode;
        this.status = status;
        this.tableNumber = tableNumber;
    }

    public String getWaitingConfirmationCode() { return waitingConfirmationCode; }
    public WaitingStatus getStatus() { return status; }
    public Integer getTableNumber() { return tableNumber; }
}
