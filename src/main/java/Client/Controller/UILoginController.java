package Client.Controller;

import Client.Controller.UILogin;
import Common.DataBase.entities.User;
import Common.Enum.UserRole;
import Common.Model.user.UserAccount;
import Server.service.AuthService;
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
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.swing.JOptionPane;
import java.io.IOException;

public class UILoginController {
    private Parent root;
    private Stage stage;
    private final AuthService authService;

    @FXML private TextField name;

    @FXML private PasswordField loginPasswordHidden;
    @FXML private TextField loginPasswordVisible;

    @FXML private TextField regname;
    @FXML private TextField regfullname;
    @FXML private PasswordField regpassHidden;
    @FXML private TextField regpassVisible;
    @FXML private PasswordField regpassagainHidden;
    @FXML private TextField regpassagainVisible;

    @FXML private RadioButton Seller;
    @FXML private RadioButton Bidder;
    @FXML private ToggleGroup group;
    @FXML private Label registerMessage;

    private boolean loginPasswordShown = false;
    private boolean registerPasswordShown = false;
    private boolean registerPasswordAgainShown = false;

    // JavaFX tự gọi sau khi load FXML: gán role cho radio button và ẩn các ô mật khẩu dạng text.
    @FXML
    public void initialize() {
        if (Bidder != null) Bidder.setUserData(UserRole.BIDDER);
        if (Seller != null) Seller.setUserData(UserRole.SELLER);
        initPasswordBindings();
    }

    // Cấu hình các TextField hiện mật khẩu ban đầu ở trạng thái ẩn.
    private void initPasswordBindings() {
        if (loginPasswordHidden != null && loginPasswordVisible != null) {
            loginPasswordVisible.setManaged(false);
            loginPasswordVisible.setVisible(false);
        }
        if (regpassHidden != null && regpassVisible != null) {
            regpassVisible.setManaged(false);
            regpassVisible.setVisible(false);
        }
        if (regpassagainHidden != null && regpassagainVisible != null) {
            regpassagainVisible.setManaged(false);
            regpassagainVisible.setVisible(false);
        }
    }

    // Lấy role người dùng đã chọn khi đăng ký tài khoản.
    public UserRole getRole() {
        if (group == null) return null;
        Toggle selected = group.getSelectedToggle();
        return selected == null ? null : (UserRole) selected.getUserData();
    }

    // Tạo controller và khởi tạo AuthService dùng cho đăng nhập/đăng ký.
    public UILoginController() {
        authService = new AuthService();
    }

    // Bật/tắt hiển thị mật khẩu ở màn đăng nhập.
    @FXML
    public void toggleLoginPassword() {
        if (loginPasswordHidden == null || loginPasswordVisible == null) return;
        if (!loginPasswordShown) {
            loginPasswordVisible.setText(loginPasswordHidden.getText());
            loginPasswordVisible.setManaged(true);
            loginPasswordVisible.setVisible(true);
            loginPasswordHidden.setManaged(false);
            loginPasswordHidden.setVisible(false);
        } else {
            loginPasswordHidden.setText(loginPasswordVisible.getText());
            loginPasswordHidden.setManaged(true);
            loginPasswordHidden.setVisible(true);
            loginPasswordVisible.setManaged(false);
            loginPasswordVisible.setVisible(false);
        }
        loginPasswordShown = !loginPasswordShown;
    }

    // Bật/tắt hiển thị mật khẩu chính ở màn đăng ký.
    @FXML
    public void toggleRegisterPassword() {
        if (regpassHidden == null || regpassVisible == null) return;
        if (!registerPasswordShown) {
            regpassVisible.setText(regpassHidden.getText());
            regpassVisible.setManaged(true);
            regpassVisible.setVisible(true);
            regpassHidden.setManaged(false);
            regpassHidden.setVisible(false);
        } else {
            regpassHidden.setText(regpassVisible.getText());
            regpassHidden.setManaged(true);
            regpassHidden.setVisible(true);
            regpassVisible.setManaged(false);
            regpassVisible.setVisible(false);
        }
        registerPasswordShown = !registerPasswordShown;
    }

    // Bật/tắt hiển thị mật khẩu nhập lại ở màn đăng ký.
    @FXML
    public void toggleRegisterPasswordAgain() {
        if (regpassagainHidden == null || regpassagainVisible == null) return;
        if (!registerPasswordAgainShown) {
            regpassagainVisible.setText(regpassagainHidden.getText());
            regpassagainVisible.setManaged(true);
            regpassagainVisible.setVisible(true);
            regpassagainHidden.setManaged(false);
            regpassagainHidden.setVisible(false);
        } else {
            regpassagainHidden.setText(regpassagainVisible.getText());
            regpassagainHidden.setManaged(true);
            regpassagainHidden.setVisible(true);
            regpassagainVisible.setManaged(false);
            regpassagainVisible.setVisible(false);
        }
        registerPasswordAgainShown = !registerPasswordAgainShown;
    }

    // Đọc giá trị mật khẩu đăng nhập từ field đang hiển thị.
    private String getLoginPasswordValue() {
        return (loginPasswordShown && loginPasswordVisible != null)
                ? loginPasswordVisible.getText()
                : (loginPasswordHidden == null ? "" : loginPasswordHidden.getText());
    }

