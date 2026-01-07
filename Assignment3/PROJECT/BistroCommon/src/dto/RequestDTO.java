package dto;

import java.io.Serializable;

import protocol.Commands;

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
