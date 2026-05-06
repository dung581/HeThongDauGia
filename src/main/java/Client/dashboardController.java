package Client;

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

public class dashboardController {

    public void goToLogin(ActionEvent actionEvent) throws IOException {
        UserAccount.clearSession();
        switchScene(actionEvent, "/com/template/hellfx/UILogin.fxml");
    }

    public void Sandaugia(ActionEvent actionEvent) throws IOException {
        UserRole role = UserAccount.getCurrentRole();
        if (role == UserRole.SELLER) {
            showWarning("Seller chỉ được bán, không được đấu giá mua.");
            return;
        }
        switchScene(actionEvent, "/com/template/hellfx/danhSachDauGia.fxml");
    }

    public void dangban(ActionEvent actionEvent) throws IOException {
        UserRole role = UserAccount.getCurrentRole();
        if (role == UserRole.BIDDER) {
            showWarning("Bidder chỉ được đấu giá, không được đăng bán.");
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
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        URL resource = getClass().getResource(fxmlPath);
        if (resource == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi điều hướng");
            alert.setHeaderText("Không tìm thấy màn hình");
            alert.setContentText(fxmlPath);
            alert.showAndWait();
            return;
        }

        Parent root = FXMLLoader.load(resource);
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}
