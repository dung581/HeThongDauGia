package Server.dao;

import Common.Model.Item;
import Common.Model.item.Art;
import Common.Model.item.Electronics;
import Common.Model.item.Vehicle;
import Exceptions.DataAccessException;
import Server.db.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object cho Item — CRUD trên bảng `items` (single-table inheritance).
 *
 * Nguyên tắc thiết kế:
 *   1. Mỗi method MỞ-DÙNG-ĐÓNG connection riêng (try-with-resources).
 *      Lý do: connection KHÔNG thread-safe, share giữa các thread = race condition.
 *   2. PreparedStatement luôn dùng — tránh SQL injection.
 *   3. SQLException được wrap thành DataAccessException (RuntimeException) để
 *      service layer không phải biết về JDBC.
 *   4. mapRow() là điểm DUY NHẤT biết cách dựng Item từ ResultSet — đa hình
 *      dispatch theo cột item_type.
 */
public class ItemDAO {

    private final DbConnection dbConnection;

    public ItemDAO() {
        this(DbConnection.getInstance());
    }

    /** Constructor cho test — inject DbConnection mock được. */
    public ItemDAO(DbConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    // ===== Câu SQL — đặt hằng để dễ review =====

    private static final String SQL_INSERT = """
            INSERT INTO dbo.items
                (iid, seller_id, item_type, item_name, description,
                 start_price, current_price, start_time, end_time, min_increment,
                 artist_name, license_plate, engine_type, mileage, warranty_months)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SQL_SELECT_ALL_FIELDS = """
            SELECT iid, seller_id, item_type, item_name, description,
                   start_price, current_price, start_time, end_time, min_increment,
                   artist_name, license_plate, engine_type, mileage, warranty_months
            FROM dbo.items
            """;

    private static final String SQL_FIND_BY_ID    = SQL_SELECT_ALL_FIELDS + " WHERE iid = ?";
    private static final String SQL_FIND_ALL      = SQL_SELECT_ALL_FIELDS + " ORDER BY created_at DESC";
    private static final String SQL_FIND_BY_SELLER= SQL_SELECT_ALL_FIELDS + " WHERE seller_id = ? ORDER BY created_at DESC";

    private static final String SQL_UPDATE_BASIC = """
            UPDATE dbo.items
               SET item_name = ?, description = ?
             WHERE iid = ?
            """;

    private static final String SQL_DELETE = "DELETE FROM dbo.items WHERE iid = ?";

    // =================================================================
    // CREATE
    // =================================================================

    /**
     * Lưu Item mới vào DB. Item phải có IID đã sinh sẵn (UUID).
     * @return số dòng được insert (luôn 1 nếu thành công)
     */
    public int insert(Item item) {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setString(1, item.getIID());
            ps.setString(2, item.getSellerId());
            ps.setString(3, item.getItemType());
            ps.setString(4, item.getItemName());
            ps.setString(5, item.getDescription());
            ps.setLong  (6, item.getStartPrice());
            ps.setLong  (7, item.getCurrentPrice());
            ps.setTimestamp(8, Timestamp.valueOf(item.getStartTime()));
            ps.setTimestamp(9, Timestamp.valueOf(item.getEndTime()));
            ps.setLong  (10, item.getMinIncrement());

            // Cột riêng theo loại — set NULL cho cột không thuộc loại
            bindTypeSpecificColumns(ps, item);

            return ps.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException(
                    "Lỗi insert item " + item.getIID() + ": " + e.getMessage(), e);
        }
    }

    private void bindTypeSpecificColumns(PreparedStatement ps, Item item) throws SQLException {
        // Mặc định set NULL hết, sau đó override theo loại
        ps.setNull(11, Types.NVARCHAR); // artist_name
        ps.setNull(12, Types.VARCHAR);  // license_plate
        ps.setNull(13, Types.VARCHAR);  // engine_type
        ps.setNull(14, Types.INTEGER);  // mileage
        ps.setNull(15, Types.INTEGER);  // warranty_months

        if (item instanceof Art art) {
            ps.setString(11, art.getArtistName());
        } else if (item instanceof Vehicle vehicle) {
            ps.setString(12, vehicle.getLicensePlate());
            ps.setString(13, vehicle.getEngineType());
            ps.setInt   (14, vehicle.getMileage());
        } else if (item instanceof Electronics elec) {
            ps.setInt   (15, elec.getWarrantyMonths());
        } else {
            throw new IllegalArgumentException(
                    "Loại item chưa hỗ trợ trong DAO: " + item.getClass().getSimpleName());
        }
    }

    // =================================================================
    // READ
    // =================================================================

    public Optional<Item> findById(String iid) {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setString(1, iid);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi findById " + iid + ": " + e.getMessage(), e);
        }
    }

    public List<Item> findAll() {
        return queryList(SQL_FIND_ALL, ps -> {});
    }

    public List<Item> findBySellerId(String sellerId) {
        return queryList(SQL_FIND_BY_SELLER, ps -> ps.setString(1, sellerId));
    }

    /** Helper rút gọn pattern query nhiều dòng. */
    private List<Item> queryList(String sql, SqlBinder binder) {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<Item> items = new ArrayList<>();
                while (rs.next()) items.add(mapRow(rs));
                return items;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi query: " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    // =================================================================
    // UPDATE — chỉ cho update name + description
    // (currentPrice update qua placeBid, không qua DAO trực tiếp)
    // =================================================================

    public int updateBasicInfo(String iid, String newName, String newDescription) {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_BASIC)) {

            ps.setString(1, newName);
            ps.setString(2, newDescription);
            ps.setString(3, iid);
            return ps.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Lỗi update item " + iid + ": " + e.getMessage(), e);
        }
    }

    // =================================================================
    // DELETE
    // =================================================================

    public int delete(String iid) {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setString(1, iid);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi delete item " + iid + ": " + e.getMessage(), e);
        }
    }

    // =================================================================
    // ROW MAPPER — đa hình dispatch theo item_type
    // =================================================================

    private Item mapRow(ResultSet rs) throws SQLException {
        String iid           = rs.getString("iid");
        String sellerId      = rs.getString("seller_id");
        String type          = rs.getString("item_type");
        String name          = rs.getString("item_name");
        String description   = rs.getString("description");
        long startPrice      = rs.getLong("start_price");
        long currentPrice    = rs.getLong("current_price");
        LocalDateTime start  = rs.getTimestamp("start_time").toLocalDateTime();
        LocalDateTime end    = rs.getTimestamp("end_time").toLocalDateTime();
        long minIncrement    = rs.getLong("min_increment");

        switch (type) {
            case "ART":
                return new Art(iid, sellerId, name, description,
                        startPrice, currentPrice, start, end, minIncrement,
                        rs.getString("artist_name"));

            case "VEHICLE":
                return new Vehicle(iid, sellerId, name, description,
                        startPrice, currentPrice, start, end, minIncrement,
                        rs.getString("license_plate"),
                        rs.getString("engine_type"),
                        rs.getInt("mileage"));

            case "ELECTRONICS":
                return new Electronics(iid, sellerId, name, description,
                        startPrice, currentPrice, start, end, minIncrement,
                        rs.getInt("warranty_months"));

            default:
                throw new SQLException("item_type không hợp lệ trong DB: " + type);
        }
    }
}