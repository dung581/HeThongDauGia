package Common;

import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class ItemDAO {
    private Connection connection;
    private Gson gson = new Gson(); // Thư viện xử lý JSON

    // Inject Connection qua constructor
    public ItemDAO(Connection connection) {
        this.connection = connection;
    }

    public boolean insertItem(Item item, String sellerId) {
        String sql = "INSERT INTO items (iid, seller_id, type, item_name, description, start_price, " +
                "current_price, start_time, end_time, min_increment, extra_data) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, item.getIID());
            pstmt.setString(2, sellerId);
            pstmt.setString(3, item.getItemType());
            pstmt.setString(4, item.getItemName());
            pstmt.setString(5, item.getDescription());
            pstmt.setLong(6, item.getStartPrice());
            pstmt.setLong(7, item.getCurrentPrice());
            pstmt.setTimestamp(8, Timestamp.valueOf(item.getStartTime()));
            pstmt.setTimestamp(9, Timestamp.valueOf(item.getEndTime()));
            pstmt.setLong(10, item.getMinIncrement());

            String extraDataJson = extractExtraData(item);
            pstmt.setString(11, extraDataJson);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi khi insert Item: " + e.getMessage());
            return false;
        }
    }

    // Hàm helper trích xuất các thuộc tính riêng của từng loại sản phẩm thành chuỗi JSON
    private String extractExtraData(Item item) {
        Map<String, Object> extraMap = new HashMap<>();

        if (item instanceof Vehicle) {
            Vehicle v = (Vehicle) item;
            extraMap.put("licensePlate", v.getLicensePlate());
            extraMap.put("engineType", v.getEngineType());
            extraMap.put("mileage", v.getMileage());
        }
        else if(item instanceof Art){
            Art a = (Art) item;
            extraMap.put("artistName", a.getArtistName());
        }
        else if (item instanceof Electronics){
            Electronics e = (Electronics) item;
            extraMap.put("warrantyMonths", e.getWarrantyMonths());
        }

        return gson.toJson(extraMap); // Biến Map thành chuỗi JSON (VD: {"mileage":1000,"engineType":"Xăng"})
    }
}