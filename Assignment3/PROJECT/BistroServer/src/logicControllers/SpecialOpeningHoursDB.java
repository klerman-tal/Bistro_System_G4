package logicControllers;

import entities.SpecialOpeningHours;
import java.sql.*;
import java.time.LocalDate;

/**
 * Provides database access for special (date-specific) opening hours.
 * <p>
 * This class is intended to encapsulate persistence operations for {@link SpecialOpeningHours}
 * records that override the regular weekly opening hours for specific dates.
 * </p>
 * <p>
 * Currently, it stores a JDBC {@link Connection} reference for executing SQL queries.
 * </p>
 */
public class SpecialOpeningHoursDB {

    private Connection conn;

    /**
     * Constructs a SpecialOpeningHoursDB with the given JDBC connection.
     *
     * @param conn active JDBC connection used for database operations
     */
    public SpecialOpeningHoursDB(Connection conn) {
        this.conn = conn;
    }

}
