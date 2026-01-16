package entities;

import java.io.Serializable;

/**
 * Entity representing a physical table in the restaurant.
 * <p>
 * This class stores basic table information such as the table number
 * and seating capacity. It is used for reservation allocation,
 * availability checks, and table management operations.
 * </p>
 */
public class Table implements Serializable {

    private static final long serialVersionUID = 1L;

    private int tableNumber;
    private int seatsAmount;

    /**
     * Default constructor.
     * <p>
     * Initializes a table with default values.
     * Typically used for framework support or database mapping.
     * </p>
     */
    public Table() {
        tableNumber = 0;
        seatsAmount = 0;
    }

    /**
     * Constructs a table with a specific table number and seating capacity.
     */
    public Table(int tableNumber, int seatsAmount) {
        this.tableNumber = tableNumber;
        this.seatsAmount = seatsAmount;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(int tableNumber) {
        this.tableNumber = tableNumber;
    }

    public int getSeatsAmount() {
        return seatsAmount;
    }

    public void setSeatsAmount(int seatsAmount) {
        this.seatsAmount = seatsAmount;
    }
}
