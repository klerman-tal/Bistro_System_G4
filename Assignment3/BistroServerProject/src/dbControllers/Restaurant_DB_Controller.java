package dbControllers;

import entities.Restaurant;
import entities.Table;

import java.sql.*;
import java.util.ArrayDeque;
import java.util.ArrayList;

public class Restaurant_DB_Controller {

    private Connection conn;

    public Restaurant_DB_Controller(Connection conn) {
        this.conn = conn;
    }

    // ===== LOAD =====
    public void loadTables() throws SQLException {
        Restaurant r = Restaurant.getInstance();

        String sql = "SELECT table_number, seats_amount, is_available FROM restaurant_tables";
        ArrayList<Table> tables = new ArrayList<>();
        ArrayDeque<Table> available = new ArrayDeque<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Table t = new Table();
                t.setTableNumber(rs.getInt("table_number"));
                t.setSeatsAmount(rs.getInt("seats_amount"));
                t.setIsAvailable(rs.getBoolean("is_available"));

                tables.add(t);
                if (t.getIsAvailable()) {
                    available.add(t);
                }
            }
        }

        r.setTables(tables);
        r.setAvailableTables(available);
    }

    // ===== UPDATE =====
    public void updateTableAvailability(Table t) throws SQLException {
        String sql = "UPDATE restaurant_tables SET is_available=? WHERE table_number=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, t.getIsAvailable());
            ps.setInt(2, t.getTableNumber());
            ps.executeUpdate();
        }
    }

    // ===== INSERT / UPDATE =====
    public void saveTable(Table t) throws SQLException {
        String sql = """
            INSERT INTO restaurant_tables (table_number, seats_amount, is_available)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE
              seats_amount = VALUES(seats_amount),
              is_available = VALUES(is_available)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, t.getTableNumber());
            ps.setInt(2, t.getSeatsAmount());
            ps.setBoolean(3, t.getIsAvailable());
            ps.executeUpdate();
        }
    }
 // ===== DELETE =====
    public boolean deleteTable(int tableNumber) throws SQLException {
        String sql = "DELETE FROM restaurant_tables WHERE table_number=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tableNumber);
            return ps.executeUpdate() == 1;
        }
    }
    
}
