package Common.DataBase.entities;

import Common.Enum.ItemStatus;
import Common.Enum.ItemType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Item {
    private long id;
    private String fullname;
    private long owner_user_id;
    private String description;
    private long beginPrice;
    private ItemType itemType;
    private long minIncrement;
    private ItemStatus status;

    public String getName() {
        return fullname;
    }
}
