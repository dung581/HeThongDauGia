package Common.DataBase.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Account {
    private long id;
    private long user_id;
    private long balance;
    private long locked_balance;
}
