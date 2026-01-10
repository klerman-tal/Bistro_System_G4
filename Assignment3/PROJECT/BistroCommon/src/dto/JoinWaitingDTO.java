package dto;

import java.io.Serializable;
import entities.Enums.UserRole;

public class JoinWaitingDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int guests;
    private final int userId;
    private final UserRole role;

    public JoinWaitingDTO(int guests, int userId, UserRole role) {
        this.guests = guests;
        this.userId = userId;
        this.role = role;
    }

    public int getGuests() { return guests; }
    public int getUserId() { return userId; }
    public UserRole getRole() { return role; }
}
