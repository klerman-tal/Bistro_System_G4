package entities;

public class Recipt {
	
	private double amountToPay;
	private Reservation reservation;
	private double discount;
	private String reciptDetails;
	private boolean isPaid;
	
	
	public double getAmountToPay() {
		return amountToPay;
	}
	public void setAmountToPay(double amountToPay) {
		this.amountToPay = amountToPay;
	}
	public Reservation getReservation() {
		return reservation;
	}
	public void setReservation(Reservation reservation) {
		this.reservation = reservation;
	}
	public double getDiscount() {
		return discount;
	}
	public void setDiscount(double discount) {
		this.discount = discount;
	}
	public String getReciptDetails() {
		return reciptDetails;
	}
	public void setReciptDetails(String reciptDetails) {
		this.reciptDetails = reciptDetails;
	}
	public boolean isPaid() {
		return isPaid;
	}
	public void setPaid(boolean isPaid) {
		this.isPaid = isPaid;
	}

}
