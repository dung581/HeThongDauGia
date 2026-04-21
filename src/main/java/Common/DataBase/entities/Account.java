package Common.DataBase.entities;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Account {
    private long id;
    private long user_id;
    private long balance;
    private long locked_balance;
}
