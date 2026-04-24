package Common.DataBase.entities;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
@Setter
@Getter
public class Auction implements Serializable {
    private long id;
    private long item_id;
    private long current_user_id;
    private long current_price;
    private LocalDateTime availability_time;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String state;
    private long sellerId;
}
