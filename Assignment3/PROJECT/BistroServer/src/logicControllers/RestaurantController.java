package logicControllers;

import entities.Restaurant;
import entities.Table;
import java.sql.SQLException;
import dbControllers.Restaurant_DB_Controller;

public class RestaurantController {

    private final Restaurant_DB_Controller db;
    private final Restaurant restaurant;

    public RestaurantController(Restaurant_DB_Controller db) {
        this.db = db;
        this.restaurant = Restaurant.getInstance();
    }

    public void loadTablesFromDb() throws SQLException {
        db.loadTables(); // כבר ממלא את ה-Restaurant singleton
    }

    public boolean addTable(Table newTable) throws SQLException {
        if (newTable == null) return false;

        for (Table t : restaurant.getTables()) {
            if (t.getTableNumber() == newTable.getTableNumber()) {
                return false;
            }
        }

        restaurant.getTables().add(newTable);
        if (newTable.getIsAvailable()) {
            restaurant.getAvailableTables().add(newTable);
        }

        db.saveTable(newTable);
        return true;
    }

    public boolean removeTable(int tableNumber) throws SQLException {
        Table toRemove = null;

        for (Table t : restaurant.getTables()) {
            if (t.getTableNumber() == tableNumber) {
                toRemove = t;
                break;
            }
        }
        if (toRemove == null) return false;

        restaurant.getTables().remove(toRemove);
        restaurant.getAvailableTables().remove(toRemove);

        db.deleteTable(tableNumber); // אחרי שהוספת את המתודה ב-DB controller
        return true;
    }

    public Table getAvailableTableAndReserve() throws SQLException {
        Table t = restaurant.GetAvailableTable();
        t.setIsAvailable(false);
        db.updateTableAvailability(t); // עובד עם החתימה שלך
        return t;
    }

    public boolean releaseTable(int tableNumber) throws SQLException {
        Table found = null;

        for (Table t : restaurant.getTables()) {
            if (t.getTableNumber() == tableNumber) {
                found = t;
                break;
            }
        }
        if (found == null) return false;

        if (!found.getIsAvailable()) {
            found.setIsAvailable(true);
            restaurant.getAvailableTables().add(found);
        }

        db.updateTableAvailability(found); // עובד עם החתימה שלך
        return true;
    }

    public void syncAllTablesToDb() throws SQLException {
        for (Table t : restaurant.getTables()) {
            db.saveTable(t);
        }
    }
    public void saveOrUpdateTable(Table t) throws SQLException {
        if (t == null) return;

        // עדכון ה-Singleton בזיכרון (אם קיים - לשנות, אם לא - להוסיף)
        Table existing = null;
        for (Table x : restaurant.getTables()) {
            if (x.getTableNumber() == t.getTableNumber()) {
                existing = x;
                break;
            }
        }

        if (existing == null) {
            restaurant.getTables().add(t);
            if (t.getIsAvailable()) restaurant.getAvailableTables().add(t);
        } else {
            existing.setSeatsAmount(t.getSeatsAmount());

            boolean wasAvailable = existing.getIsAvailable();
            existing.setIsAvailable(t.getIsAvailable());

            // לעדכן רשימת availableTables בהתאם
            if (!wasAvailable && existing.getIsAvailable()) {
                restaurant.getAvailableTables().add(existing);
            } else if (wasAvailable && !existing.getIsAvailable()) {
                restaurant.getAvailableTables().remove(existing);
            }
        }

        // UPSERT DB
        db.saveTable(t);
    }

}
