package Client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class UILogin extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/template/hellofx/main.fxml"));
        Scene scene1 = new Scene(loader.load());

        stage.setTitle("Đấu giá online 😎");
        stage.setScene(scene1);
        stage.show();
    }
}