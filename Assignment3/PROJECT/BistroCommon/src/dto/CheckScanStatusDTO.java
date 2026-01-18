package dto;

import java.io.Serializable;

public class CheckScanStatusDTO implements Serializable {
	private static final long serialVersionUID = 1L;

	private String sessionId;

	public CheckScanStatusDTO(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
}