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

/**
 * Server-side request handler responsible for updating special opening hours
 * for specific calendar dates.
 * <p>
 * This handler manages exceptional opening rules (such as holidays or special events)
 * that override the regular weekly opening hours. It ensures consistency between:
 * <ul>
 *   <li>Special opening hours stored in the database</li>
 *   <li>The availability grid for the affected date</li>
 *   <li>Existing reservations that may conflict with the new hours</li>
 * </ul>
 * </p>
 */
public class UpdateSpecialOpeningHoursHandler implements RequestHandler {

    private final SpecialOpeningHours_DB_Controller specialDb;
    private final RestaurantController restaurantController;
    private final ReservationController reservationController;

    /**
     * Constructs a handler with required controller dependencies.
     *
     * @param specialDb              database controller for special opening hours
     * @param restaurantController   controller responsible for availability grids
     * @param reservationController  controller responsible for reservation management
     */
    public UpdateSpecialOpeningHoursHandler(
            SpecialOpeningHours_DB_Controller specialDb,
            RestaurantController restaurantController,
            ReservationController reservationController) {

        this.specialDb = specialDb;
        this.restaurantController = restaurantController;
        this.reservationController = reservationController;
    }

    /**
     * Handles a request to create or update special opening hours for a specific date.
     * <p>
     * The method performs the following steps:
     * <ol>
     *   <li>Validates the incoming request payload</li>
     *   <li>Converts the received DTO into a {@link SpecialOpeningHours} entity</li>
     *   <li>Persists the special opening hours in the database</li>
     *   <li>Synchronizes the availability grid for the affected date</li>
     *   <li>Cancels conflicting reservations and triggers related notifications</li>
     *   <li>Returns the updated list of special opening hours to refresh the client UI</li>
     * </ol>
     * </p>
     */
    @Override
    public void handle(RequestDTO request, ConnectionToClient client) {

        try {
            if (client == null) return;

            if (request == null || request.getData() == null) {
                client.sendToClient(new ResponseDTO(false, "Missing special hours data", null));
                return;
            }

            // Expect a DTO payload representing special opening hours
            if (!(request.getData() instanceof dto.SpecialOpeningHoursDTO dto)) {
                client.sendToClient(new ResponseDTO(false, "Invalid special hours payload", null));
                return;
            }

            // Convert DTO to entity
            SpecialOpeningHours entity = new SpecialOpeningHours(
                    dto.getSpecialDate(),
                    dto.getOpenTime(),
                    dto.getCloseTime(),
                    dto.isClosed()
            );

            // 1) Persist special opening hours in the database
            boolean ok = specialDb.upsertSpecialHours(entity);
            if (!ok) {
                client.sendToClient(new ResponseDTO(false, "Failed to update special opening hours", null));
                return;
            }

            // 2) Synchronize availability grid for the affected date
            restaurantController.syncGridForSpecialDate(entity.getSpecialDate());

            // 3) Cancel conflicting reservations caused by the new opening rules
            int cancelled =
                    reservationController.cancelReservationsDueToOpeningHoursChange(
                            entity.getSpecialDate(),
                            entity.getOpenTime() == null ? null : entity.getOpenTime().toLocalTime(),
                            entity.getCloseTime() == null ? null : entity.getCloseTime().toLocalTime(),
                            entity.isClosed()
                    );

            // 4) Return updated special opening hours list so GUI refreshes immediately
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
