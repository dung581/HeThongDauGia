package Common.DataBase.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class bid implements Serializable {
    private long id;
    private long auction_id;
    private long user_id;
    private long item_id;
    private long price;
}
