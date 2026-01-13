package network;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import application.ChatClient;
import dto.CreateReservationDTO;
import dto.JoinWaitingDTO;
import dto.RegisterSubscriberDTO;
import dto.RequestDTO;
import dto.WaitingCodeDTO;
import entities.Enums.UserRole;
import entities.User;
import protocol.Commands;

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

        CreateReservationDTO data = new CreateReservationDTO(date, time, guests, userId, userRole);
        RequestDTO request = new RequestDTO(Commands.CREATE_RESERVATION, data);

        client.sendToServer(request);
    }

    public void getReservationHistory(int subscriberId) throws IOException {
        dto.GetReservationHistoryDTO data = new dto.GetReservationHistoryDTO(subscriberId);
        RequestDTO request = new RequestDTO(Commands.GET_RESERVATION_HISTORY, data);
        client.sendToServer(request);
    }
    
    public void cancelReservation(String confirmationCode) throws IOException {

        if (confirmationCode == null || confirmationCode.isBlank()) {
            throw new IllegalArgumentException("Confirmation code is required");
        }

        dto.CancelReservationDTO data =
                new dto.CancelReservationDTO(confirmationCode, null);

        RequestDTO request =
                new RequestDTO(Commands.CANCEL_RESERVATION, data);

        client.sendToServer(request);
    }

    
    

    // =========================
    // OPENING HOURS
    // =========================

    public void getOpeningHours() throws IOException {
        RequestDTO request = new RequestDTO(Commands.GET_OPENING_HOURS, null);
        client.sendToServer(request);
    }

    // =========================
    // LOGIN
    // =========================

    public void loginSubscriber(int subscriberId, String username) throws IOException {
        dto.SubscriberLoginDTO data = new dto.SubscriberLoginDTO(subscriberId, username);
        RequestDTO request = new RequestDTO(Commands.SUBSCRIBER_LOGIN, data);
        client.sendToServer(request);
    }

    public void loginGuest(String phone, String email) throws IOException {
        dto.GuestLoginDTO data = new dto.GuestLoginDTO(phone, email);
        RequestDTO request = new RequestDTO(Commands.GUEST_LOGIN, data);
        client.sendToServer(request);
    }

    // =========================
    // RECOVERY
    // =========================

    public void recoverSubscriberCode(String username, String phone, String email) throws IOException {
        dto.RecoverSubscriberCodeDTO data = new dto.RecoverSubscriberCodeDTO(username, phone, email);
        RequestDTO request = new RequestDTO(Commands.RECOVER_SUBSCRIBER_CODE, data);
        client.sendToServer(request);
    }

    public void recoverGuestConfirmationCode(String phone, String email, LocalDateTime reservationDateTime) throws IOException {
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

        RequestDTO request = new RequestDTO(Commands.RECOVER_GUEST_CONFIRMATION_CODE, data);
        client.sendToServer(request);
    }

    // =========================
    // SUBSCRIBERS
    // =========================

    public void registerSubscriber(String username, String firstName, String lastName, String phone, String email, UserRole role)
            throws IOException {

        dto.RegisterSubscriberDTO data =
                new dto.RegisterSubscriberDTO(username, firstName, lastName, phone, email, role);

        RequestDTO request = new RequestDTO(Commands.REGISTER_SUBSCRIBER, data);
        client.sendToServer(request);
    }

    public void updateSubscriberDetails(int subscriberId, String username, String firstName, String lastName, String phone, String email)
            throws IOException {

        dto.UpdateSubscriberDetailsDTO data =
                new dto.UpdateSubscriberDetailsDTO(subscriberId,username,firstName, lastName, phone, email);

        RequestDTO request = new RequestDTO(Commands.UPDATE_SUBSCRIBER_DETAILS, data);
        client.sendToServer(request);
    }
        
        
        public void deleteSubscriber(int subscriberId) throws IOException {

            dto.DeleteSubscriberDTO data =
                    new dto.DeleteSubscriberDTO(subscriberId);

            RequestDTO request =
                    new RequestDTO(Commands.DELETE_SUBSCRIBER, data);

            client.sendToServer(request);
        

    }

    // =========================
    // TABLES
    // =========================

    public void getTables() throws IOException {
        RequestDTO request = new RequestDTO(Commands.GET_TABLES, null);
        client.sendToServer(request);
    }

    public void saveTable(int tableNumber, int seatsAmount) throws IOException {
        dto.SaveTableDTO data = new dto.SaveTableDTO(tableNumber, seatsAmount);
        RequestDTO request = new RequestDTO(Commands.SAVE_TABLE, data);
        client.sendToServer(request);
    }

    public void deleteTable(int tableNumber) throws IOException {
        dto.DeleteTableDTO data = new dto.DeleteTableDTO(tableNumber);
        RequestDTO request = new RequestDTO(Commands.DELETE_TABLE, data);
        client.sendToServer(request);
    }

    // =========================
    // WAITING LIST
    // =========================

    public void joinWaitingList(int guests, User user) throws IOException {
        if (guests <= 0 || user == null) {
            throw new IllegalArgumentException("Invalid waiting data");
        }

        JoinWaitingDTO data =
                new JoinWaitingDTO(guests, user.getUserId(), user.getUserRole());

        RequestDTO request =
                new RequestDTO(Commands.JOIN_WAITING_LIST, data);

        client.sendToServer(request);
    }

    public void getWaitingStatus(String confirmationCode) throws IOException {
        WaitingCodeDTO data = new WaitingCodeDTO(confirmationCode);
        RequestDTO req = new RequestDTO(Commands.GET_WAITING_STATUS, data);
        client.sendToServer(req);
    }

    public void cancelWaiting(String confirmationCode) throws IOException {
        WaitingCodeDTO data = new WaitingCodeDTO(confirmationCode);
        RequestDTO req = new RequestDTO(Commands.CANCEL_WAITING, data);
        client.sendToServer(req);
    }

    public void confirmWaitingArrival(String confirmationCode) throws IOException {
        WaitingCodeDTO data = new WaitingCodeDTO(confirmationCode);
        RequestDTO req = new RequestDTO(Commands.CONFIRM_WAITING_ARRIVAL, data);
        client.sendToServer(req);
    } 
    
    public void getAllSubscribers() throws IOException {
        RequestDTO request =
                new RequestDTO(Commands.GET_ALL_SUBSCRIBERS, null);
        client.sendToServer(request);
    }
    
    public void checkinReservation(String confirmationCode) throws IOException {
        if (confirmationCode == null || confirmationCode.isBlank()) {
            throw new IllegalArgumentException("Confirmation code is required");
        }

        dto.CheckinReservationDTO data =
                new dto.CheckinReservationDTO(confirmationCode.trim());

        RequestDTO request =
                new RequestDTO(Commands.CHECKIN_RESERVATION, data);

        client.sendToServer(request);
    }


   

}
