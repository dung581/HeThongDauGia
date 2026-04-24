package Client;
import Common.DataBase.entities.User;
import Common.DataBase.repository.UserRepository;

import Common.Enum.UserRole;

// vai tro gui thong tin de xac thuc

public class Authservice {
    UserRepository userRepository;

    public Authservice() {
        this.userRepository = new UserRepository();
    }


//    // Hàm đăng nhập: ví dụ kiểm tra dữ liệu
//    public boolean login(String username, String password) {
//
//    }
}
