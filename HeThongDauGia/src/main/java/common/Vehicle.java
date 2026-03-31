package common;

import java.time.LocalDateTime;

public class Vehicle extends Item{
    public Vehicle(String licensePlate, String IID, String itemName, String description, long startPrice, long currentPrice, LocalDateTime startTime, LocalDateTime endTime, long minIncrement) {
        super(IID, "VEHICLE", itemName, description, startPrice, currentPrice, startTime, endTime, minIncrement);
    }

    @Override
    public String getItemType() {
        return "VEHICLE";
    }
}
