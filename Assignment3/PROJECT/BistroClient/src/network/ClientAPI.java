package network;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

import application.ChatClient;
import dto.CreateReservationDTO;
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
}
