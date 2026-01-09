package entities;

import entities.Enums.UserRole;

/**
 * Represents a subscriber entity inheriting from User.
 */
public class Subscriber extends User { // ✨ הוספת הירושה

    // ====== Fields ======
    // שדות ה-ID, ה-Phone, ה-Email וה-Role כבר קיימים ב-User, לכן מחקנו אותם מכאן כדי למנוע כפילות
    private String username;
    private String firstName;
    private String lastName;

    // ====== Constructors ======

    /** Empty constructor required for DB mapping */
    public Subscriber() {
        super();
    }

    /** Full constructor */
    public Subscriber(
            int subscriberId,
            String username,
            String firstName,
            String lastName,
            String phone,
            String email,
            UserRole role
    ) {
        // ✨ קריאה לקונסטרקטור של האבא (User)
        super(subscriberId, phone, email, role); 
        
        // אתחול השדות הייחודיים ל-Subscriber
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // ====== Getters & Setters ======

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    // שימי לב: Getters כמו getSubscriberId או getPhone יעבדו אוטומטית כי הם יורשים מהאבא (User)
}