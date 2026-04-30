package Client.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class NavigationUtil {

    // Lưu trữ cửa sổ chính của toàn bộ ứng dụng
    private static Stage mainStage;

    /**
     * Hàm này phải được gọi ở hàm start() trong file Main/Launcher của ứng dụng
     * Ví dụ: NavigationUtil.setMainStage(primaryStage);
     */
    public static void setMainStage(Stage stage) {
        mainStage = stage;
    }

    /**
     * Chuyển màn hình chỉ bằng tên file FXML (Chuẩn theo Diagram 01, 02, 05...)
     */
    public static void switchScene(String fxmlName) {
        if (mainStage == null) {
            AlertUtil.showError("Lỗi hệ thống: Chưa khởi tạo Main Stage!");
            return;
        }

        try {
            // Nạp file giao diện. Đảm bảo đường dẫn thư mục resources của bạn là đúng
            FXMLLoader loader = new FXMLLoader(NavigationUtil.class.getResource("/" + fxmlName));
            Parent root = loader.load();
            Scene scene = new Scene(root);

            mainStage.setScene(scene);
            mainStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Không thể tải màn hình: " + fxmlName);
        }
    }
}