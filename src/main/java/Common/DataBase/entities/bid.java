package Common.DataBase.entities;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class bid {
    private long id;
    private long aution_id;
    private long user_id;
    private long item_id;
    private long price;
}
