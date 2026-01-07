package entities;

import java.io.Serializable;

public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type { INFO, SUCCESS, WARNING, ERROR }

    private final Type type;
    private final String message;

    public Notification(Type type, String message) {
        this.type = type;
        this.message = message;
    }

    public Type getType() { return type; }
    public String getMessage() { return message; }


}
/* 
 * הערה : ככה משתמשים באובייקט בסרבר - 
 * return new Notification(Notification.Type.SUCCESS, "ההזמנה בוצעה בהצלחה ✅");
 * 
 */