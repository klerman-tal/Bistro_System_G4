package entities;

import java.io.Serializable;

public class Table implements Serializable {

    private static final long serialVersionUID = 1L;

    private int tableNumber;
    private int seatsAmount;

    public Table() {
        tableNumber = 0;
        seatsAmount = 0;
    }

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
