package network;

import dto.GetReceiptByCodeDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Enums;
import entities.Receipt;
import entities.Reservation;
import entities.Enums.ReservationStatus;
import logicControllers.ReceiptController;
import logicControllers.ReservationController;
import ocsf.server.ConnectionToClient;

import java.math.BigDecimal;

/**
 * Server-side request handler responsible for retrieving a receipt by
 * reservation confirmation code.
 * <p>
 * This handler validates session ownership, reservation state, and payment
 * status before returning the receipt details. A subscriber discount is applied
 * when relevant.
 * </p>
 */
public class GetReceiptByCodeHandler implements RequestHandler {

	private final ReservationController reservationController;
	private final ReceiptController receiptController;

	/**
	 * Constructs a handler with the required reservation and receipt controller
	 * dependencies.
	 */
	public GetReceiptByCodeHandler(ReservationController reservationController, ReceiptController receiptController) {
		this.reservationController = reservationController;
		this.receiptController = receiptController;
	}

	/**
	 * Handles a request to retrieve a receipt by confirmation code.
	 * <p>
	 * The method verifies that the user is logged in, owns the reservation, that
	 * the reservation is active and checked-in, and that the receipt exists and is
	 * not already paid. If the requester is a subscriber, a discounted amount is
	 * returned.
	 * </p>
	 */
	@Override
	public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

		Object dataObj = request.getData();
		if (!(dataObj instanceof GetReceiptByCodeDTO dto)) {
			client.sendToClient(new ResponseDTO(false, "Invalid request data.", null));
			return;
		}

		Object sessionUserObj = (client == null) ? null : client.getInfo("user");
		if (!(sessionUserObj instanceof entities.User sessionUser)) {
			client.sendToClient(new ResponseDTO(false, "Not logged-in session. Please login again.", null));
			return;
		}

		String code = (dto.getConfirmationCode() == null) ? "" : dto.getConfirmationCode().trim();
		if (code.isEmpty()) {
			client.sendToClient(new ResponseDTO(false, "Confirmation code is required.", null));
			return;
		}

		Reservation r = reservationController.getReservationByConfirmationCode(code);
		if (r == null) {
			client.sendToClient(new ResponseDTO(false, "Reservation not found.", null));
			return;
		}

		if (r.getCreatedByUserId() != sessionUser.getUserId()) {
			client.sendToClient(new ResponseDTO(false, "This confirmation code does not belong to you.", null));
			return;
		}

		if (!r.isActive() || r.getReservationStatus() != ReservationStatus.Active) {
			client.sendToClient(new ResponseDTO(false, "Reservation is not active.", null));
			return;
		}

		if (r.getCheckinTime() == null) {
			client.sendToClient(new ResponseDTO(false, "Bill is available only after check-in.", null));
			return;
		}

		Receipt receipt = receiptController.getReceiptByReservationId(r.getReservationId());
		if (receipt == null) {
			client.sendToClient(new ResponseDTO(false, "Receipt not found for this reservation.", null));
			return;
		}

		if (receipt.isPaid()) {
			client.sendToClient(new ResponseDTO(false, "Receipt is already paid.", null));
			return;
		}

	 {
			// ✅ Subscriber discount 10% (return discounted amount to client only)
			if (r.getCreatedByRole() != Enums.UserRole.RandomClient) {
				Receipt copy = new Receipt();
				copy.setReceiptId(receipt.getReceiptId());
				copy.setReservationId(receipt.getReservationId());
				copy.setCreatedAt(receipt.getCreatedAt());
				copy.setPaid(receipt.isPaid());
				copy.setPaidAt(receipt.getPaidAt());
				copy.setPaymentType(receipt.getPaymentType());
				copy.setCreatedByUserId(receipt.getCreatedByUserId());
				copy.setCreatedByRole(receipt.getCreatedByRole());

				BigDecimal discounted = receipt.getAmount().multiply(BigDecimal.valueOf(0.9));
				copy.setAmount(discounted);

				client.sendToClient(new ResponseDTO(true, "Receipt loaded (10% subscriber discount applied).", copy));
				return;
			}

			client.sendToClient(new ResponseDTO(true, "Receipt loaded.", receipt));
		}
	}
}
