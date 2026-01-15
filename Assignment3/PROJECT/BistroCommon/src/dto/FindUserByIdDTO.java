package dto;

import java.io.Serializable;

public class FindUserByIdDTO implements Serializable {
    private int userId;

    public FindUserByIdDTO(int userId) {
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }
}
