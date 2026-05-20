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

    @FXML
    public void initialize() {
        configureRoleUi();
        refreshBalance();
    }

    private void configureRoleUi() {
        UserRole role = UserAccount.getCurrentRole();
        setVisibleManaged(browseItemsNav, role == UserRole.ADMIN);
        setVisibleManaged(uploadItemNav, role == UserRole.SELLER);
    }

    private void setVisibleManaged(Node node, boolean visible) {
        if (node == null) return;
        node.setVisible(visible);
        node.setManaged(visible);
    }

    // hien thi so du hien tai cua user
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

    @FXML
    public void goHome(ActionEvent actionEvent) throws IOException {
        onBack(actionEvent);
    }

    @FXML
    public void goToSessions(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/danhSachDauGia.fxml");
    }

    @FXML
    public void goToBrowseItems(ActionEvent actionEvent) throws IOException {
        if (UserAccount.getCurrentRole() != UserRole.ADMIN) {
            AlertUtil.showError("Chuc nang nay chi danh cho Admin.");
            return;
        }
        switchScene(actionEvent, "/com/template/hellfx/ItemBrowse.fxml");
    }

    @FXML
    public void goToAccount(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/account.fxml");
    }

    @FXML
    public void goToDeposit(ActionEvent actionEvent) throws IOException {
        if (UserAccount.getCurrentRole() != UserRole.BIDDER) {
            AlertUtil.showError("Chuc nang nay chi danh cho nguoi dau gia.");
            return;
        }
        switchScene(actionEvent, "/com/template/hellfx/Deposit.fxml");
    }

    @FXML
    public void goToUploadItem(ActionEvent actionEvent) throws IOException {
        if (UserAccount.getCurrentRole() != UserRole.SELLER) {
            AlertUtil.showError("Chuc nang nay chi danh cho nguoi ban.");
            return;
        }
        switchScene(actionEvent, "/com/template/hellfx/SellerProducts.fxml");
    }

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

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Loi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thong bao");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
