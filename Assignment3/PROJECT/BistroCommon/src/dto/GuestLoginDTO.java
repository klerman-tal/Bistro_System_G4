package dto;

import java.io.Serializable;

public class GuestLoginDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String phone;
    private String email;

    public GuestLoginDTO(String phone, String email) {
        this.phone = phone;
        this.email = email;
    }

    public String getPhone() { return phone; }
    public String getEmail() { return email; }
}