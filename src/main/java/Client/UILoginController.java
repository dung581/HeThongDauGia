package Client;

import Common.DataBase.entities.User;
import Server.service.Exceptions.*;
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
    private Stage stage;
    private Authservice authService;

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

    public UILoginController() {
        authService = new Authservice();
    }

    // Ham login xu ly dang nhap
    public void login() throws UsernameIsBlankException, UserNotFoundException, WrongPasswordException, PasswordIsBlankException {
        String ten = name.getText();
        String pass = password.getText();

        try {
            User user = authService.login(ten, pass);
            JOptionPane.showMessageDialog(null, "Dang nhap thanh cong: " + user.getUsername(), "Thong bao", JOptionPane.INFORMATION_MESSAGE);
            // TODO: chuyen sang man hinh menu sau khi dang nhap thanh cong
        } catch (UsernameIsBlankException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Thong bao", JOptionPane.WARNING_MESSAGE);
        } catch (UserNotFoundException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Thong bao", JOptionPane.ERROR_MESSAGE);
        } catch (WrongPasswordException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Thong bao", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void Register(ActionEvent actionEvent) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/com/template/hellfx/UIRegister.fxml"));
        stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void Register2(ActionEvent actionEvent) throws IOException,UsernameIsBlankException, UsernameAlreadyExistsException, PasswordIsBlankException {
        String tenDK = regname.getText();
        String mkhau = regpass.getText();
        String mkhauLai = regpassagain.getText();

        if (!mkhau.equals(mkhauLai)) {
            JOptionPane.showMessageDialog(null, "Dang ky that bai: mat khau nhap lai khong khop", "Thong bao", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            authService.register(tenDK, mkhau);
            JOptionPane.showMessageDialog(null, "Dang ky thanh cong", "Thong bao", JOptionPane.INFORMATION_MESSAGE);

            // tro ve man hinh login
            root = FXMLLoader.load(getClass().getResource("/com/template/hellfx/UILogin.fxml"));
            stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (UsernameIsBlankException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Thong bao", JOptionPane.WARNING_MESSAGE);
        } catch (UsernameAlreadyExistsException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Thong bao", JOptionPane.ERROR_MESSAGE);
        }
    }
}
