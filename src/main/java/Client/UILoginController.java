package Client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

import static Client.Authservice.RqLogin;

public class UILoginController {
    private Parent root;
    private Scene scene;
    private Stage stage;
    private ActionEvent actionEvent;
    @FXML
    private TextField name;
    @FXML
    private TextField password;
    @FXML
    private Label welcomeText;

    // Hàm login xử lý đăng nhập
    public void login() throws IOException {
        String ten = name.getText();
        String pass = password.getText();
        boolean result = RqLogin(ten, pass);
        if (result) {
            // chuyển scene sang màn hình chính
        } else {
            // nhảy ra thông báo sai mật khẩu
        }
    }

    public void Register(ActionEvent actionEvent) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/com/template/hellofx/Scene2.fxml"));
        stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void dangki(ActionEvent actionEvent) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/com/template/hellofx/Main.fxml"));
        stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}
