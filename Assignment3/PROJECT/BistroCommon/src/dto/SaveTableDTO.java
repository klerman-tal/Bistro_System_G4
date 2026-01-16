package dto;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) used to save or update a restaurant table.
 * <p>
 * This object is used for both add and update operations, where the
 * table number determines whether a new table is created or an
 * existing table is updated.
 * </p>
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
