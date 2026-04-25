package Common.DataBase.entities;

import Common.Enum.ItemStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor

public class VehicleItem extends Item {
    private String licensePlate;
    private String engineType;
    private int mileage;
    public VehicleItem(long id, long winner_id, long beginPrice, ItemStatus status, String licensePlate, String engineType, int mileage){
        super(id, winner_id, beginPrice, status);
        this.licensePlate = licensePlate;
        this.engineType = engineType;
        this.mileage = mileage;
    }
}
