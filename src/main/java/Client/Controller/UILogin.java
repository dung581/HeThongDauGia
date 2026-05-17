package Client.Controller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class UILogin extends Application {
    public static final double APP_WIDTH = 1220;
    public static final double APP_HEIGHT = 760;

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/template/hellfx/UILogin.fxml"));
        Scene scene1 = new Scene(loader.load(), APP_WIDTH, APP_HEIGHT);

        stage.setTitle("Dau gia online");
        stage.setScene(scene1);
        stage.setMinWidth(APP_WIDTH);
        stage.setMinHeight(APP_HEIGHT);
        stage.show();
    }
}