    // Đọc giá trị mật khẩu đăng ký từ field đang hiển thị.
    private String getRegisterPasswordValue() {
        return (registerPasswordShown && regpassVisible != null)
                ? regpassVisible.getText()
                : (regpassHidden == null ? "" : regpassHidden.getText());
    }

    // Đọc giá trị mật khẩu nhập lại từ field đang hiển thị.
    private String getRegisterPasswordAgainValue() {
        return (registerPasswordAgainShown && regpassagainVisible != null)
                ? regpassagainVisible.getText()
                : (regpassagainHidden == null ? "" : regpassagainHidden.getText());
    }

    // Xử lý đăng nhập: gọi AuthService, lưu session user và chuyển sang dashboard theo role.
    @FXML
    public void login() throws UsernameIsBlankException, UserNotFoundException, WrongPasswordException, PasswordIsBlankException {
        String ten = name.getText();
        String pass = getLoginPasswordValue();

        try {
            User user = authService.login(ten, pass);
            UserAccount.setSession(user.getId(), user.getUsername(), user.getFullname(), user.getRole());

            UserRole role = user.getRole();
            if (role == UserRole.BIDDER) {
                switchScene("/com/template/hellfx/dashboard-Bidder.fxml");
            } else if (role == UserRole.SELLER) {
                switchScene("/com/template/hellfx/dashboard - Seller.fxml");
            } else if (role == UserRole.ADMIN) {
                switchScene("/com/template/hellfx/dashboard - Admin.fxml");
            } else {
                JOptionPane.showMessageDialog(null, "Vai trò không hợp lệ.", "Thông báo", JOptionPane.ERROR_MESSAGE);
            }

        } catch (UsernameIsBlankException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Thông báo", JOptionPane.WARNING_MESSAGE);
        } catch (UserNotFoundException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Thông báo", JOptionPane.ERROR_MESSAGE);
        } catch (WrongPasswordException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Thông báo", JOptionPane.ERROR_MESSAGE);
        } catch (DataAccessException e) {
            JOptionPane.showMessageDialog(null, "Không thể kết nối database hoặc cấu hình DB chưa đúng.", "Thông báo", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Không mở được màn hình sau đăng nhập.\nChi tiết: " + e.getMessage(), "Thông báo", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Chuyển từ màn đăng nhập sang màn đăng ký.
    @FXML
    public void Register(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/UIRegister.fxml");
    }

    // Chuyển từ màn đăng ký về màn đăng nhập.
    @FXML
    public void goToLogin(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/UILogin.fxml");
    }

    // Xử lý đăng ký: validate mật khẩu/role, gọi AuthService và tự động quay về đăng nhập khi thành công.
    @FXML
    public void Register2(ActionEvent actionEvent) throws IOException, UsernameIsBlankException, UsernameAlreadyExistsException, PasswordIsBlankException {
        String tenDK = regname.getText();
        String hten = (regfullname != null) ? regfullname.getText() : "";
        String mkhau = getRegisterPasswordValue();
        String mkhauLai = getRegisterPasswordAgainValue();
        UserRole role = getRole();

        clearRegisterMessage();

        if (!mkhau.equals(mkhauLai)) {
            JOptionPane.showMessageDialog(null, "Đăng ký thất bại: mật khẩu nhập lại không khớp", "Thông báo", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (role == null) {
            JOptionPane.showMessageDialog(null, "Vui lòng chọn vai trò Người đấu giá hoặc Người bán.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (role == UserRole.ADMIN) {
            JOptionPane.showMessageDialog(null, "Không thể đăng ký tài khoản Admin.", "Thông báo", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            authService.register(tenDK, mkhau, hten, role);
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
            JOptionPane.showMessageDialog(null, "Đăng ký thất bại do không kết nối được database hoặc cấu hình DB chưa đúng.", "Thông báo", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Đổi màn theo ActionEvent của nút được bấm.
    private void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        root = FXMLLoader.load(getClass().getResource(fxmlPath));
        stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        replaceSceneRoot(stage, root);
    }

    // Đổi màn khi không có ActionEvent, dùng scene hiện tại của ô username.
    private void switchScene(String fxmlPath) throws IOException {
        root = FXMLLoader.load(getClass().getResource(fxmlPath));
        stage = (Stage) name.getScene().getWindow();
        replaceSceneRoot(stage, root);
    }

    // Thay root của scene hiện tại để giữ nguyên kích thước cửa sổ ứng dụng.
    private void replaceSceneRoot(Stage stage, Parent root) {
        Scene currentScene = stage.getScene();
        if (currentScene == null) {
            stage.setScene(new Scene(root, UILogin.APP_WIDTH, UILogin.APP_HEIGHT));
        } else {
            currentScene.setRoot(root);
        }
        stage.show();
    }

    // Hiện message đăng ký trên UI thay vì bật popup.
    private void showRegisterMessage(String message) {
        if (registerMessage != null) {
            registerMessage.setText(message);
            registerMessage.setVisible(true);
        }
    }

    // Xóa message đăng ký cũ trước khi validate/submit lần mới.
    private void clearRegisterMessage() {
        if (registerMessage != null) {
            registerMessage.setText("");
            registerMessage.setVisible(false);
        }
    }
}
