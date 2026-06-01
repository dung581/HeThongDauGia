package Server.Controller.account;

import Client.Controller.UILogin;
import Client.util.AlertUtil;
import Common.DataBase.entities.Account;
import Common.DataBase.entities.Stake;
import Common.Enum.UserRole;
import Common.Model.user.UserAccount;
import Server.service.AccountService;
import Server.service.StakeService;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AccountController {

    @FXML
    VBox userInfoPane;
    @FXML
    VBox adminPane;
    @FXML
    VBox stakePane;
    @FXML
    HBox accountSummaryPane;
    @FXML
    Node browseItemsNav;
    @FXML
    Node uploadItemNav;
    @FXML
    Node depositNav;
    @FXML
    Node accountSection;
    @FXML
    Button accountNavButton;

    @FXML
    Label lblTitle;
    @FXML
    Label lblUsername;
    @FXML
    Label lblFullname;
    @FXML
    Label lblRole;
    @FXML
    Label lblBalance;
    @FXML
    Label lblLocked;
    @FXML
    Label lblAvailable;
    @FXML
    Label lblBreadcrumb;
    @FXML
    Label lblSubtitle;
    @FXML
    Label lblFooter;
    @FXML
    ListView<Stake> stakeTable;


    @FXML
    ListView<AccountService.ManagedAccount> adminTable;
    @FXML
    TextField accountSearchField;
    @FXML
    ChoiceBox<String> accountRoleFilter;
    @FXML
    TextField stakeSearchField;
    @FXML
    ChoiceBox<String> stakeStatusFilter;

    final AccountService accountService = new AccountService();
    final StakeService stakeService = new StakeService();
    final List<AccountService.ManagedAccount> allAdminRows = new ArrayList<>();
    final List<Stake> allStakeRows = new ArrayList<>();
    static final String ALL_FILTER = "Tat ca";
    final AccountRoleUiSection roleUiSection = new AccountRoleUiSection(this);
    final AccountUserInfoSection userInfoSection = new AccountUserInfoSection(this);
    final AccountAdminSection adminSection = new AccountAdminSection(this);
    final AccountStakeSection stakeSection = new AccountStakeSection(this);
    final AccountNavigation navigation = new AccountNavigation(this);

    // JavaFX tự gọi sau khi load FXML: cấu hình menu theo role và chọn view tài khoản phù hợp.
    @FXML
    public void initialize() {
        configureRoleUi();
        UserRole role = UserAccount.getCurrentRole();
        if (role == UserRole.ADMIN) {
            showAdminView();
        } else {
            showUserInfoView();
        }
    }

    // Ẩn/hiện các mục điều hướng theo role hiện tại của user.
    void configureRoleUi() {
        roleUiSection.configureRoleUi();
    }

    // Set đồng thời visible và managed để node ẩn không chiếm chỗ trong layout.
    void setVisibleManaged(Node node, boolean visible) {
        if (node == null) return;
        node.setVisible(visible);
        node.setManaged(visible);
    }

    // Set text an toàn cho Label có thể không tồn tại trong một số FXML.
    void setLabel(Label label, String text) {
        if (label != null) {
            label.setText(text);
        }
    }

    // Hiển thị view tài khoản cá nhân cho Bidder/Seller, gồm số dư và lịch sử stake.
    void showUserInfoView() {
        userInfoSection.showUserInfoView();
    }

    // Hiển thị view quản lý tài khoản cho Admin.
    void showAdminView() {
        adminSection.showAdminView();
    }

    // Cấu hình lịch sử stake thành danh sách card.
    void configureStakeList() {
        stakeSection.configureStakeList();
    }

    // Cấu hình danh sách tài khoản Admin thành card.
    void configureAdminList() {
        adminSection.configureAdminList();
    }

    // Tạo card hiển thị một stake.
    Node createStakeCard(Stake stake) {
        return stakeSection.createStakeCard(stake);
    }

    // Tạo card hiển thị một tài khoản quản lý.
    Node createManagedAccountCard(AccountService.ManagedAccount account) {
        return adminSection.createManagedAccountCard(account);
    }

    // Cấu hình tìm kiếm/lọc lịch sử stake cho tài khoản cá nhân.
    void configureStakeFilters() {
        stakeSection.configureStakeFilters();
    }

    // Cấu hình tìm kiếm/lọc tài khoản cho Admin.
    void configureAdminFilters() {
        adminSection.configureAdminFilters();
    }

    // Tải danh sách tài khoản quản lý trên background thread để không khóa UI.
    void loadAdminDataAsync() {
        adminSection.loadAdminDataAsync();
    }

    // Tải lịch sử stake của user hiện tại trên background thread.
    void loadStakeDataAsync() {
        stakeSection.loadStakeDataAsync();
    }

    // Lọc danh sách tài khoản theo keyword và role, bảng sẽ tự cuộn thay vì phân trang.
    void applyAdminFilters() {
        adminSection.applyAdminFilters();
    }

    // Lọc lịch sử stake theo keyword và trạng thái.
    void applyStakeFilters() {
        stakeSection.applyStakeFilters();
    }

    // Kiểm tra role tài khoản có khớp bộ lọc Admin không.
    boolean matchesAccountRole(AccountService.ManagedAccount account, String selectedRole) {
        return adminSection.matchesAccountRole(account, selectedRole);
    }

    // Kiểm tra keyword theo user ID, username, họ tên, role và số dư.
    boolean matchesAccountSearch(AccountService.ManagedAccount account, String query) {
        return adminSection.matchesAccountSearch(account, query);
    }

    // Kiểm tra trạng thái stake có khớp bộ lọc không.
    boolean matchesStakeStatus(Stake stake, String selectedStatus) {
        return stakeSection.matchesStakeStatus(stake, selectedStatus);
    }

    // Kiểm tra keyword theo ID stake, auction, item, user, số tiền và trạng thái.
    boolean matchesStakeSearch(Stake stake, String query) {
        return stakeSection.matchesStakeSearch(stake, query);
    }

    // Chuẩn hóa chuỗi để tìm kiếm không phân biệt hoa thường.
    String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    // Quay lại dashboard đúng với role hiện tại.
    @FXML
    public void backToDashboard(ActionEvent actionEvent) throws IOException {
        navigation.backToDashboard(actionEvent);
    }

    // Điều hướng về dashboard.
    @FXML
    public void goHome(ActionEvent actionEvent) throws IOException {
        backToDashboard(actionEvent);
    }

    // Điều hướng sang màn danh sách phiên đấu giá.
    @FXML
    public void goToSessions(ActionEvent actionEvent) throws IOException {
        navigation.switchScene(actionEvent, "/com/template/hellfx/danhSachDauGia.fxml");
    }

    // Điều hướng sang màn duyệt/quản lý item, chỉ cho Admin.
    @FXML
    public void goToBrowseItems(ActionEvent actionEvent) throws IOException {
        navigation.goToBrowseItems(actionEvent);
    }

    // Điều hướng sang chính màn tài khoản.
    @FXML
    public void goToAccount(ActionEvent actionEvent) throws IOException {
        navigation.switchScene(actionEvent, "/com/template/hellfx/account.fxml");
    }

    // Điều hướng sang màn nạp tiền, chỉ cho Bidder.
    @FXML
    public void goToDeposit(ActionEvent actionEvent) throws IOException {
        navigation.goToDeposit(actionEvent);
    }

    // Điều hướng sang màn sản phẩm của Seller, chỉ cho Seller.
    @FXML
    public void goToUploadItem(ActionEvent actionEvent) throws IOException {
        navigation.goToUploadItem(actionEvent);
    }

    // Thay root scene hiện tại bằng FXML mới và giữ kích thước chuẩn của ứng dụng.
    void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        navigation.switchScene(actionEvent, fxmlPath);
    }
}
