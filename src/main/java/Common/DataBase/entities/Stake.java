package Common.DataBase.entities;

import Common.Enum.StakeStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Stake {
    private long id;
    private  long aution_id;
    private long locked_item_id;
    private long user_id;
    private long amount;
    private StakeStatus status;
}
