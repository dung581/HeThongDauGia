package Common.Model.item;

import Common.Model.Item;

import java.time.LocalDateTime;

public class Vehicle extends Item {
    private String licensePlate;
    private String engineType;
    private int mileage;
    public Vehicle(String licensePlate, String IID, String itemName, String description, long startPrice, long currentPrice, LocalDateTime startTime, LocalDateTime endTime, long minIncrement, String engineType, int mileage) {
        super(IID, "VEHICLE", itemName, description, startPrice, currentPrice, startTime, endTime, minIncrement);
        this.engineType = engineType;
        this.mileage = mileage;
        this.licensePlate = licensePlate;
    }

    public String getEngineType() {
        return engineType;
    }

    public int getMileage() {
        return mileage;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    @Override
    public String getItemType() {
        return "VEHICLE";
    }
}
