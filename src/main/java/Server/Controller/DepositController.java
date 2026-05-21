package Server.Controller;

import Common.DataBase.entities.Account;
import Common.Enum.UserRole;
import Common.Model.user.UserAccount;
import Server.service.AccountService;
import Client.util.AlertUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class DepositController {

    @FXML private TextField depositAmountField;
    @FXML private Label lblBalance;
    @FXML private Label lblLocked;
    @FXML private Label lblAvailable;
    @FXML private Node browseItemsNav;
    @FXML private Node uploadItemNav;

    private final AccountService accountService = new AccountService();

    // JavaFX tự gọi sau khi load FXML: cấu hình menu theo role và tải số dư hiện tại.
    @FXML
    public void initialize() {
        configureRoleUi();
        refreshBalance();
    }

    // Ẩn/hiện các nút điều hướng theo quyền của tài khoản đang đăng nhập.
    private void configureRoleUi() {
        UserRole role = UserAccount.getCurrentRole();
        setVisibleManaged(browseItemsNav, role == UserRole.ADMIN);
        setVisibleManaged(uploadItemNav, role == UserRole.SELLER);
    }

    // Set đồng thời visible và managed để node bị ẩn không còn chiếm chỗ trong layout.
    private void setVisibleManaged(Node node, boolean visible) {
        if (node == null) return;
        node.setVisible(visible);
        node.setManaged(visible);
    }

    // Tải số dư hiện tại của user và cập nhật các label tổng số dư/đang khóa/khả dụng.
    private void refreshBalance(){
        long userId = UserAccount.getUserId();
        if(userId <=0){
            setLabels(0,0,0);
            return;
        }
        try{
            Account acc = accountService.getBalance(userId);
            if(acc == null){
                setLabels(0,0,0);
                return;
            }
            long total = acc.getBalance();
            long locked = acc.getLocked_balance();
            setLabels(total, locked, total - locked);
        } catch (Exception e){
            setLabels(0,0,0);
        }
    }

    // Gán giá trị tiền lên ba label thông tin tài khoản.
    private void setLabels(long total, long locked, long availible){
        if(lblBalance != null){
            lblBalance.setText(String.valueOf(total));
        }
        if (lblLocked != null){
            lblLocked.setText(String.valueOf(locked));
        }
        if(lblAvailable != null){
            lblAvailable.setText(String.valueOf(availible));
        }
    }

    // Xử lý nút xác nhận nạp tiền: đọc input, validate số tiền rồi gọi AccountService.deposit.
    public void onConfirmDeposit() {
        String raw = depositAmountField == null ? "" : depositAmountField.getText();
        if (raw == null) raw = "";
        raw = raw.trim();

        long amount;
        try {
            amount = Long.parseLong(raw);
        } catch (NumberFormatException e) {
            showError("Số tiền không hợp lệ.");
            return;
        }

        if (amount <= 0) {
            showError("Số tiền phải lớn hơn 0.");
            return;
        }

        long userId = UserAccount.getUserId();
        if (userId <= 0) {
            showError("Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.");
            return;
        }

        try {
            accountService.deposit(userId, amount);
            refreshBalance();
            depositAmountField.clear();
            showInfo("Nạp thành công " + amount + " VND");
        } catch (Exception e) {
            showError("Nạp tiền thất bại: " + e.getMessage());
        }
    }

    // Quay về dashboard đúng với role hiện tại của user.
    public void onBack(ActionEvent actionEvent) throws IOException {
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

    // Điều hướng về màn dashboard.
    @FXML
    public void goHome(ActionEvent actionEvent) throws IOException {
        onBack(actionEvent);
    }

    // Điều hướng sang màn danh sách phiên đấu giá.
    @FXML
    public void goToSessions(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/danhSachDauGia.fxml");
    }

    // Điều hướng sang màn duyệt/quản lý item, chỉ cho Admin.
    @FXML
    public void goToBrowseItems(ActionEvent actionEvent) throws IOException {
        if (UserAccount.getCurrentRole() != UserRole.ADMIN) {
            AlertUtil.showError("Chuc nang nay chi danh cho Admin.");
            return;
        }
        switchScene(actionEvent, "/com/template/hellfx/ItemBrowse.fxml");
    }

    // Điều hướng sang màn tài khoản.
    @FXML
    public void goToAccount(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/account.fxml");
    }

    // Điều hướng sang chính màn nạp tiền, chỉ cho Bidder.
    @FXML
    public void goToDeposit(ActionEvent actionEvent) throws IOException {
        if (UserAccount.getCurrentRole() != UserRole.BIDDER) {
            AlertUtil.showError("Chuc nang nay chi danh cho nguoi dau gia.");
            return;
        }
        switchScene(actionEvent, "/com/template/hellfx/Deposit.fxml");
    }

    // Điều hướng sang màn sản phẩm của Seller, chỉ cho Seller.
    @FXML
    public void goToUploadItem(ActionEvent actionEvent) throws IOException {
        if (UserAccount.getCurrentRole() != UserRole.SELLER) {
            AlertUtil.showError("Chuc nang nay chi danh cho nguoi ban.");
            return;
        }
        switchScene(actionEvent, "/com/template/hellfx/SellerProducts.fxml");
    }

    // Thay root scene hiện tại bằng FXML mới.
    private void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene currentScene = stage.getScene();
        if (currentScene == null) {
            stage.setScene(new Scene(root));
        } else {
            currentScene.setRoot(root);
        }
        stage.show();
    }

    // Hiện popup lỗi cho các thao tác nạp tiền/điều hướng không hợp lệ.
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Loi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Hiện popup thông báo khi nạp tiền thành công.
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thong bao");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
