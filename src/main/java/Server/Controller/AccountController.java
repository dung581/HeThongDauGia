package Server.Controller;

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

// Nhóm cấu hình điều hướng hiển thị theo role hiện tại.
final class AccountRoleUiSection {
    private final AccountController controller;

    AccountRoleUiSection(AccountController controller) {
        this.controller = controller;
    }

    // Ẩn/hiện các mục điều hướng theo role hiện tại của user.
    void configureRoleUi() {
        UserRole role = UserAccount.getCurrentRole();
        controller.setVisibleManaged(controller.browseItemsNav, role == UserRole.ADMIN);
        controller.setVisibleManaged(controller.uploadItemNav, role == UserRole.SELLER);
        controller.setVisibleManaged(controller.depositNav, role == UserRole.BIDDER);
        controller.setVisibleManaged(controller.accountSection, role == UserRole.SELLER || role == UserRole.BIDDER);
        if (controller.accountNavButton != null) {
            controller.accountNavButton.setText(role == UserRole.ADMIN ? "Account Management" : "My Account");
        }
    }
}

// Nhóm hiển thị thông tin tài khoản cá nhân của Bidder/Seller.
final class AccountUserInfoSection {
    private final AccountController controller;

    AccountUserInfoSection(AccountController controller) {
        this.controller = controller;
    }

    // Hiển thị view tài khoản cá nhân, gồm số dư và lịch sử stake.
    void showUserInfoView() {
        if (controller.adminPane != null) {
            controller.adminPane.setManaged(false);
            controller.adminPane.setVisible(false);
        }
        controller.setVisibleManaged(controller.accountSummaryPane, true);
        controller.setVisibleManaged(controller.stakePane, true);
        if (controller.userInfoPane != null) {
            controller.userInfoPane.setManaged(true);
            controller.userInfoPane.setVisible(true);
        }

        long userId = UserAccount.getUserId();
        String username = UserAccount.getCurrentUsername();
        String fullname = UserAccount.getCurrentFullname();
        UserRole role = UserAccount.getCurrentRole();

        Account account = null;
        try {
            account = controller.accountService.getBalance(userId);
        } catch (Exception ignored) {
        }
        controller.lblTitle.setText("Thông tin tài khoản");
        controller.setLabel(controller.lblBreadcrumb, "BidNow / My Account");
        controller.setLabel(controller.lblSubtitle, "Theo doi so du, tien dang khoa va lich su stake cua tai khoan.");
        controller.setLabel(controller.lblFooter, "BidNow Desktop | JavaFX 21 | Account workspace");
        controller.lblUsername.setText(username == null ? "" : username);
        controller.lblFullname.setText(fullname == null ? "" : fullname);
        controller.lblRole.setText(role == null ? "" : role.name());

        controller.configureStakeList();
        controller.configureStakeFilters();
        controller.loadStakeDataAsync();

        if (account != null) {
            long balance = account.getBalance();
            long locked = account.getLocked_balance();
            controller.lblBalance.setText(String.valueOf(balance));
            controller.lblLocked.setText(String.valueOf(locked));
            controller.lblAvailable.setText(String.valueOf(balance - locked));
        } else {
            controller.lblBalance.setText("0");
            controller.lblLocked.setText("0");
            controller.lblAvailable.setText("0");
        }
    }
}

// Nhóm quản lý danh sách tài khoản dành cho Admin.
final class AccountAdminSection {
    private final AccountController controller;

    AccountAdminSection(AccountController controller) {
        this.controller = controller;
    }

    // Hiển thị view quản lý tài khoản cho Admin.
    void showAdminView() {
        if (controller.userInfoPane != null) {
            controller.userInfoPane.setManaged(false);
            controller.userInfoPane.setVisible(false);
        }
        controller.setVisibleManaged(controller.accountSummaryPane, false);
        controller.setVisibleManaged(controller.stakePane, false);
        if (controller.adminPane != null) {
            controller.adminPane.setManaged(true);
            controller.adminPane.setVisible(true);
        }

        if (controller.lblTitle != null) {
            controller.lblTitle.setText("Quan ly tai khoan");
        }
        controller.setLabel(controller.lblBreadcrumb, "BidNow / Account Management");
        controller.setLabel(controller.lblSubtitle, "Xem danh sach nguoi dung, role, so du va tien dang khoa trong he thong.");
        controller.setLabel(controller.lblFooter, "BidNow Desktop | JavaFX 21 | Account management workspace");

        controller.configureAdminList();
        controller.configureAdminFilters();
        controller.loadAdminDataAsync();
    }

