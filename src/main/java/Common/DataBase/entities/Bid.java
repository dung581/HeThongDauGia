package Common.DataBase.entities;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
public class Bid implements Serializable {
    private long id;
    private long auction_id;
    private long user_id;
    private long item_id;
    private long price;
    private LocalDateTime created_at;
}
