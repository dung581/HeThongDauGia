package Client.Controller;

import Server.service.AutoBidBackgroundRunner;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class UILogin extends Application {
    public static final double APP_WIDTH = 1220;
    public static final double APP_HEIGHT = 760;

    // Khởi động ứng dụng JavaFX, load màn đăng nhập đầu tiên và cấu hình kích thước cửa sổ.
    @Override
    public void start(Stage stage) throws IOException {
        AutoBidBackgroundRunner.start();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/template/hellfx/UILogin.fxml"));
        Scene scene1 = new Scene(loader.load(), APP_WIDTH, APP_HEIGHT);

        stage.setTitle("Dau gia online");
        stage.setScene(scene1);
        stage.setMinWidth(APP_WIDTH);
        stage.setMinHeight(APP_HEIGHT);
        stage.show();
    }

    // Khi tắt app thì dừng runner nền để không giữ kết nối DB/luồng thừa.
    @Override
    public void stop() {
        AutoBidBackgroundRunner.stop();
    }
}
