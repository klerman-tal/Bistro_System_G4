package entities;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Random;

import entities.Enums.UserRole;
import entities.Enums.WaitingStatus;

/**
 * Entity representing a waiting list entry.
 * <p>
 * This class stores information about users who joined the waiting list,
 * including guest count, creator details, waiting status, optional table
 * assignment, and lifecycle timestamps.
 * </p>
 */
public class Waiting implements Serializable {

    private static final long serialVersionUID = 1L;

    private int waitingId;
    private int guestAmount;
    private int createdByUserId;
    private UserRole createdByRole;

    private String confirmationCode;
    private WaitingStatus waitingStatus = WaitingStatus.Waiting;

    private Integer tableNumber;
    private LocalDateTime tableFreedTime;

    private LocalDateTime joinedAt;

    // ====Confirmation Code====
  
    /**
     * Generates and assigns a random six-digit confirmation code
     * for this waiting list entry.
     * <p>
     * The confirmation code is used to identify the waiting request
     * from the user's perspective.
     * </p>
     */
    public void generateAndSetConfirmationCode() {
        Random rnd = new Random();
        this.confirmationCode = String.valueOf(100000 + rnd.nextInt(900000));
    }

	// ===== Getters / Setters =====


    public String getConfirmationCode() {
        return confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    public WaitingStatus getWaitingStatus() {
        return waitingStatus;
    }

    public void setWaitingStatus(WaitingStatus waitingStatus) {
        this.waitingStatus = waitingStatus;
    }

    public Integer getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(Integer tableNumber) {
        this.tableNumber = tableNumber;
    }

    public LocalDateTime getTableFreedTime() {
        return tableFreedTime;
    }

    public void setTableFreedTime(LocalDateTime tableFreedTime) {
        this.tableFreedTime = tableFreedTime;
    }

    public int getGuestAmount() {
        return guestAmount;
    }

    public void setGuestAmount(int guestAmount) {
        this.guestAmount = guestAmount;
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

    public int getWaitingId() {
        return waitingId;
    }

    public void setWaitingId(int waitingId) {
        this.waitingId = waitingId;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}
