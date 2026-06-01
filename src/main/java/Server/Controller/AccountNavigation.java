package Server.Controller;

import Client.Controller.UILogin;
import Client.util.AlertUtil;
import Common.Enum.UserRole;
import Common.Model.user.UserAccount;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

// Nhóm điều hướng của màn Account.
final class AccountNavigation {
    private final AccountController controller;

    AccountNavigation(AccountController controller) {
        this.controller = controller;
    }

    // Quay lại dashboard đúng với role hiện tại.
    void backToDashboard(ActionEvent actionEvent) throws IOException {
        UserRole role = UserAccount.getCurrentRole();
        String target;
        if (role == UserRole.ADMIN) {
            target = "/com/template/hellfx/dashboard - Admin.fxml";
        } else if (role == UserRole.SELLER) {
            target = "/com/template/hellfx/dashboard - Seller.fxml";
        } else {
            target = "/com/template/hellfx/dashboard-Bidder.fxml";
        }
        switchScene(actionEvent, target);
    }

    // Điều hướng sang màn duyệt/quản lý item, chỉ cho Admin.
    void goToBrowseItems(ActionEvent actionEvent) throws IOException {
        if (UserAccount.getCurrentRole() != UserRole.ADMIN) {
            AlertUtil.showError("Chuc nang nay chi danh cho Admin.");
            return;
        }
        switchScene(actionEvent, "/com/template/hellfx/ItemBrowse.fxml");
    }

    // Điều hướng sang màn nạp tiền, chỉ cho Bidder.
    void goToDeposit(ActionEvent actionEvent) throws IOException {
        if (UserAccount.getCurrentRole() != UserRole.BIDDER) {
            AlertUtil.showError("Chuc nang nay chi danh cho nguoi dau gia.");
            return;
        }
        switchScene(actionEvent, "/com/template/hellfx/Deposit.fxml");
    }

    // Điều hướng sang màn sản phẩm của Seller, chỉ cho Seller.
    void goToUploadItem(ActionEvent actionEvent) throws IOException {
        if (UserAccount.getCurrentRole() != UserRole.SELLER) {
            AlertUtil.showError("Chuc nang nay chi danh cho nguoi ban.");
            return;
        }
        switchScene(actionEvent, "/com/template/hellfx/SellerProducts.fxml");
    }

    // Thay root scene hiện tại bằng FXML mới và giữ kích thước chuẩn của ứng dụng.
    void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(controller.getClass().getResource(fxmlPath));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene currentScene = stage.getScene();
        if (currentScene == null) {
            stage.setScene(new Scene(root, UILogin.APP_WIDTH, UILogin.APP_HEIGHT));
        } else {
            currentScene.setRoot(root);
        }
        stage.show();
    }
}
