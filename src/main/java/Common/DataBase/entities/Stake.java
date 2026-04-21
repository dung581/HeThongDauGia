package Common.DataBase.entities;

import Common.Enum.ItemStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Stake {
    private long id;
    private  long aution_id;
    private long locked_item_id;
    private long user_id;
    private long amount;
    private ItemStatus status;
}
