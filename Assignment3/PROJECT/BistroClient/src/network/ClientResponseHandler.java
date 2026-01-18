package network;

import dto.ResponseDTO;

/**
 * Defines a callback contract for handling responses and connection-related
 * events on the client side.
 *
 * <p>
 * Implementations of this interface are responsible for reacting to server
 * responses, handling connection errors, and responding to connection
 * termination events.
 * </p>
 *
 * <p>
 * This interface is typically used by the networking layer to decouple
 * low-level communication logic from higher-level application behavior.
 * </p>
 */
public interface ClientResponseHandler {

	/**
	 * Handles a successful response received from the server.
	 *
	 * @param response the response object received from the server
	 */
	void handleResponse(ResponseDTO response);

	/**
	 * Handles an error that occurred during the client-server communication.
	 *
	 * @param e the exception representing the connection or communication error
	 */
	void handleConnectionError(Exception e);

	/**
	 * Handles the event of an unexpected or intentional connection closure.
	 */
	void handleConnectionClosed();
}
