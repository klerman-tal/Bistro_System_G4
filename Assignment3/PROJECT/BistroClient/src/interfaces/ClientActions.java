package interfaces;

import java.util.ArrayList;

/**
 * Interface for actions that the GUI can invoke on the client.
 */
public interface ClientActions {
    void sendToServer(ArrayList<String> msg);

	boolean loginGuest(String phone, String email);

	boolean loginSubscriber(int subscriberId, String username);
}
