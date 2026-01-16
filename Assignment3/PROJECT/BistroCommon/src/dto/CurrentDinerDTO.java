package dto;

import java.io.Serializable;

import entities.Enums.UserRole;

public class CurrentDinerDTO implements Serializable {
    private int createdBy;
    private String createdByRole;
    private Integer tableNumber;

    public CurrentDinerDTO(int createdBy, String createdByRole, Integer tableNumber) {
        this.createdBy = createdBy;
        this.createdByRole = createdByRole;
        this.tableNumber = tableNumber;
    }

    public int getCreatedBy() { return createdBy; }
    public String getCreatedByRole() { return createdByRole; }
    public Integer getTableNumber() { return tableNumber; }
}
