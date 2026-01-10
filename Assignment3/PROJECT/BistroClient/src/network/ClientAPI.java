package network;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import application.ChatClient;
import dto.CreateReservationDTO;
import dto.RecoverSubscriberCodeDTO;
import dto.SubscriberLoginDTO;
import dto.RequestDTO;
import protocol.Commands;
import entities.Enums.UserRole;
import entities.User;

/**
 * ClientAPI
 * =========
 * שכבת API של הקליינט:
 * - ה-GUI קורא רק לפה
 * - לא יודע כלום על DTO / Commands / Server
 */
public class ClientAPI {

    private final ChatClient client;

    public ClientAPI(ChatClient client) {
        this.client = client;
    }

    // =========================
    // RESERVATIONS
    // =========================

    public void createReservation(LocalDate date, LocalTime time, int guests, User user) throws IOException {
        if (date == null || time == null || user == null)
            throw new IllegalArgumentException("Invalid reservation data");

        LocalDateTime requested = LocalDateTime.of(date, time);
        if (requested.isBefore(LocalDateTime.now().plusHours(1))) {
            throw new IllegalArgumentException("Reservation must be at least 1 hour in advance.");
        }
        if (requested.isAfter(LocalDateTime.now().plusMonths(1))) {
            throw new IllegalArgumentException("Reservation cannot be more than 1 month in advance.");
        }

        int userId = user.getUserId();
        UserRole userRole = user.getUserRole();

        CreateReservationDTO data =
                new CreateReservationDTO(date, time, guests, userId, userRole);

        RequestDTO request =
                new RequestDTO(Commands.CREATE_RESERVATION, data);

        client.sendToServer(request);
    }

    public void getReservationHistory(int subscriberId) throws IOException {
        dto.GetReservationHistoryDTO data =
                new dto.GetReservationHistoryDTO(subscriberId);

        RequestDTO request =
                new RequestDTO(Commands.GET_RESERVATION_HISTORY, data);

        client.sendToServer(request);
    }

    // =========================
    // OPENING HOURS
    // =========================

    public void getOpeningHours() throws IOException {
        RequestDTO request =
                new RequestDTO(Commands.GET_OPENING_HOURS, null);

        client.sendToServer(request);
    }

    // =========================
    // LOGIN
    // =========================

    public void loginSubscriber(int subscriberId, String username) throws IOException {
        SubscriberLoginDTO data =
                new SubscriberLoginDTO(subscriberId, username);

        RequestDTO request =
                new RequestDTO(Commands.SUBSCRIBER_LOGIN, data);

        client.sendToServer(request);
    }

    public void loginGuest(String phone, String email) throws IOException {
        dto.GuestLoginDTO data =
                new dto.GuestLoginDTO(phone, email);

        RequestDTO request =
                new RequestDTO(Commands.GUEST_LOGIN, data);

        client.sendToServer(request);
    }

    // =========================
    // RECOVERY
    // =========================

    public void recoverSubscriberCode(String username, String phone, String email) throws IOException {
        RecoverSubscriberCodeDTO data =
                new RecoverSubscriberCodeDTO(username, phone, email);

        RequestDTO request =
                new RequestDTO(Commands.RECOVER_SUBSCRIBER_CODE, data);

        client.sendToServer(request);
    }

    public void recoverGuestConfirmationCode(
            String phone,
            String email,
            LocalDateTime reservationDateTime
    ) throws IOException {

        if ((phone == null || phone.isBlank()) && (email == null || email.isBlank()))
            throw new IllegalArgumentException("Please enter a phone number or an email.");

        if (reservationDateTime == null)
            throw new IllegalArgumentException("Please select reservation date and time.");

        dto.RecoverGuestConfirmationCodeDTO data =
                new dto.RecoverGuestConfirmationCodeDTO(
                        phone == null ? "" : phone.trim(),
                        email == null ? "" : email.trim(),
                        reservationDateTime
                );

        RequestDTO request =
                new RequestDTO(Commands.RECOVER_GUEST_CONFIRMATION_CODE, data);

        client.sendToServer(request);
    }

    // =========================
    // SUBSCRIBERS
    // =========================

    public void registerSubscriber(
            String username,
            String firstName,
            String lastName,
            String phone,
            String email,
            UserRole role
    ) throws IOException {

        dto.RegisterSubscriberDTO data =
                new dto.RegisterSubscriberDTO(
                        username,
                        firstName,
                        lastName,
                        phone,
                        email,
                        role
                );

        RequestDTO request =
                new RequestDTO(Commands.REGISTER_SUBSCRIBER, data);

        client.sendToServer(request);
    }

    public void updateSubscriberDetails(
            int subscriberId,
            String firstName,
            String lastName,
            String phone,
            String email
    ) throws IOException {

        dto.UpdateSubscriberDetailsDTO data =
                new dto.UpdateSubscriberDetailsDTO(
                        subscriberId,
                        firstName,
                        lastName,
                        phone,
                        email
                );

        RequestDTO request =
                new RequestDTO(Commands.UPDATE_SUBSCRIBER_DETAILS, data);

        client.sendToServer(request);
    }

    // =========================
    // ⭐ RESTAURANT MANAGEMENT – TABLES ⭐
    // =========================

    /**
     * בקשת כל השולחנות במסעדה
     */
    public void getTables() throws IOException {
        RequestDTO request =
                new RequestDTO(Commands.GET_TABLES, null);

        client.sendToServer(request);
    }

    /**
     * הוספה / עדכון שולחן
     */
    public void saveTable(int tableNumber, int seatsAmount) throws IOException {
        dto.SaveTableDTO data =
                new dto.SaveTableDTO(tableNumber, seatsAmount);

        RequestDTO request =
                new RequestDTO(Commands.SAVE_TABLE, data);

        client.sendToServer(request);
    }

    /**
     * מחיקת שולחן
     */
    public void deleteTable(int tableNumber) throws IOException {
        dto.DeleteTableDTO data =
                new dto.DeleteTableDTO(tableNumber);

        RequestDTO request =
                new RequestDTO(Commands.DELETE_TABLE, data);

        client.sendToServer(request);
    }
}
