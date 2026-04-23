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

import javax.swing.*;
import java.io.IOException;




public class UILoginController {
    private Parent root;
    private Scene scene;
    private Stage stage;
    private ActionEvent actionEvent;
    private Sendservice Send;
    @FXML
    private TextField name;
    @FXML
    private TextField password;
    @FXML
    private TextField regname;
    @FXML
    private TextField regpass;
    @FXML
    private TextField regpassagain;
    @FXML
    private Label welcomeText;

    public UILoginController(){
        Send = new Sendservice();
    }

    // Hàm login xử lý đăng nhập
    public void login() throws IOException {
        String ten = name.getText();
        String pass = password.getText();
        //to do : chuyen sang man hinh menu neu dung tk mk ko thi hien thong bao sai mk
    }

    public void Register(ActionEvent actionEvent) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/com/template/hellfx/UIRegister.fxml"));
        stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void Register2(ActionEvent actionEvent) throws IOException {
        String TenDK = regname.getText();
        String Mkhau = regpass.getText();
        String Mkhaulai = regpassagain.getText();

        if(Mkhau.equals(Mkhaulai)){
            // thong bao dang ki thanh cong
            JOptionPane.showMessageDialog(null, "Đăng ký thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);

            // tro ve man hinh login
            root = FXMLLoader.load(getClass().getResource("/com/template/hellfx/UILogin.fxml"));
            stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        }else {JOptionPane.showMessageDialog(null, "Đăng ký thất bại!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);}

    }
}
