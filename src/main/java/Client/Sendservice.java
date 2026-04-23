package Client;

import Common.DataBase.entities.User;
import Common.DataBase.repository.UserRepository;
import Common.Enum.UserStatus;

public class Sendservice {
    UserRepository userRepository;
    public Sendservice() {
        this.userRepository = new UserRepository();
    }
    // Hàm đăng ký: nhận dữ liệu từ Controller và gửi xuống Repository
    public void InfoRegister(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(UserStatus.BIDDER);
        userRepository.saveUser(user);
    }
}