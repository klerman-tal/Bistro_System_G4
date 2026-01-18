package dto;

import java.io.Serializable;

public class LoginQRCodeResponseDTO implements Serializable {
	private static final long serialVersionUID = 1L;

	private String sessionId;
	private byte[] qrCodeImage; // התמונה של הברקוד כמערך בתים

	public LoginQRCodeResponseDTO(String sessionId, byte[] qrCodeImage) {
		this.sessionId = sessionId;
		this.qrCodeImage = qrCodeImage;
	}

	public String getSessionId() {
		return sessionId;
	}

	public byte[] getQrCodeImage() {
		return qrCodeImage;
	}
}
