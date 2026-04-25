package Common.DataBase.entities;

import Common.Enum.ItemStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class ElectronicsItem extends Item{
    private int warrantyMonths;
    public ElectronicsItem(long id, long winner_id, long beginPrice, ItemStatus status, int warrantyMonths){
        super(id, winner_id, beginPrice, status);
        this.warrantyMonths = warrantyMonths;
    }
}

