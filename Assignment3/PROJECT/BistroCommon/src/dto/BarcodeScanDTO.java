package dto;

import java.io.Serializable;

/**
 * DTO המייצג סריקה של כרטיס מנוי חיצוני.
 * משמש להעברת מספר המנוי בלבד מהסימולטור לשרת.
 */
public class BarcodeScanDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String subscriberId;

    /**
     * @param subscriberId מספר המנוי שנסרק ב"מכשיר החיצוני"
     */
    public BarcodeScanDTO(String subscriberId) {
        this.subscriberId = subscriberId;
    }

    // Getters & Setters
    public String getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(String subscriberId) {
        this.subscriberId = subscriberId;
    }

    @Override
    public String toString() {
        return "BarcodeScanDTO [subscriberId=" + subscriberId + "]";
    }
}