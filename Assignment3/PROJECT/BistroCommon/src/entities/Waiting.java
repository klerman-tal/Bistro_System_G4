package entities;

import java.time.LocalDateTime;
import java.util.Random;

import entities.Enums.UserRole;
import entities.Enums.WaitingStatus;

public class Waiting {

    private int waitingId;
    private String confirmationCode;

    private int createdByUserId;
    private UserRole createdByRole;

    private int guestAmount;

    // "שעה שהתפנה שולחן בשבילו"
    private LocalDateTime tableFreedTime;

    private Integer tableNumber;

    private WaitingStatus waitingStatus;

    public Waiting() {
        confirmationCode = createConfirmationCode();
        createdByUserId = -1;
        createdByRole = null;

        guestAmount = 0;

        tableFreedTime = null;
        tableNumber = null;

        waitingStatus = WaitingStatus.Waiting;
    }

    public int getWaitingId() {
        return waitingId;
    }

    public void setWaitingId(int waitingId) {
        this.waitingId = waitingId;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    public int getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(int createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public UserRole getCreatedByRole() {
        return createdByRole;
    }

    public void setCreatedByRole(UserRole createdByRole) {
        this.createdByRole = createdByRole;
    }

    public int getGuestAmount() {
        return guestAmount;
    }

    public void setGuestAmount(int guestAmount) {
        this.guestAmount = guestAmount;
    }

    public LocalDateTime getTableFreedTime() {
        return tableFreedTime;
    }

    public void setTableFreedTime(LocalDateTime tableFreedTime) {
        this.tableFreedTime = tableFreedTime;
    }

    public Integer getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(Integer tableNumber) {
        this.tableNumber = tableNumber;
    }

    public WaitingStatus getWaitingStatus() {
        return waitingStatus;
    }

    public void setWaitingStatus(WaitingStatus waitingStatus) {
        this.waitingStatus = waitingStatus;
    }

    private String createConfirmationCode() {
        int code = new Random().nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
