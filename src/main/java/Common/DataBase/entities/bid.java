package Common.DataBase.entities;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class bid implements Serializable {
    private long id;
    private long auction_id;
    private long user_id;
    private long item_id;
    private long price;
}
