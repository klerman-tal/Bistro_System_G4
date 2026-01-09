package entities;

import java.io.Serializable; // ✨ ייבוא הממשק

public class User implements Serializable { // ✨ הוספת הממשק
    private static final long serialVersionUID = 1L; // ✨ מזהה גרסה (מומלץ)
    
    // // private Integer userId; // הקוד הקודם
    protected Integer userId; // שינוי ל-protected כדי ש-Subscriber יוכל לגשת
    protected String phoneNumber;
    protected String email;
    protected Enums.UserRole userRole;
    protected Reservation activeReservation;
    
    public User() {}

    public User(Integer userId, String phoneNumber, String email, Enums.UserRole userRole) {
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.userRole = userRole;
    }

    public User(String phone, String email) {
        this.phoneNumber = phone;
        this.email = email;
    }

    // Getters & Setters נשארים אותו דבר...
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Enums.UserRole getUserRole() { return userRole; }
    public void setUserRole(Enums.UserRole userRole) { this.userRole = userRole; }
}