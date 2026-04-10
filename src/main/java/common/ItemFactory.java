package common;

import java.time.LocalDateTime;
import java.util.Map;

public class ItemFactory {
    private ItemFactory(){
        throw new UnsupportedOperationException("Day la lop factory, khong khoi tao object!");
    }
    public static Item createItem(String typeString, Map<String, Object> data){

        ItemType type = ItemType.fromString(typeString); // chuyen enum thanh String an toan
        // trich xuat cac thuoc tinh chung
        String iid = (String) data.get("IID");
        String itemName = (String) data.get("itemName");
        String description = (String) data.get("description");

        // su dung ham helper de ep kieu so an toan
        long startPrice = parseLong(data.get("startPrice"));
        long currentPrice = data.containsKey("currentPrice") ? parseLong(data.get("currentPrice")) : startPrice;
        long minIncrement = parseLong(data.get("minIncrement"));

        // ep kieu thoi gian
        LocalDateTime startTime = (LocalDateTime) data.get("startTime");
        LocalDateTime endTime = (LocalDateTime) data.get("endTime");

        // phan nhanh khoi tao cac doi tuong cu the
        switch (type){
            case VEHICLE:
                // trich xuat thuoc tinh rieng cua VEHICLE
                String licensePlate = (String) data.get("licensePlate");
                String engineType = (String) data.get("engineType");
                int mileage = parseInt(data.get("mileage"));
                return new Vehicle(licensePlate, iid, itemName, description, startPrice, currentPrice, startTime, endTime, minIncrement, engineType, mileage);

            case ELECTRONICS:
                // trich xuat thuoc tinh rieng cua ELECTRONICS
                int warrantyMonths = parseInt(data.get("warrantyMonths"));
                return new Electronics(iid, itemName, description, startPrice, currentPrice, startTime, endTime, minIncrement, warrantyMonths);



            case ART:
                // trich xuat thuoc tinh rieng cua ART
                String artistName = (String) data.get("artistName");
                return new Art(iid, itemName, description, startPrice, currentPrice, startTime, endTime, minIncrement, artistName);

            default:
                throw new IllegalArgumentException("Loi logic noi bo, khong the khoi tao: "+type);

        }
    }
    private static long parseLong(Object value){
        if(value == null){
            return 0L;
        }
        if(value instanceof Number){
            return ((Number) value).longValue();
        }
        if (value instanceof String){
            return Long.parseLong((String) value);
        }
        throw new IllegalArgumentException("Du lieu khong phai dinh dang so Long: "+value);
    }
    private static int parseInt(Object value){
        if(value == null){
            return 0;
        }
        if (value instanceof Number){
            return ((Number) value).intValue();
        }
        if (value instanceof String){
            return Integer.parseInt((String) value);
        }
        throw new IllegalArgumentException("Du lieu nay khong phai dinh dang so nguyen: "+value);
    }
}
