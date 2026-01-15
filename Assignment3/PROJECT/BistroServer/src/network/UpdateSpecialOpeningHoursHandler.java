package network;

import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;

import dbControllers.SpecialOpeningHours_DB_Controller;
import dto.RequestDTO;
import dto.ResponseDTO;
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
            if (client == null) return;

            if (request == null || request.getData() == null) {
                client.sendToClient(new ResponseDTO(false, "Missing special hours data", null));
                return;
            }

            // ✅ FIX: expect DTO, not entity
            if (!(request.getData() instanceof dto.SpecialOpeningHoursDTO dto)) {
                client.sendToClient(new ResponseDTO(false, "Invalid special hours payload", null));
                return;
            }

            // ✅ Convert DTO -> Entity
            SpecialOpeningHours entity = new SpecialOpeningHours(
                    dto.getSpecialDate(),
                    dto.getOpenTime(),
                    dto.getCloseTime(),
                    dto.isClosed()
            );

            // 1️⃣ Save DB
            boolean ok = specialDb.upsertSpecialHours(entity);
            if (!ok) {
                client.sendToClient(new ResponseDTO(false, "Failed to update special opening hours", null));
                return;
            }

            // 2️⃣ Sync availability grid
            restaurantController.syncGridForSpecialDate(entity.getSpecialDate());

            // 3️⃣ Cancel invalid reservations + notify
            int cancelled =
                    reservationController.cancelReservationsDueToOpeningHoursChange(
                            entity.getSpecialDate(),
                            entity.getOpenTime() == null ? null : entity.getOpenTime().toLocalTime(),
                            entity.getCloseTime() == null ? null : entity.getCloseTime().toLocalTime(),
                            entity.isClosed()
                    );

            // 4️⃣ Return updated list so GUI refreshes immediately
            client.sendToClient(new ResponseDTO(
                    true,
                    "Special hours updated. Cancelled reservations: " + cancelled,
                    specialDb.getAllSpecialOpeningHours()
            ));

        } catch (Exception e) {
            try {
                client.sendToClient(new ResponseDTO(false, "Server error: " + e.getMessage(), null));
            } catch (Exception ignore) {}
            e.printStackTrace();
        }
    }

}
