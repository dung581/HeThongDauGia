package Client;

import Common.DataBase.entities.User;
import Server.service.Exceptions.DataAccessException;
import Server.service.Exceptions.PasswordIsBlankException;
import Server.service.Exceptions.UserNotFoundException;
import Server.service.Exceptions.UsernameAlreadyExistsException;
import Server.service.Exceptions.UsernameIsBlankException;
import Server.service.Exceptions.WrongPasswordException;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.swing.*;
import java.io.IOException;
import java.util.Objects;

public class UILoginController {
    private Parent root;
    private Stage stage;
    private final Authservice authService;

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
    @FXML
    private Label registerMessage;

    public UILoginController() {
        authService = new Authservice();
    }

    public void login() throws UsernameIsBlankException, UserNotFoundException, WrongPasswordException, PasswordIsBlankException {
        String ten = name.getText();
        String pass = password.getText();

        try {
            User user = authService.login(ten, pass);
            JOptionPane.showMessageDialog(null, "Đăng nhập thành công: " + user.getUsername(), "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        } catch (UsernameIsBlankException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Thông báo", JOptionPane.WARNING_MESSAGE);
        } catch (UserNotFoundException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Thông báo", JOptionPane.ERROR_MESSAGE);
        } catch (WrongPasswordException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Thông báo", JOptionPane.ERROR_MESSAGE);
        } catch (DataAccessException e) {
            JOptionPane.showMessageDialog(null, "Không thể kết nối database. Kiểm tra SQL Server và db.properties.", "Thông báo", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void Register(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/UIRegister.fxml");
    }

    public void goToLogin(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/UILogin.fxml");
    }

    public void Register2(ActionEvent actionEvent) throws IOException, UsernameIsBlankException, UsernameAlreadyExistsException, PasswordIsBlankException {
        String tenDK = regname.getText();
        String mkhau = regpass.getText();
        String mkhauLai = regpassagain.getText();

        clearRegisterMessage();

        if (!mkhau.equals(mkhauLai)) {
            JOptionPane.showMessageDialog(null, "Đăng ký thất bại: mật khẩu nhập lại không khớp", "Thông báo", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            authService.register(tenDK, mkhau);
            showRegisterMessage("Đăng ký thành công");

            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
            pause.setOnFinished(event -> {
                try {
                    switchScene(actionEvent, "/com/template/hellfx/UILogin.fxml");
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "Không mở được màn hình đăng nhập.", "Thông báo", JOptionPane.ERROR_MESSAGE);
                }
            });
            pause.play();
        } catch (UsernameIsBlankException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Thông báo", JOptionPane.WARNING_MESSAGE);
        } catch (UsernameAlreadyExistsException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Thông báo", JOptionPane.ERROR_MESSAGE);
        } catch (PasswordIsBlankException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Thông báo", JOptionPane.WARNING_MESSAGE);
        } catch (DataAccessException e) {
            JOptionPane.showMessageDialog(null, "Đăng ký thất bại do lỗi kết nối máy chủ.", "Thông báo", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        root = FXMLLoader.load(getClass().getResource(fxmlPath));
        stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    private void showRegisterMessage(String message) {
        if (registerMessage != null) {
            registerMessage.setText(message);
            registerMessage.setVisible(true);
        }
    }

    private void clearRegisterMessage() {
        if (registerMessage != null) {
            registerMessage.setText("");
            registerMessage.setVisible(false);
        }
    }

}
