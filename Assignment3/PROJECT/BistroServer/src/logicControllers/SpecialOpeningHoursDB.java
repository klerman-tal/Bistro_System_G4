package logicControllers;

import entities.SpecialOpeningHours;
import java.sql.*;
import java.time.LocalDate;

public class SpecialOpeningHoursDB {

    private Connection conn;

    public SpecialOpeningHoursDB(Connection conn) {
        this.conn = conn;
    }

}