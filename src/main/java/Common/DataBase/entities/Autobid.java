package Common.DataBase.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Autobid {
    private long id;
    private long auction_id;
    private long user_id;
    private long item_id;
    private long max_price;
    private boolean is_active;
}