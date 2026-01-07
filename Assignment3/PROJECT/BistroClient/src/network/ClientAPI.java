package network;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;

import application.ChatClient;
import dto.CreateReservationDTO;
import dto.RequestDTO;
import protocol.Commands;
import entities.User;

public class ClientAPI {

    private final ChatClient client;

    public ClientAPI(ChatClient client) {
        this.client = client;
    }

    public void createReservation(
            LocalDate date,
            LocalTime time,
            int guests,
            User user) throws IOException {

        if (date == null || time == null || user == null)
            throw new IllegalArgumentException("Invalid reservation data");

        if (guests <= 0)
            throw new IllegalArgumentException("Guests must be positive");

        int userId = user.getUserId(); // ⭐ זה השינוי הקריטי

        CreateReservationDTO data =
                new CreateReservationDTO(date, time, guests, userId);

        RequestDTO request =
                new RequestDTO(Commands.CREATE_RESERVATION, data);

        client.sendToServer(request);
    }
}
