package Server.Controller;

import Common.Enum.UserRole;
import Common.Model.user.UserAccount;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class DashBoardController {

    public void goToLogin(ActionEvent actionEvent) throws IOException {
        UserAccount.clearSession();
        switchScene(actionEvent, "/com/template/hellfx/UILogin.fxml");
    }

    public void Sandaugia(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/danhSachDauGia.fxml");
    }

    public void dangban(ActionEvent actionEvent) throws IOException {
        UserRole role = UserAccount.getCurrentRole();
        if (role == UserRole.BIDDER) {
            showWarning("Bidder chá»‰ Ä‘Æ°á»£c Ä‘áº¥u giÃ¡, khÃ´ng Ä‘Æ°á»£c Ä‘Äƒng bÃ¡n.");
            return;
        }
        switchScene(actionEvent, "/com/template/hellfx/SellerProducts.fxml");
    }

    public void quanlytk(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/account.fxml");
    }

    public void duyetsp(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/ApproveItem.fxml");
    }

    private void showWarning(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("ThÃ´ng bÃ¡o");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        URL resource = getClass().getResource(fxmlPath);
        if (resource == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lá»—i Ä‘iá»u hÆ°á»›ng");
            alert.setHeaderText("KhÃ´ng tÃ¬m tháº¥y mÃ n hÃ¬nh");
            alert.setContentText(fxmlPath);
            alert.showAndWait();
            return;
        }

        Parent root = FXMLLoader.load(resource);
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene scene = new Scene(root, UILogin.APP_WIDTH, UILogin.APP_HEIGHT);
        stage.setScene(scene);
        stage.show();
    }
}
