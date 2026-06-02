package Server;

import Common.DataBase.entities.User;
import Common.Enum.UserRole;
import Server.service.AuthService;

public class CreateAdmin {
    public static void main(String[] args) {
        System.out.println("[INFO] Bắt đầu tạo tài khoản Admin 'ad2'...");
        AuthService authService = new AuthService();
        try {
            User user = authService.register("ad2", "123456", "Administrator 2", UserRole.ADMIN);
            System.out.println("[SUCCESS] Đã tạo thành công tài khoản Admin!");
            System.out.println("ID: " + user.getId());
            System.out.println("Username: " + user.getUsername());
            System.out.println("Fullname: " + user.getFullname());
            System.out.println("Role: " + user.getRole());
        } catch (Exception e) {
            System.err.println("[ERROR] Tạo tài khoản Admin thất bại: " + e.getMessage());
            e.printStackTrace();
        }
        System.exit(0);
    }
}
