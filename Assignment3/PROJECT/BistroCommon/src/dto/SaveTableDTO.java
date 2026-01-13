package dto;

import java.io.Serializable;

/**
 * DTO לשמירה / עדכון שולחן.
 * עובד גם ל-ADD וגם ל-UPDATE (לפי tableNumber).
 */
public class SaveTableDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int tableNumber;
    private int seatsAmount;

    public SaveTableDTO(int tableNumber, int seatsAmount) {
        this.tableNumber = tableNumber;
        this.seatsAmount = seatsAmount;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public int getSeatsAmount() {
        return seatsAmount;
    }
}
