package entities;

public class Recipt {
	
	// ====== Fields ======
	private double amountToPay;
	private Reservation reservation;
	private double discount;
	private String reciptDetails;
	private boolean isPaid;
	
	// ====== Constructors ======
	public Recipt() {
		amountToPay = 0;
		reservation = null;
		discount = 0;
		reciptDetails = "";
		isPaid = false;
	}
	
	// ====== Properties ======
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
	
	// ====== Methods ======
	public double CalculateFinalPayment() {
		return amountToPay*discount;
	}

}
