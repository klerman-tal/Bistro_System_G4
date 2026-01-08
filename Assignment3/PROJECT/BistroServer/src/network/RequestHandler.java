package network;

import ocsf.server.ConnectionToClient;
import dto.RequestDTO;

@FunctionalInterface
public interface RequestHandler {
    void handle(RequestDTO request, ConnectionToClient client) throws Exception;
}
