package network;

import java.util.HashMap;
import java.util.Map;

import dto.RequestDTO;
import dto.ResponseDTO;
import ocsf.server.ConnectionToClient;
import protocol.Commands;

/**
 * Central router responsible for dispatching incoming client requests to their
 * corresponding {@link RequestHandler} implementations.
 * <p>
 * The router maintains a mapping between protocol {@link Commands} and concrete
 * request handlers. For each incoming request, it resolves the appropriate
 * handler and delegates the handling logic.
 * </p>
 * <p>
 * This class acts as the main entry point for server-side request processing
 * and enforces a clean separation between network transport and business logic.
 * </p>
 */
public class RequestRouter {

	/**
	 * Registry mapping protocol commands to their corresponding handlers.
	 */
	private final Map<Commands, RequestHandler> handlers = new HashMap<>();

	/**
	 * Registers a request handler for a specific protocol command.
	 * <p>
	 * This method is typically invoked during server initialization to configure
	 * the routing table.
	 * </p>
	 *
	 * @param cmd     the protocol command to handle
	 * @param handler the handler responsible for processing this command
	 */
	public void register(Commands cmd, RequestHandler handler) {
		handlers.put(cmd, handler);
	}

	/**
	 * Routes an incoming request to the appropriate handler based on its protocol
	 * command.
	 * <p>
	 * If no handler is registered for the requested command, an error response is
	 * sent back to the client.
	 * </p>
	 *
	 * @param request the incoming request containing command and payload
	 * @param client  the client connection associated with the request
	 */
	public void route(RequestDTO request, ConnectionToClient client) {

		RequestHandler handler = handlers.get(request.getCommand());

		// No handler registered for this command
		if (handler == null) {
			safeSend(client, new ResponseDTO(false, "Unknown command", null));
			return;
		}

		try {
			// Delegate request processing to the resolved handler
			handler.handle(request, client);
		} catch (Exception e) {
			e.printStackTrace();
			safeSend(client, new ResponseDTO(false, "Server error", null));
		}
	}

	/**
	 * Safely sends a message to the client connection.
	 * <p>
	 * This helper method centralizes error handling for network transmission and
	 * prevents the routing flow from crashing due to I/O errors.
	 * </p>
	 *
	 * @param client the client connection
	 * @param msg    the message to send
	 */
	private void safeSend(ConnectionToClient client, Object msg) {
		try {
			client.sendToClient(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
