package Common.DataBase.entities;

import Common.Enum.ItemStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Item {
    private long id;
    private long owner_user_id;
    private long beginPrice;
    private ItemStatus status;
}
