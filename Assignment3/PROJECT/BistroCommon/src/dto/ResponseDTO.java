package dto;

import java.io.Serializable;

/**
 * Generic Data Transfer Object (DTO) used to represent a standard
 * response returned from the server.
 * <p>
 * This object encapsulates the outcome of a request, including
 * a success indicator, an informational message, and an optional
 * payload containing response data.
 * </p>
 */
public class ResponseDTO implements Serializable{
	
	private boolean success;
	private String message;
	private Object data;
	
	public ResponseDTO(boolean success, String message, Object data) {
		this.success = success;
		this.message = message;
		this.data = data;
	}
	
	
	public boolean isSuccess() {
		return success;
	}
	public String getMessage() {
		return message;
	}
	public Object getData() {
		return data;
	}
}