    // Cấu hình danh sách tài khoản Admin thành card.
    void configureAdminList() {
        if (controller.adminTable == null) {
            return;
        }
        controller.adminTable.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AccountService.ManagedAccount account, boolean empty) {
                super.updateItem(account, empty);
                setText(null);
                setGraphic(empty || account == null ? null : controller.createManagedAccountCard(account));
            }
        });
    }

    // Tạo card hiển thị một tài khoản quản lý.
    Node createManagedAccountCard(AccountService.ManagedAccount account) {
        HBox row = new HBox(14.0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14.0, 16.0, 14.0, 16.0));
        row.getStyleClass().add("data-row");

        VBox main = new VBox(5.0);
        HBox.setHgrow(main, Priority.ALWAYS);
        Label title = new Label(account.getUsername() == null ? "Người dùng #" + account.getUserId() : account.getUsername());
        title.getStyleClass().add("data-title");
        Label fullname = new Label(account.getFullname() == null ? "" : account.getFullname());
        fullname.getStyleClass().add("product-description");
        HBox meta = new HBox(10.0);
        Label id = new Label("Người dùng #" + account.getUserId());
        id.getStyleClass().add("data-meta");
        meta.getChildren().add(id);
        main.getChildren().addAll(title, fullname, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox value = new VBox(6.0);
        value.setAlignment(Pos.CENTER_RIGHT);
        Label balance = new Label(String.format("%,d", account.getBalance()));
        balance.getStyleClass().add("data-money");
        Label locked = new Label("Đang khóa " + String.format("%,d", account.getLockedBalance()));
        locked.getStyleClass().add("data-meta");
        Label role = new Label(account.getRole() == null ? "" : account.getRole());
        role.getStyleClass().add("data-pill");
        value.getChildren().addAll(balance, locked, role);

        row.getChildren().addAll(main, spacer, value);
        return row;
    }

    // Cấu hình tìm kiếm/lọc tài khoản cho Admin.
    void configureAdminFilters() {
        if (controller.accountRoleFilter != null) {
            controller.accountRoleFilter.setItems(FXCollections.observableArrayList(
                    AccountController.ALL_FILTER,
                    UserRole.ADMIN.name(),
                    UserRole.SELLER.name(),
                    UserRole.BIDDER.name()
            ));
            if (controller.accountRoleFilter.getValue() == null) {
                controller.accountRoleFilter.setValue(AccountController.ALL_FILTER);
            }
            controller.accountRoleFilter.setOnAction(event -> controller.applyAdminFilters());
        }

        if (controller.accountSearchField != null) {
            controller.accountSearchField.textProperty()
                    .addListener((observable, oldValue, newValue) -> controller.applyAdminFilters());
        }
    }

    // Tải danh sách tài khoản quản lý trên background thread để không khóa UI.
    void loadAdminDataAsync() {
        Task<List<AccountService.ManagedAccount>> task = new Task<>() {
            @Override
            protected List<AccountService.ManagedAccount> call() {
                return controller.accountService.listManagedAccounts();
            }
        };

        task.setOnSucceeded(event -> {
            controller.allAdminRows.clear();
            controller.allAdminRows.addAll(task.getValue());
            controller.applyAdminFilters();
        });

        task.setOnFailed(event -> {
            controller.allAdminRows.clear();
            controller.applyAdminFilters();
        });

        Thread worker = new Thread(task, "account-admin-load");
        worker.setDaemon(true);
        worker.start();
    }

    // Lọc danh sách tài khoản theo keyword và role, bảng sẽ tự cuộn thay vì phân trang.
    void applyAdminFilters() {
        if (controller.adminTable == null) {
            return;
        }

        String query = controller.normalize(controller.accountSearchField == null ? "" : controller.accountSearchField.getText());
        String role = controller.accountRoleFilter == null ? AccountController.ALL_FILTER : controller.accountRoleFilter.getValue();
        List<AccountService.ManagedAccount> rows = new ArrayList<>();

        for (AccountService.ManagedAccount account : controller.allAdminRows) {
            if (!matchesAccountRole(account, role)) {
                continue;
            }
            if (!matchesAccountSearch(account, query)) {
                continue;
            }
            rows.add(account);
        }

        controller.adminTable.setItems(FXCollections.observableArrayList(rows));
    }

    // Kiểm tra role tài khoản có khớp bộ lọc Admin không.
    boolean matchesAccountRole(AccountService.ManagedAccount account, String selectedRole) {
        if (selectedRole == null || selectedRole.equals(AccountController.ALL_FILTER)) {
            return true;
        }
        return selectedRole.equals(account.getRole());
    }

    // Kiểm tra keyword theo user ID, username, họ tên, role và số dư.
    boolean matchesAccountSearch(AccountService.ManagedAccount account, String query) {
        if (query.isEmpty()) {
            return true;
        }

        String target = String.join(" ",
                String.valueOf(account.getUserId()),
                account.getUsername() == null ? "" : account.getUsername(),
                account.getFullname() == null ? "" : account.getFullname(),
                account.getRole() == null ? "" : account.getRole(),
                String.valueOf(account.getBalance()),
                String.valueOf(account.getLockedBalance())
        );
        return controller.normalize(target).contains(query);
    }
}

// Nhóm lịch sử stake của tài khoản cá nhân.
final class AccountStakeSection {
    private final AccountController controller;

