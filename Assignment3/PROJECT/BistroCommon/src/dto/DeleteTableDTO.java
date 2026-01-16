package dto;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) used to transfer table deletion data
 * between the client and the server.
 * <p>
 * This object contains the table number to be deleted.
 * </p>
 */
public class DeleteTableDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int tableNumber;

    public DeleteTableDTO(int tableNumber) {
        this.tableNumber = tableNumber;
    }

    public int getTableNumber() {
        return tableNumber;
    }
}