package Common.DataBase.entities;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Setter
@Getter
public class session {
    private long id;
    private long item_id;
    private long current_user_id;
    private long current_price;
    private LocalDateTime availability_time;
}
