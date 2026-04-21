package Common.DataBase.entities;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class User {
    private long id;
    private String username;
    private long password;
    private UserStatus roll;
}
