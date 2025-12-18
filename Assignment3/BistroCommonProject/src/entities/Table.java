package entities;

public class Table {
	
	private int tableNumber;
	private int seatsAmount;
	private boolean isAvailable;
	
	public Table() {
		tableNumber = 0;
		seatsAmount = 0;
		isAvailable = false;
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
	public boolean getIsAvailable() {
		return isAvailable;
	}
	public void setIsAvailable(boolean isAvailable) {
		 this.isAvailable=isAvailable;
	}

}
