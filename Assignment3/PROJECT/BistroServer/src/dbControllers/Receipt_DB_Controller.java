package dbControllers;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;

import entities.Enums;
import entities.Receipt;

/**
 * DB controller for receipts.
 * Receipt is created at check-in (random amount),
 * and marked as paid at payment time.
 */
public class Receipt_DB_Controller {

    private final Connection conn;

    public Receipt_DB_Controller(Connection conn) {
        this.conn = conn;
    }

    // =====================================================
    // TABLE SETUP
    // =====================================================

    public void createReceiptsTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS receipts (
                receipt_id INT AUTO_INCREMENT PRIMARY KEY,
                reservation_id INT NOT NULL,
                created_at DATETIME NOT NULL,
                amount DECIMAL(10,2) NOT NULL,
                is_paid TINYINT(1) NOT NULL DEFAULT 0,
                paid_at DATETIME NULL,
                payment_type ENUM('CreditCard','Cash') NULL,

                created_by_user_id INT NULL,
                created_by_role ENUM(
                    'RandomClient',
                    'Subscriber',
                    'RestaurantAgent',
                    'RestaurantManager'
                ) NULL,

                UNIQUE (reservation_id),
                INDEX (reservation_id),
                INDEX (is_paid),
                INDEX (created_at),
                INDEX (paid_at)
            );
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =====================================================
    // INSERT / GET
    // =====================================================

    /**
     * Creates a receipt only if it does not exist for this reservation_id (idempotent).
     * Returns receipt_id if inserted, or existing receipt_id if already exists, or -1 on error.
     */
    public int createReceiptIfNotExists(Receipt r) throws SQLException {
        if (r == null) return -1;
        if (r.getReservationId() <= 0) return -1;

        // 1) If exists -> return existing id
        Integer existingId = getReceiptIdByReservationId(r.getReservationId());
        if (existingId != null) return existingId;

        // 2) Insert
        String sql = """
            INSERT INTO receipts
            (reservation_id, created_at, amount, is_paid, paid_at, payment_type, created_by_user_id, created_by_role)
            VALUES (?, ?, ?, 0, NULL, NULL, ?, ?);
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getReservationId());

            LocalDateTime createdAt = (r.getCreatedAt() != null) ? r.getCreatedAt() : LocalDateTime.now();
            ps.setObject(2, createdAt);

            BigDecimal amount = (r.getAmount() != null) ? r.getAmount() : BigDecimal.ZERO;
            ps.setBigDecimal(3, amount);

            if (r.getCreatedByUserId() != null) ps.setInt(4, r.getCreatedByUserId());
            else ps.setNull(4, Types.INTEGER);

            if (r.getCreatedByRole() != null) ps.setString(5, r.getCreatedByRole().name());
            else ps.setNull(5, Types.VARCHAR);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        return -1;
    }

    public Receipt getReceiptByReservationId(int reservationId) throws SQLException {
        String sql = "SELECT * FROM receipts WHERE reservation_id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRowToReceipt(rs);
            }
        }
        return null;
    }

    private Integer getReceiptIdByReservationId(int reservationId) throws SQLException {
        String sql = "SELECT receipt_id FROM receipts WHERE reservation_id = ? LIMIT 1;";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("receipt_id");
            }
        }
        return null;
    }

    // =====================================================
    // PAYMENT UPDATE
    // =====================================================

    /**
     * Marks receipt as paid (idempotent).
     * Returns true if updated from unpaid -> paid.
     */
    public boolean markReceiptPaid(int reservationId, Enums.TypeOfPayment paymentType, LocalDateTime paidAt) throws SQLException {
        String sql = """
            UPDATE receipts
            SET is_paid = 1,
                paid_at = ?,
                payment_type = ?
            WHERE reservation_id = ?
              AND is_paid = 0;
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, (paidAt != null) ? paidAt : LocalDateTime.now());
            ps.setString(2, paymentType != null ? paymentType.name() : null);
            ps.setInt(3, reservationId);
            return ps.executeUpdate() > 0;
        }
    }

    // =====================================================
    // MAPPING
    // =====================================================

    private Receipt mapRowToReceipt(ResultSet rs) throws SQLException {
        Receipt r = new Receipt();

        r.setReceiptId(rs.getInt("receipt_id"));
        r.setReservationId(rs.getInt("reservation_id"));

        LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);
        r.setCreatedAt(createdAt);

        BigDecimal amount = rs.getBigDecimal("amount");
        r.setAmount(amount);

        r.setPaid(rs.getInt("is_paid") == 1);

        LocalDateTime paidAt = rs.getObject("paid_at", LocalDateTime.class);
        r.setPaidAt(paidAt);

        String pt = rs.getString("payment_type");
        if (pt != null) {
            r.setPaymentType(Enums.TypeOfPayment.valueOf(pt));
        }

        int createdBy = rs.getInt("created_by_user_id");
        if (!rs.wasNull()) r.setCreatedByUserId(createdBy);

        String role = rs.getString("created_by_role");
        if (role != null) r.setCreatedByRole(Enums.UserRole.valueOf(role));

        return r;
    }
}
