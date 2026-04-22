package Common.Model.item;

import java.time.LocalDateTime;

public class Vehicle extends Item {
    private static final long serialVersionUID = 1L;

    private final String licensePlate;
    private final String engineType;
    private final int mileage;

    public Vehicle(String IID,
                   String sellerId,
                   String itemName,
                   String description,
                   long startPrice,
                   long currentPrice,
                   LocalDateTime startTime,
                   LocalDateTime endTime,
                   long minIncrement,
                   String licensePlate,
                   String engineType,
                   int mileage) {
        super(IID, sellerId, itemName, description, startPrice, currentPrice,
                startTime, endTime, minIncrement);
        this.licensePlate = licensePlate;
        this.engineType = engineType;
        this.mileage = mileage;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public String getEngineType() {
        return engineType;
    }

    public int getMileage() {
        return mileage;
    }

    @Override
    public String getItemType() {
        return "VEHICLE";
    }
}