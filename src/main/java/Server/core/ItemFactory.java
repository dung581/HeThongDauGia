package Server.core;

import Common.Enum.ItemType;
import Common.Model.item.Item;
import Common.Model.item.Art;
import Common.Model.item.Electronics;
import Common.Model.item.Vehicle;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Factory Method tạo Item từ Map dữ liệu (thường nhận từ JSON / form).
 *
 * FIX so với bản cũ:
 *   1. Thêm sellerId (bắt buộc - dùng để check quyền).
 *   2. Validate các trường bắt buộc (ném IllegalArgumentException sớm
 *      thay vì để NullPointerException ở downstream).
 *   3. Cập nhật theo constructor mới của Vehicle (đổi thứ tự tham số
 *      cho nhất quán: IID, sellerId luôn đầu).
 */
public final class ItemFactory {

    private ItemFactory() {
        throw new UnsupportedOperationException("Đây là lớp factory, không khởi tạo object!");
    }

    public static Item createItem(String typeString, Map<String, Object> data) {
        Objects.requireNonNull(typeString, "typeString không được null");
        Objects.requireNonNull(data, "data không được null");

        ItemType type = ItemType.fromString(typeString);

        // Trường chung
        String iid         = requireString(data, "IID");
        String sellerId    = requireString(data, "sellerId");
        String itemName    = requireString(data, "itemName");
        String description = (String) data.getOrDefault("description", "");
        long startPrice    = parseLong(data.get("startPrice"));
        long currentPrice  = data.containsKey("currentPrice")
                ? parseLong(data.get("currentPrice"))
                : startPrice;
        long minIncrement  = parseLong(data.get("minIncrement"));

        LocalDateTime startTime = (LocalDateTime) Objects.requireNonNull(
                data.get("startTime"), "Thiếu startTime");
        LocalDateTime endTime   = (LocalDateTime) Objects.requireNonNull(
                data.get("endTime"), "Thiếu endTime");

        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("endTime phải sau startTime");
        }
        if (startPrice < 0 || minIncrement <= 0) {
            throw new IllegalArgumentException(
                    "startPrice phải >= 0 và minIncrement phải > 0");
        }

        switch (type) {
            case VEHICLE: {
                String licensePlate = requireString(data, "licensePlate");
                String engineType   = requireString(data, "engineType");
                int mileage         = parseInt(data.get("mileage"));
                return new Vehicle(iid, sellerId, itemName, description,
                        startPrice, currentPrice, startTime, endTime, minIncrement,
                        licensePlate, engineType, mileage);
            }
            case ELECTRONICS: {
                int warrantyMonths = parseInt(data.get("warrantyMonths"));
                return new Electronics(iid, sellerId, itemName, description,
                        startPrice, currentPrice, startTime, endTime, minIncrement,
                        warrantyMonths);
            }
            case ART: {
                String artistName = requireString(data, "artistName");
                return new Art(iid, sellerId, itemName, description,
                        startPrice, currentPrice, startTime, endTime, minIncrement,
                        artistName);
            }
            default:
                throw new IllegalArgumentException("Lỗi logic nội bộ, không thể khởi tạo: " + type);
        }
    }

    // ===== Helpers =====

    private static String requireString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("Thiếu hoặc rỗng trường bắt buộc: " + key);
        }
        return value.toString();
    }

    private static long parseLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) return Long.parseLong((String) value);
        throw new IllegalArgumentException("Dữ liệu không phải định dạng số Long: " + value);
    }

    private static int parseInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) return Integer.parseInt((String) value);
        throw new IllegalArgumentException("Dữ liệu không phải định dạng số nguyên: " + value);
    }
}
