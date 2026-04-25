package Common.DataBase.entities;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Autobid {
    private long id;
    private long auction_id;
    private long user_id;
    private long item_id;
    private long max_price;
    private boolean is_active;
}