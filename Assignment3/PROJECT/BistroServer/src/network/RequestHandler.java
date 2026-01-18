package network;

import ocsf.server.ConnectionToClient;
import dto.RequestDTO;

/**
 * Functional interface representing a server-side request handler.
 * <p>
 * Each implementation of this interface is responsible for handling a specific
 * type of client request, identified by a command in the application protocol.
 * </p>
 * <p>
 * Implementations typically:
 * <ul>
 * <li>Extract and validate request data</li>
 * <li>Delegate business logic to the appropriate controller</li>
 * <li>Send a {@link dto.ResponseDTO} back to the client</li>
 * </ul>
 * </p>
 */
@FunctionalInterface
public interface RequestHandler {

	/**
	 * Handles a client request received by the server.
	 *
	 * @param request the incoming request DTO containing command and payload
	 * @param client  the client connection associated with this request
	 * @throws Exception if an unrecoverable error occurs during handling
	 */
	void handle(RequestDTO request, ConnectionToClient client) throws Exception;
}
