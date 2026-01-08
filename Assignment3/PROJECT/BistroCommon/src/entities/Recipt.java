package entities;

public class Recipt {

	// ====== Fields ======
	// amountToPay = מחיר "ברוטו" לפני הנחה
	private double amountToPay;

	// reservation = ההזמנה שעליה משלמים (שם נמצא הקוד אישור, שולחן וכו')
	private Reservation reservation;

	// discount = אחוז הנחה בין 0 ל-1 (למשל 0.1 = 10% הנחה)
	private double discount;

	// reciptDetails = טקסט להצגה ללקוח (אפשר לייצר בשרת)
	private String reciptDetails;

	// isPaid = האם שולם בפועל (בשרת נדאג להפוך ל-true אחרי הצלחה)
	private boolean isPaid;

	// ====== Constructors ======
	public Recipt() {
		this.amountToPay = 0;
		this.reservation = null;
		this.discount = 0;
		this.reciptDetails = "";
		this.isPaid = false;
	}

	// קונסטרקטור נוח לשרת
	public Recipt(Reservation reservation, double amountToPay, double discount, String details) {
		this.reservation = reservation;
		this.amountToPay = amountToPay;
		setDiscount(discount); // נרמול עם בדיקה
		this.reciptDetails = details;
		this.isPaid = false;
	}

	// ====== Getters/Setters ======
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

	// חשוב: לא מאפשרים ערכים לא הגיוניים
	public void setDiscount(double discount) {
		if (discount < 0)
			discount = 0;
		if (discount > 1)
			discount = 1;
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

	// ====== Business helpers ======

	/**
	 * מחשב תשלום סופי: final = gross * (1 - discount) אם discount=0.1 => 10% הנחה
	 */
	public double calculateFinalPayment() {
		return amountToPay * (1.0 - discount);
	}
}
