package Client;

import Common.DataBase.entities.User;
import Common.Enum.UserRole;
import Common.Model.user.UserAccount;
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
    private final Authservice authService;

    @FXML
    private TextField name;

    @FXML
    private PasswordField loginPasswordHidden;
    @FXML
    private TextField loginPasswordVisible;

    @FXML
    private TextField regname;
    @FXML
    private PasswordField regpassHidden;
    @FXML
    private TextField regpassVisible;
    @FXML
    private PasswordField regpassagainHidden;
    @FXML
    private TextField regpassagainVisible;

    @FXML
    private RadioButton Seller;
    @FXML
    private RadioButton Bidder;
    @FXML
    private ToggleGroup group;
    @FXML
    private Label registerMessage;

    private boolean loginPasswordShown = false;
    private boolean registerPasswordShown = false;
    private boolean registerPasswordAgainShown = false;

    @FXML
    public void initialize() {
        if (Bidder != null) {
            Bidder.setUserData(UserRole.BIDDER);
        }
        if (Seller != null) {
            Seller.setUserData(UserRole.SELLER);
        }

        initPasswordBindings();
    }

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

    public UserRole getRole() {
        if (group == null) {
            return null;
        }

        Toggle selected = group.getSelectedToggle();
        if (selected != null) {
            return (UserRole) selected.getUserData();
        }
        return null;
    }

    public UILoginController() {
        authService = new Authservice();
    }

    public void toggleLoginPassword() {
        if (loginPasswordHidden == null || loginPasswordVisible == null) {
            return;
        }

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

    public void toggleRegisterPassword() {
        if (regpassHidden == null || regpassVisible == null) {
            return;
        }

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

    public void toggleRegisterPasswordAgain() {
        if (regpassagainHidden == null || regpassagainVisible == null) {
            return;
        }

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

    private String getLoginPasswordValue() {
        if (loginPasswordShown && loginPasswordVisible != null) {
            return loginPasswordVisible.getText();
        }
        return loginPasswordHidden == null ? "" : loginPasswordHidden.getText();
    }

    private String getRegisterPasswordValue() {
        if (registerPasswordShown && regpassVisible != null) {
            return regpassVisible.getText();
        }
        return regpassHidden == null ? "" : regpassHidden.getText();
    }

    private String getRegisterPasswordAgainValue() {
        if (registerPasswordAgainShown && regpassagainVisible != null) {
            return regpassagainVisible.getText();
        }
        return regpassagainHidden == null ? "" : regpassagainHidden.getText();
    }

    public void login() throws UsernameIsBlankException, UserNotFoundException, WrongPasswordException, PasswordIsBlankException {
        String ten = name.getText();
        String pass = getLoginPasswordValue();

        try {
            User user = authService.login(ten, pass);
            UserAccount.setSession(user.getId(), user.getUsername(), user.getFullname(), user.getRole());
            JOptionPane.showMessageDialog(null, "Dang nhap thanh cong: " + user.getUsername(), "Thong bao", JOptionPane.INFORMATION_MESSAGE);

            UserRole role = user.getRole();
            if (role == UserRole.BIDDER) {
                switchScene("/com/template/hellfx/dashboard-Bidder.fxml");
            } else if (role == UserRole.SELLER) {
                switchScene("/com/template/hellfx/dashboard - Seller.fxml");
            } else if (role == UserRole.ADMIN) {
                switchScene("/com/template/hellfx/dashboard - Admin.fxml");
            } else {
                JOptionPane.showMessageDialog(null, "Role khong hop le.", "Thong bao", JOptionPane.ERROR_MESSAGE);
            }

        } catch (UsernameIsBlankException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Thong bao", JOptionPane.WARNING_MESSAGE);
        } catch (UserNotFoundException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Thong bao", JOptionPane.ERROR_MESSAGE);
        } catch (WrongPasswordException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Thong bao", JOptionPane.ERROR_MESSAGE);
        } catch (DataAccessException e) {
            JOptionPane.showMessageDialog(null, "Khong the ket noi database. Kiem tra cau hinh ket noi.", "Thong bao", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Khong mo duoc man hinh dau gia.", "Thong bao", JOptionPane.ERROR_MESSAGE);
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
        String mkhau = getRegisterPasswordValue();
        String mkhauLai = getRegisterPasswordAgainValue();
        UserRole role = getRole();

        clearRegisterMessage();

        if (!mkhau.equals(mkhauLai)) {
            JOptionPane.showMessageDialog(null, "Dang ky that bai: mat khau nhap lai khong khop", "Thong bao", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (role == null) {
            JOptionPane.showMessageDialog(null, "Vui long chon role Bidder hoac Seller.", "Thong bao", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (role == UserRole.ADMIN) {
            JOptionPane.showMessageDialog(null, "Khong the dang ky tai khoan Admin.", "Thong bao", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            authService.register(tenDK, mkhau, role);
            showRegisterMessage("Dang ky thanh cong");

            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
            pause.setOnFinished(event -> {
                try {
                    switchScene(actionEvent, "/com/template/hellfx/UILogin.fxml");
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "Khong mo duoc man hinh dang nhap.", "Thong bao", JOptionPane.ERROR_MESSAGE);
                }
            });
            pause.play();
        } catch (UsernameIsBlankException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Thong bao", JOptionPane.WARNING_MESSAGE);
        } catch (UsernameAlreadyExistsException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Thong bao", JOptionPane.ERROR_MESSAGE);
        } catch (PasswordIsBlankException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Thong bao", JOptionPane.WARNING_MESSAGE);
        } catch (DataAccessException e) {
            JOptionPane.showMessageDialog(null, "Dang ky that bai do loi ket noi may chu.", "Thong bao", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        root = FXMLLoader.load(getClass().getResource(fxmlPath));
        stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    private void switchScene(String fxmlPath) throws IOException {
        root = FXMLLoader.load(getClass().getResource(fxmlPath));
        stage = (Stage) name.getScene().getWindow();
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
