package network;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

import application.ChatClient;
import dto.CreateReservationDTO;
import dto.RecoverSubscriberCodeDTO;
import dto.SubscriberLoginDTO;
import dto.RequestDTO;
import protocol.Commands;
import entities.Enums.UserRole;
import entities.User;

public class ClientAPI {

    private final ChatClient client;

    public ClientAPI(ChatClient client) {
        this.client = client;
    }

    public void createReservation(LocalDate date, LocalTime time, int guests, User user) throws IOException {
        if (date == null || time == null || user == null)
            throw new IllegalArgumentException("Invalid reservation data");

        // ✨ ולידציה ב-API: לפחות שעה מעכשיו ולא יותר מחודש
        LocalDateTime requested = LocalDateTime.of(date, time);
        if (requested.isBefore(LocalDateTime.now().plusHours(1))) {
            throw new IllegalArgumentException("Reservation must be at least 1 hour in advance.");
        }
        if (requested.isAfter(LocalDateTime.now().plusMonths(1))) {
            throw new IllegalArgumentException("Reservation cannot be more than 1 month in advance.");
        }

        int userId = user.getUserId();
        entities.Enums.UserRole userRole = user.getUserRole();
        
        CreateReservationDTO data = new CreateReservationDTO(date, time, guests, userId, userRole);
        RequestDTO request = new RequestDTO(protocol.Commands.CREATE_RESERVATION, data);

        client.sendToServer(request);
    }
    
    public void getOpeningHours() throws IOException {
        RequestDTO request = new RequestDTO(protocol.Commands.GET_OPENING_HOURS, null);
        client.sendToServer(request);
    }
    
    public void loginSubscriber(int subscriberId, String username) throws IOException {
        dto.SubscriberLoginDTO data = new dto.SubscriberLoginDTO(subscriberId, username);
        dto.RequestDTO request = new dto.RequestDTO(protocol.Commands.SUBSCRIBER_LOGIN, data);
        client.sendToServer(request);
    }
    
 // בתוך מחלקת ClientAPI
    public void loginGuest(String phone, String email) throws IOException {
        // יצירת ה-DTO עם הנתונים
        dto.GuestLoginDTO data = new dto.GuestLoginDTO(phone, email);
        
        // יצירת בקשה עם הפקודה המתאימה (וודאי שקיים GUEST_LOGIN ב-protocol.Commands)
        dto.RequestDTO request = new dto.RequestDTO(protocol.Commands.GUEST_LOGIN, data);
        
        // שליחה לשרת
        client.sendToServer(request);
    }
    
    public void recoverSubscriberCode(String username, String phone, String email) throws IOException {

        RecoverSubscriberCodeDTO data =
                new RecoverSubscriberCodeDTO(username, phone, email);

        RequestDTO request =
                new RequestDTO(Commands.RECOVER_SUBSCRIBER_CODE, data);

        client.sendToServer(request);
    }
    
    public void recoverGuestConfirmationCode(String phone, String email, java.time.LocalDateTime reservationDateTime)
            throws IOException {

        if ((phone == null || phone.isBlank()) && (email == null || email.isBlank()))
            throw new IllegalArgumentException("Please enter a phone number or an email.");

        if (reservationDateTime == null)
            throw new IllegalArgumentException("Please select reservation date and time.");

        dto.RecoverGuestConfirmationCodeDTO data =
                new dto.RecoverGuestConfirmationCodeDTO(
                        (phone == null ? "" : phone.trim()),
                        (email == null ? "" : email.trim()),
                        reservationDateTime
                );

        dto.RequestDTO request =
                new dto.RequestDTO(protocol.Commands.RECOVER_GUEST_CONFIRMATION_CODE, data);

        client.sendToServer(request);
    }

    public void registerSubscriber(String username, String firstName, String lastName,
            String phone, String email, int performedById) throws IOException {

			dto.RegisterSubscriberDTO data =
			new dto.RegisterSubscriberDTO(username, firstName, lastName, phone, email, performedById);
			
			dto.RequestDTO request =
			new dto.RequestDTO(protocol.Commands.REGISTER_SUBSCRIBER, data);
			
			client.sendToServer(request);
		}


}
