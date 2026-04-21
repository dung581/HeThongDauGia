package Common.DataBase.entities;

import Common.Enum.ItemStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Item {
    private long id;
    private long winner_id;
    private long beginPrice;
    private ItemStatus status;
}
