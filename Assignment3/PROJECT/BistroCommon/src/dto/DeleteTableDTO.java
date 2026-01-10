package dto;

import java.io.Serializable;

/**
 * DTO למחיקת שולחן לפי מספר שולחן.
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
