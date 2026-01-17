package network;

import dto.PayReceiptDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Receipt;
import entities.Reservation;
import entities.Enums.ReservationStatus;
import logicControllers.ReceiptController;
import logicControllers.ReservationController;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for processing
 * receipt payment operations.
 * <p>
 * This handler validates payment details, verifies session ownership,
 * ensures reservation eligibility for payment, marks the receipt as paid,
 * finalizes the reservation, and performs post-payment cleanup when required.
 * </p>
 */
public class PayReceiptHandler implements RequestHandler {

    private final ReservationController reservationController;
    private final ReceiptController receiptController;
    private final UserController userController;

    /**
     * Constructs a handler with the required controller dependencies.
     */
    public PayReceiptHandler(ReservationController reservationController,
                             ReceiptController receiptController,
                             UserController userController) {
        this.reservationController = reservationController;
        this.receiptController = receiptController;
        this.userController = userController;
    }

    /**
     * Handles a receipt payment request received from the client.
     * <p>
     * The method validates the payment data, verifies that the requesting
     * user owns the reservation, checks reservation and receipt status,
     * processes the payment, finalizes the reservation, and removes
     * temporary guest users when applicable.
     * </p>
     */
    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        Object dataObj = request.getData();
        if (!(dataObj instanceof PayReceiptDTO dto)) {
            client.sendToClient(new ResponseDTO(false, "Invalid payment data.", null));
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

        if (dto.getPaymentType() == null) {
            client.sendToClient(new ResponseDTO(false, "Payment type is required.", null));
            return;
        }

        if (dto.getPaymentType().name().equals("CreditCard")) {
            Integer last4 = dto.getLast4Digits();
            Integer cvv = dto.getCvv();
            String exp = dto.getExpiryDate();

            if (last4 == null || last4 < 0 || last4 > 9999) {
                client.sendToClient(new ResponseDTO(false, "Invalid last 4 digits.", null));
                return;
            }
            if (cvv == null || cvv < 100 || cvv > 999) {
                client.sendToClient(new ResponseDTO(false, "Invalid CVV.", null));
                return;
            }
            if (exp == null || exp.isBlank()) {
                client.sendToClient(new ResponseDTO(false, "Invalid expiry date.", null));
                return;
            }
        }

        Reservation r = reservationController.getReservationByConfirmationCode(code);
        if (r == null) {
            client.sendToClient(new ResponseDTO(false, "Reservation not found.", null));
            return;
        }

        if (r.getCreatedByUserId() != sessionUser.getUserId()) {
            client.sendToClient(new ResponseDTO(false, "This reservation does not belong to you.", null));
            return;
        }

        if (!r.isActive() || r.getReservationStatus() != ReservationStatus.Active) {
            client.sendToClient(new ResponseDTO(false, "Reservation is not active.", null));
            return;
        }

        if (r.getCheckinTime() == null) {
            client.sendToClient(new ResponseDTO(false, "Cannot pay before check-in.", null));
            return;
        }

        Receipt receipt = receiptController.getReceiptByReservationId(r.getReservationId());
        if (receipt == null) {
            client.sendToClient(new ResponseDTO(false, "Receipt not found.", null));
            return;
        }

        if (receipt.isPaid()) {
            client.sendToClient(new ResponseDTO(false, "Receipt already paid.", null));
            return;
        }

        boolean paid = receiptController.markPaid(
                r.getReservationId(),
                dto.getPaymentType(),
                java.time.LocalDateTime.now()
        );

        if (!paid) {
            client.sendToClient(new ResponseDTO(false, "Payment failed. Please try again.", null));
            return;
        }

        boolean finished = reservationController.FinishReservation(code);
        if (!finished) {
            client.sendToClient(new ResponseDTO(false, "Payment done, but failed to finish reservation.", null));
            return;
        }

        if (r.getCreatedByRole() == entities.Enums.UserRole.RandomClient) {
            try {
                userController.deleteGuestAfterPayment(r.getCreatedByUserId());
            } catch (Exception ignored) {}
        }

        client.sendToClient(new ResponseDTO(true, "Payment completed successfully.", null));
    }
}