    AccountStakeSection(AccountController controller) {
        this.controller = controller;
    }

    // Cấu hình lịch sử stake thành danh sách card.
    void configureStakeList() {
        if (controller.stakeTable == null) {
            return;
        }
        controller.stakeTable.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Stake stake, boolean empty) {
                super.updateItem(stake, empty);
                setText(null);
                setGraphic(empty || stake == null ? null : controller.createStakeCard(stake));
            }
        });
    }

    // Tạo card hiển thị một stake.
    Node createStakeCard(Stake stake) {
        HBox row = new HBox(14.0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14.0, 16.0, 14.0, 16.0));
        row.getStyleClass().add("data-row");

        VBox main = new VBox(5.0);
        HBox.setHgrow(main, Priority.ALWAYS);
        Label title = new Label("Đặt cọc #" + stake.getId());
        title.getStyleClass().add("data-title");
        HBox meta = new HBox(10.0);
        Label auction = new Label("Đấu giá #" + stake.getAution_id());
        auction.getStyleClass().add("data-meta");
        Label item = new Label("Sản phẩm #" + stake.getLocked_item_id());
        item.getStyleClass().add("data-meta");
        Label user = new Label("Người dùng #" + stake.getUser_id());
        user.getStyleClass().add("data-meta");
        meta.getChildren().addAll(auction, item, user);
        main.getChildren().addAll(title, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox value = new VBox(6.0);
        value.setAlignment(Pos.CENTER_RIGHT);
        Label amount = new Label(String.format("%,d", stake.getAmount()));
        amount.getStyleClass().add("data-money");
        Label status = new Label(stake.getStatus() == null ? "" : stake.getStatus().name());
        status.getStyleClass().add("data-pill");
        value.getChildren().addAll(amount, status);

        row.getChildren().addAll(main, spacer, value);
        return row;
    }

    // Cấu hình tìm kiếm/lọc lịch sử stake cho tài khoản cá nhân.
    void configureStakeFilters() {
        if (controller.stakeStatusFilter != null) {
            controller.stakeStatusFilter.setItems(FXCollections.observableArrayList(AccountController.ALL_FILTER, "LOCKED", "RELEASED", "WON"));
            if (controller.stakeStatusFilter.getValue() == null) {
                controller.stakeStatusFilter.setValue(AccountController.ALL_FILTER);
            }
            controller.stakeStatusFilter.setOnAction(event -> controller.applyStakeFilters());
        }

        if (controller.stakeSearchField != null) {
            controller.stakeSearchField.textProperty()
                    .addListener((observable, oldValue, newValue) -> controller.applyStakeFilters());
        }
    }

    // Tải lịch sử stake của user hiện tại trên background thread.
    void loadStakeDataAsync() {
        Task<List<Stake>> task = new Task<>() {
            @Override
            protected List<Stake> call() {
                return controller.stakeService.getUserStakes(UserAccount.getUserId());
            }
        };

        task.setOnSucceeded(event -> {
            controller.allStakeRows.clear();
            controller.allStakeRows.addAll(task.getValue());
            controller.applyStakeFilters();
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
        });

        Thread worker = new Thread(task, "account-stake-load");
        worker.setDaemon(true);
        worker.start();
    }

    // Lọc lịch sử stake theo keyword và trạng thái.
    void applyStakeFilters() {
        if (controller.stakeTable == null) {
            return;
        }

        String query = controller.normalize(controller.stakeSearchField == null ? "" : controller.stakeSearchField.getText());
        String status = controller.stakeStatusFilter == null ? AccountController.ALL_FILTER : controller.stakeStatusFilter.getValue();
        List<Stake> rows = new ArrayList<>();

        for (Stake stake : controller.allStakeRows) {
            if (!matchesStakeStatus(stake, status)) {
                continue;
            }
            if (!matchesStakeSearch(stake, query)) {
                continue;
            }
            rows.add(stake);
        }

        controller.stakeTable.setItems(FXCollections.observableArrayList(rows));
    }

    // Kiểm tra trạng thái stake có khớp bộ lọc không.
    boolean matchesStakeStatus(Stake stake, String selectedStatus) {
        if (selectedStatus == null || selectedStatus.equals(AccountController.ALL_FILTER)) {
            return true;
        }
        return stake.getStatus() != null && stake.getStatus().name().equals(selectedStatus);
    }

    // Kiểm tra keyword theo ID stake, auction, item, user, số tiền và trạng thái.
    boolean matchesStakeSearch(Stake stake, String query) {
        if (query.isEmpty()) {
            return true;
        }

        String target = String.join(" ",
                String.valueOf(stake.getId()),
                String.valueOf(stake.getAution_id()),
                String.valueOf(stake.getLocked_item_id()),
                String.valueOf(stake.getUser_id()),
                String.valueOf(stake.getAmount()),
                stake.getStatus() == null ? "" : stake.getStatus().name()
        );
        return controller.normalize(target).contains(query);
    }
}

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
