package Common.DataBase.entities;

import Common.Enum.AuctionState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Auction implements Serializable {
    private long id;
    private long item_id;
    private long current_user_id;
    private long current_price;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionState state;
}
