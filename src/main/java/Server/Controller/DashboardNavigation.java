package Server.Controller;

import Client.Controller.UILogin;
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

// Nhóm điều hướng của dashboard: đổi màn, mở chi tiết phiên/kết quả và hiển thị cảnh báo.
final class DashboardNavigation {
    private final DashBoardController controller;

    DashboardNavigation(DashBoardController controller) {
        this.controller = controller;
    }

    // Điều hướng về dashboard đúng với role hiện tại.
    void goHome(ActionEvent actionEvent) throws IOException {
        UserRole role = UserAccount.getCurrentRole();
        if (role == UserRole.ADMIN) {
            switchScene(actionEvent, "/com/template/hellfx/dashboard - Admin.fxml");
        } else if (role == UserRole.SELLER) {
            switchScene(actionEvent, "/com/template/hellfx/dashboard - Seller.fxml");
        } else {
            switchScene(actionEvent, "/com/template/hellfx/dashboard-Bidder.fxml");
        }
    }

    // Đăng xuất: xóa session user và quay về màn đăng nhập.
    void goToLogin(ActionEvent actionEvent) throws IOException {
        UserAccount.clearSession();
        switchScene(actionEvent, "/com/template/hellfx/UILogin.fxml");
    }

    // Mở màn sản phẩm của Seller; chặn Bidder vì Bidder không được đăng bán.
    void goToSellerProducts(ActionEvent actionEvent) throws IOException {
        UserRole role = UserAccount.getCurrentRole();
        if (role == UserRole.BIDDER) {
            showWarning("Bidder chỉ được đấu giá, không được đăng bán.");
            return;
        }
        switchScene(actionEvent, "/com/template/hellfx/SellerProducts.fxml");
    }

    // Hiện popup cảnh báo cho thao tác không hợp lệ.
    void showWarning(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // Load FXML mới, dừng timer dashboard nếu có và thay root scene hiện tại.
    void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        URL resource = controller.getClass().getResource(fxmlPath);
        if (resource == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi điều hướng");
            alert.setHeaderText("Không tìm thấy màn hình");
            alert.setContentText(fxmlPath);
            alert.showAndWait();
            return;
        }

        controller.stopLiveCountdown();
        Parent root = FXMLLoader.load(resource);
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        replaceSceneRoot(stage, root);
    }

    // Mở màn chi tiết phiên từ một card/list item bất kỳ.
    void openSessionDetailFromNode(Node source, long sessionId) {
        if (sessionId <= 0) {
            showWarning("Phiên không hợp lệ.");
            return;
        }

        try {
            SessionDetailController.setSessionId(sessionId);
            switchSceneFromNode(source, "/com/template/hellfx/session-detail.fxml");
        } catch (IOException e) {
            showWarning("Không mở được hoạt động phiên: " + e.getMessage());
        }
    }

    // Mở màn kết quả của phiên khi bấm vào sản phẩm đã thắng.
    void openWinnerFromNode(Node source, long sessionId) {
        try {
            WinnerController.setSessionId(sessionId);
            switchSceneFromNode(source, "/com/template/hellfx/Winner.fxml");
        } catch (IOException e) {
            showWarning("Không mở được kết quả phiên: " + e.getMessage());
        }
    }

    // Chuyển màn từ một Node bất kỳ, dùng cho các card/list item không phát ActionEvent.
    void switchSceneFromNode(Node source, String fxmlPath) throws IOException {
        URL resource = controller.getClass().getResource(fxmlPath);
        if (resource == null) {
            showWarning("Không tìm thấy màn hình: " + fxmlPath);
            return;
        }

        controller.stopLiveCountdown();
        Parent root = FXMLLoader.load(resource);
        Stage stage = (Stage) source.getScene().getWindow();
        replaceSceneRoot(stage, root);
    }

    // Thay root scene hiện tại và giữ kích thước cửa sổ thống nhất.
    private void replaceSceneRoot(Stage stage, Parent root) {
        Scene currentScene = stage.getScene();
        if (currentScene == null) {
            stage.setScene(new Scene(root, UILogin.APP_WIDTH, UILogin.APP_HEIGHT));
        } else {
            currentScene.setRoot(root);
        }
        stage.show();
    }
}
