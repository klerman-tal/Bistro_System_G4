package dto;

import java.io.Serializable;

/**
 * Data Transfer Object representing a barcode scan event.
 * <p>
 * This DTO is used to transfer a scanned subscriber identifier from an external
 * barcode scanner simulator to the server.
 * </p>
 */
public class BarcodeScanDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private String subscriberId;

	/**
	 * Constructs a BarcodeScanDTO with the scanned subscriber identifier.
	 *
	 * @param subscriberId the identifier of the subscriber obtained from the
	 *                     barcode scan
	 */
	public BarcodeScanDTO(String subscriberId) {
		this.subscriberId = subscriberId;
	}

	/**
	 * Returns the scanned subscriber identifier.
	 *
	 * @return subscriber identifier
	 */
	public String getSubscriberId() {
		return subscriberId;
	}

	/**
	 * Updates the subscriber identifier.
	 *
	 * @param subscriberId new subscriber identifier
	 */
	public void setSubscriberId(String subscriberId) {
		this.subscriberId = subscriberId;
	}

	/**
	 * Returns a string representation of this DTO.
	 *
	 * @return string representation containing the subscriber identifier
	 */
	@Override
	public String toString() {
		return "BarcodeScanDTO [subscriberId=" + subscriberId + "]";
	}
}
