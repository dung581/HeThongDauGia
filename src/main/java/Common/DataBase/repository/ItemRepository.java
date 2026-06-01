package Common.DataBase.repository;

import Common.DataBase.DbConnection;
import Common.DataBase.entities.Item;
import Common.Enum.ItemStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ItemRepository {
    DbConnection db = new DbConnection();

    // DB của một số máy chưa có cột minIncrement nên cần tự bổ sung trước khi đọc/ghi item.
    private void ensureMinIncrementColumn() {
        String sql = "ALTER TABLE item ADD COLUMN IF NOT EXISTS minIncrement BIGINT NOT NULL DEFAULT 1";
        try (Connection conn = db.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException("Cannot ensure item.minIncrement column", e);
        }
    }

    private Item map(ResultSet rs) throws Exception {
        Item i = new Item();
        // Map ResultSet sang entity ở một chỗ để các hàm query giữ cùng format dữ liệu.
        i.setId(rs.getLong("id"));
        i.setFullname(rs.getString("fullname"));
        i.setOwner_user_id(rs.getLong("owner_user_id"));
        i.setDescription(rs.getString("description"));
        i.setBeginPrice(rs.getLong("beginPrice"));
        i.setMinIncrement(readLongOrDefault(rs, "minIncrement", 1L));
        i.setMota(rs.getString("mota"));
        i.setStatus(ItemStatus.valueOf(rs.getString("status")));
        return i;
    }

    // DB cũ có thể chưa có cột minIncrement; fallback để màn item không bị trắng toàn bộ.
    private long readLongOrDefault(ResultSet rs, String column, long fallback) {
        try {
            long value = rs.getLong(column);
            return rs.wasNull() || value <= 0 ? fallback : value;
        } catch (Exception e) {
            return fallback;
        }
    }

    public List<Item> getAllItem() {
        ensureMinIncrementColumn();
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM item ORDER BY id DESC";
        // try-with-resources tự đóng connection/statement/resultSet sau khi đọc xong.
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                items.add(map(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    public void saveItem(Item item) {
        ensureMinIncrementColumn();
        String sql = "INSERT INTO item (fullname, owner_user_id, description, beginPrice, minIncrement, mota, status) VALUES (?, ?, ?, ?, ?, ?, ?)";

        // Thứ tự set parameter phải khớp đúng thứ tự cột trong câu INSERT phía trên.
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getFullname());
            ps.setLong(2, item.getOwner_user_id());
            ps.setString(3, item.getDescription());
            ps.setLong(4, item.getBeginPrice());
            ps.setLong(5, item.getMinIncrement());
            ps.setString(6, item.getMota());
            ps.setString(7, item.getStatus().toString());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Item getItemById(long id) {
        ensureMinIncrementColumn();
        String sql = "SELECT * FROM item WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return map(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void update(Item item) {
        ensureMinIncrementColumn();
        String sql = "UPDATE item SET fullname = ?, owner_user_id = ?, description = ?, beginPrice = ?, minIncrement = ?, mota = ?, status = ? WHERE id = ?";

        // Update toàn bộ field hiển thị/quản lý của item, giữ id làm điều kiện cuối cùng.
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, item.getFullname());
            ps.setLong(2, item.getOwner_user_id());
            ps.setString(3, item.getDescription());
            ps.setLong(4, item.getBeginPrice());
            ps.setLong(5, item.getMinIncrement());
            ps.setString(6, item.getMota());
            ps.setString(7, item.getStatus().name());
            ps.setLong(8, item.getId());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("Item not found");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void rejectWithReason(long id, String reason) {
        String sql = "UPDATE item SET status = ?, mota = ? WHERE id = ?";
        // Reject không xóa item; chỉ đổi trạng thái và lưu lý do để seller xem lại.
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ItemStatus.CANCELED.name());
            ps.setString(2, reason);
            ps.setLong(3, id);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("Item not found, id =" + id);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Item> getByStatus(ItemStatus status) {
        ensureMinIncrementColumn();
        List<Item> list = new ArrayList<>();
        String sql = "SELECT * FROM item WHERE status = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    public List<Item> getByOwnerUserId(long ownerUserId) {
        ensureMinIncrementColumn();
        List<Item> list = new ArrayList<>();
        String sql = "SELECT * FROM item WHERE owner_user_id = ? ORDER BY id DESC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, ownerUserId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

}
