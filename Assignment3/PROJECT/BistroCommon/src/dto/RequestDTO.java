package dto;

import java.io.Serializable;

import protocol.Commands;

/**
 * Data Transfer Object (DTO) used to encapsulate a request
 * sent from the client to the server.
 * <p>
 * This object contains the command to be executed and the associated data.
 * </p>
 */
public class RequestDTO implements Serializable{
	
	private Commands command;
	private Object data;
	
	public RequestDTO(Commands command, Object data) {
		this.command = command;
		this.data = data;
	}
	
	public Commands getCommand() {
		return command;
	}
	public Object getData() {
		return data;
	}
	
	
	
}