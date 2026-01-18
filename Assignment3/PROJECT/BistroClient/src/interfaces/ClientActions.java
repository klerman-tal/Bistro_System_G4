package interfaces;

import java.util.ArrayList;

/**
 * Defines the actions that the GUI layer can invoke on the client layer.
 *
 * <p>Implementations of this interface are responsible for translating UI actions
 * into client operations, such as sending messages to the server and performing
 * login requests.</p>
 */
public interface ClientActions {

    /**
     * Sends a raw message payload to the server.
     *
     * @param msg a list of strings representing the message payload
     */
    void sendToServer(ArrayList<String> msg);

    /**
     * Attempts to log in as a guest using contact details.
     *
     * @param phone the guest phone number
     * @param email the guest email address
     * @return {@code true} if the login request was accepted/started successfully; otherwise {@code false}
     */
    boolean loginGuest(String phone, String email);

    /**
     * Attempts to log in as a subscriber using an id and username.
     *
     * @param subscriberId the subscriber identifier
     * @param username     the subscriber username
     * @return {@code true} if the login request was accepted/started successfully; otherwise {@code false}
     */
    boolean loginSubscriber(int subscriberId, String username);
}
