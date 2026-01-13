package network;

import dto.ResponseDTO;

public interface ClientResponseHandler {
    void handleResponse(ResponseDTO response);
    void handleConnectionError(Exception e);
    void handleConnectionClosed();
}