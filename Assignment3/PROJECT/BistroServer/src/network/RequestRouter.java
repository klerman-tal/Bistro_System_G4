	package network;
	
	import java.util.EnumMap;
	import java.util.Map;
	
	import ocsf.server.ConnectionToClient;
	import dto.RequestDTO;
	import dto.ResponseDTO;
	import protocol.Commands;
	
	public class RequestRouter {
	
	    private final Map<Commands, RequestHandler> handlers = new EnumMap<>(Commands.class);
	
	    public void register(Commands cmd, RequestHandler handler) {
	        handlers.put(cmd, handler);
	    }
	
	    public void route(RequestDTO request, ConnectionToClient client) {
	        RequestHandler handler = handlers.get(request.getCommand());
	
	        if (handler == null) {
	            safeSend(client, new ResponseDTO(false, "Unknown command: " + request.getCommand(), null));
	            return;
	        }
	
	        try {
	            handler.handle(request, client);
	        } catch (Exception e) {
	            e.printStackTrace();
	            safeSend(client, new ResponseDTO(false, "Server error", null));
	        }
	    }
	
	    private void safeSend(ConnectionToClient client, Object msg) {
	        try {
	            client.sendToClient(msg);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	}
