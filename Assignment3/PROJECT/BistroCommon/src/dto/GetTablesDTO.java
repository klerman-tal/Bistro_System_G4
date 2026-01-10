package dto;

import java.io.Serializable;

/**
 * DTO לבקשת כל השולחנות של המסעדה.
 * אין בו שדות – עצם הבקשה מספיקה.
 */
public class GetTablesDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    public GetTablesDTO() {
        // empty on purpose
    }
}
