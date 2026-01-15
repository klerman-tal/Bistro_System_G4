package network;

import dbControllers.SpecialOpeningHours_DB_Controller;
import dto.RequestDTO;
import dto.ResponseDTO;
import dto.SpecialOpeningHoursDTO;
import entities.SpecialOpeningHours;
import logicControllers.ReservationController;
import logicControllers.RestaurantController;
import ocsf.server.ConnectionToClient;

public class UpdateSpecialOpeningHoursHandler implements RequestHandler {

    private final SpecialOpeningHours_DB_Controller specialDb;
    private final RestaurantController restaurantController;
    private final ReservationController reservationController;

    public UpdateSpecialOpeningHoursHandler(
            SpecialOpeningHours_DB_Controller specialDb,
            RestaurantController restaurantController,
            ReservationController reservationController) {

        this.specialDb = specialDb;
        this.restaurantController = restaurantController;
        this.reservationController = reservationController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) {

        try {
            if (request == null || request.getData() == null) {
                client.sendToClient(
                        new ResponseDTO(false, "Missing special hours data", null));
                return;
            }

            // ✅ FIX: expect DTO (not Entity)
            if (!(request.getData() instanceof SpecialOpeningHoursDTO dto)) {
                client.sendToClient(
                        new ResponseDTO(false, "Invalid special hours payload", null));
                return;
            }

            // ✅ Convert DTO → Entity
            SpecialOpeningHours entity = new SpecialOpeningHours(
                    dto.getSpecialDate(),
                    dto.getOpenTime(),
                    dto.getCloseTime(),
                    dto.isClosed()
            );

            boolean ok = specialDb.upsertSpecialHours(entity);
            if (!ok) {
                client.sendToClient(
                        new ResponseDTO(false, "Failed to update special opening hours", null));
                return;
            }

            // (אופציונלי – כשתרצי)
            // restaurantController.syncGridForSpecialDate(dto.getDate());
            // reservationController.cancelReservationsDueToOpeningHoursChange(...);

            client.sendToClient(
                    new ResponseDTO(true, "Special hours updated successfully", null));

        } catch (Exception e) {
            try {
                client.sendToClient(
                        new ResponseDTO(false, "Server error: " + e.getMessage(), null));
            } catch (Exception ignore) {}
            e.printStackTrace();
        }
    }
}
