import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ClientApp1 extends Application {
    Stage window;
    Scene scene1, scene2;
    @Override
    public void start(Stage stage) {
        window = stage;
        //scene 1
        Label label1 = new Label(" đấu giá đâu giá đấu giá dê");
        Button nut1 = new Button("dam con meo");
        nut1.setOnAction(envent -> {
            window.setScene(scene2);
        });
        VBox layout1 = new VBox();
        layout1.getChildren().addAll(label1,nut1);
        scene1 = new Scene(layout1, 300 , 350);

        //scene 2
        Label label2 = new Label("co j o day");
        Button nut2 = new Button("xoa dau con meo");
        nut2.setOnAction(envent -> {
            window.setScene(scene1);
        });
        VBox layout2  = new VBox();
        layout2.getChildren().addAll(label2, nut2);
        scene2 = new Scene(layout2, 300 , 350);

        window.setTitle("dau gia truc tip");
        window.setScene(scene1);
        window.show();
    }
    public static void main() {
        launch();
    }
}