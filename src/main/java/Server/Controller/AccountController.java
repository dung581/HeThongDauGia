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
    private VBox userInfoPane;
    @FXML
    private VBox adminPane;
    @FXML
    private VBox stakePane;
    @FXML
    private HBox accountSummaryPane;
    @FXML
    private Node browseItemsNav;
    @FXML
    private Node uploadItemNav;
    @FXML
    private Node depositNav;
    @FXML
    private Node accountSection;
    @FXML
    private Button accountNavButton;

    @FXML
    private Label lblTitle;
    @FXML
    private Label lblUsername;
    @FXML
    private Label lblFullname;
    @FXML
    private Label lblRole;
    @FXML
    private Label lblBalance;
    @FXML
    private Label lblLocked;
    @FXML
    private Label lblAvailable;
    @FXML
    private Label lblBreadcrumb;
    @FXML
    private Label lblSubtitle;
    @FXML
    private Label lblFooter;
    @FXML
    private ListView<Stake> stakeTable;


    @FXML
    private ListView<AccountService.ManagedAccount> adminTable;
    @FXML
    private TextField accountSearchField;
    @FXML
    private ChoiceBox<String> accountRoleFilter;
    @FXML
    private TextField stakeSearchField;
    @FXML
    private ChoiceBox<String> stakeStatusFilter;

    private final AccountService accountService = new AccountService();
    private final StakeService stakeService = new StakeService();
    private final List<AccountService.ManagedAccount> allAdminRows = new ArrayList<>();
    private final List<Stake> allStakeRows = new ArrayList<>();
    private static final String ALL_FILTER = "Tat ca";

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
    private void configureRoleUi() {
        UserRole role = UserAccount.getCurrentRole();
        setVisibleManaged(browseItemsNav, role == UserRole.ADMIN);
        setVisibleManaged(uploadItemNav, role == UserRole.SELLER);
        setVisibleManaged(depositNav, role == UserRole.BIDDER);
        setVisibleManaged(accountSection, role == UserRole.SELLER || role == UserRole.BIDDER);
        if (accountNavButton != null) {
            accountNavButton.setText(role == UserRole.ADMIN ? "Account Management" : "My Account");
        }
    }

    // Set đồng thời visible và managed để node ẩn không chiếm chỗ trong layout.
    private void setVisibleManaged(Node node, boolean visible) {
        if (node == null) return;
        node.setVisible(visible);
        node.setManaged(visible);
    }

    // Set text an toàn cho Label có thể không tồn tại trong một số FXML.
    private void setLabel(Label label, String text) {
        if (label != null) {
            label.setText(text);
        }
    }

    // Hiển thị view tài khoản cá nhân cho Bidder/Seller, gồm số dư và lịch sử stake.
    private void showUserInfoView() {
        if (adminPane != null) {
            adminPane.setManaged(false);
            adminPane.setVisible(false);
        }
        setVisibleManaged(accountSummaryPane, true);
        setVisibleManaged(stakePane, true);
        if (userInfoPane != null) {
            userInfoPane.setManaged(true);
            userInfoPane.setVisible(true);
        }

        long userId = UserAccount.getUserId();
        String username = UserAccount.getCurrentUsername();
        String fullname = UserAccount.getCurrentFullname();
        UserRole role = UserAccount.getCurrentRole();

        Account account = null;
        try {
            account = accountService.getBalance(userId);
        } catch (Exception ignored) {
        }
        // Thông tin tài khoản.
        lblTitle.setText("Thông tin tài khoản");
        setLabel(lblBreadcrumb, "BidNow / My Account");
        setLabel(lblSubtitle, "Theo doi so du, tien dang khoa va lich su stake cua tai khoan.");
        setLabel(lblFooter, "BidNow Desktop | JavaFX 21 | Account workspace");
        lblUsername.setText(username == null ? "" : username);
        lblFullname.setText(fullname == null ? "" : fullname);
        lblRole.setText(role == null ? "" : role.name());

        configureStakeList();
        configureStakeFilters();

        // Tải dữ liệu lịch sử stake.
        loadStakeDataAsync();

        if (account != null) {
            long balance = account.getBalance();
            long locked = account.getLocked_balance();
            lblBalance.setText(String.valueOf(balance));
            lblLocked.setText(String.valueOf(locked));
            lblAvailable.setText(String.valueOf(balance - locked));
        } else {
            lblBalance.setText("0");
            lblLocked.setText("0");
            lblAvailable.setText("0");
        }
    }

    // Hiển thị view quản lý tài khoản cho Admin.
    private void showAdminView() {
        if (userInfoPane != null) {
            userInfoPane.setManaged(false);
            userInfoPane.setVisible(false);
        }
        setVisibleManaged(accountSummaryPane, false);
        setVisibleManaged(stakePane, false);
        if (adminPane != null) {
            adminPane.setManaged(true);
            adminPane.setVisible(true);
        }

        if (lblTitle != null) {
            lblTitle.setText("Quan ly tai khoan");
        }
        setLabel(lblBreadcrumb, "BidNow / Account Management");
        setLabel(lblSubtitle, "Xem danh sach nguoi dung, role, so du va tien dang khoa trong he thong.");
        setLabel(lblFooter, "BidNow Desktop | JavaFX 21 | Account management workspace");

        configureAdminList();
        configureAdminFilters();
        loadAdminDataAsync();
    }

    // Cấu hình lịch sử stake thành danh sách card.
    private void configureStakeList() {
        if (stakeTable == null) {
            return;
        }
        stakeTable.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Stake stake, boolean empty) {
                super.updateItem(stake, empty);
                if (empty || stake == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(null);
                setGraphic(createStakeCard(stake));
            }
        });
    }

    // Cấu hình danh sách tài khoản Admin thành card.
    private void configureAdminList() {
        if (adminTable == null) {
            return;
        }
        adminTable.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AccountService.ManagedAccount account, boolean empty) {
                super.updateItem(account, empty);
                if (empty || account == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(null);
                setGraphic(createManagedAccountCard(account));
            }
        });
    }

    // Tạo card hiển thị một stake.
    private Node createStakeCard(Stake stake) {
        HBox row = new HBox(14.0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14.0, 16.0, 14.0, 16.0));
        row.getStyleClass().add("data-row");

        VBox main = new VBox(5.0);
        HBox.setHgrow(main, Priority.ALWAYS);
        Label title = new Label("Stake #" + stake.getId());
        title.getStyleClass().add("data-title");
        HBox meta = new HBox(10.0);
        Label auction = new Label("Auction #" + stake.getAution_id());
        auction.getStyleClass().add("data-meta");
        Label item = new Label("Item #" + stake.getLocked_item_id());
        item.getStyleClass().add("data-meta");
        Label user = new Label("User #" + stake.getUser_id());
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

    // Tạo card hiển thị một tài khoản quản lý.
    private Node createManagedAccountCard(AccountService.ManagedAccount account) {
        HBox row = new HBox(14.0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14.0, 16.0, 14.0, 16.0));
        row.getStyleClass().add("data-row");

        VBox main = new VBox(5.0);
        HBox.setHgrow(main, Priority.ALWAYS);
        Label title = new Label(account.getUsername() == null ? "User #" + account.getUserId() : account.getUsername());
        title.getStyleClass().add("data-title");
        Label fullname = new Label(account.getFullname() == null ? "" : account.getFullname());
        fullname.getStyleClass().add("product-description");
        HBox meta = new HBox(10.0);
        Label id = new Label("User #" + account.getUserId());
        id.getStyleClass().add("data-meta");
        Label password = new Label("Pass: " + (account.getPassword() == null ? "" : account.getPassword()));
        password.getStyleClass().add("data-meta");
        meta.getChildren().addAll(id, password);
        main.getChildren().addAll(title, fullname, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox value = new VBox(6.0);
        value.setAlignment(Pos.CENTER_RIGHT);
        Label balance = new Label(String.format("%,d", account.getBalance()));
        balance.getStyleClass().add("data-money");
        Label locked = new Label("Locked " + String.format("%,d", account.getLockedBalance()));
        locked.getStyleClass().add("data-meta");
        Label role = new Label(account.getRole() == null ? "" : account.getRole());
        role.getStyleClass().add("data-pill");
        value.getChildren().addAll(balance, locked, role);

        row.getChildren().addAll(main, spacer, value);
        return row;
    }

    // Cấu hình tìm kiếm/lọc lịch sử stake cho tài khoản cá nhân.
    private void configureStakeFilters() {
        if (stakeStatusFilter != null) {
            stakeStatusFilter.setItems(FXCollections.observableArrayList(ALL_FILTER, "LOCKED", "RELEASED", "WON"));
            if (stakeStatusFilter.getValue() == null) {
                stakeStatusFilter.setValue(ALL_FILTER);
            }
            stakeStatusFilter.setOnAction(event -> applyStakeFilters());
        }

        if (stakeSearchField != null) {
            stakeSearchField.textProperty().addListener((observable, oldValue, newValue) -> applyStakeFilters());
        }
    }

    // Cấu hình tìm kiếm/lọc tài khoản cho Admin.
    private void configureAdminFilters() {
        if (accountRoleFilter != null) {
            accountRoleFilter.setItems(FXCollections.observableArrayList(
                    ALL_FILTER,
                    UserRole.ADMIN.name(),
                    UserRole.SELLER.name(),
                    UserRole.BIDDER.name()
            ));
            if (accountRoleFilter.getValue() == null) {
                accountRoleFilter.setValue(ALL_FILTER);
            }
            accountRoleFilter.setOnAction(event -> applyAdminFilters());
        }

        if (accountSearchField != null) {
            accountSearchField.textProperty().addListener((observable, oldValue, newValue) -> applyAdminFilters());
        }
    }

    // Tải danh sách tài khoản quản lý trên background thread để không khóa UI.
    private void loadAdminDataAsync() {
        Task<List<AccountService.ManagedAccount>> task = new Task<>() {
            @Override
            // Hàm chạy trong background task: lấy danh sách tài khoản để Admin quản lý.
            protected List<AccountService.ManagedAccount> call() {
                return accountService.listManagedAccounts();
            }
        };

        task.setOnSucceeded(event -> {
            allAdminRows.clear();
            allAdminRows.addAll(task.getValue());
            applyAdminFilters();
        });

        task.setOnFailed(event -> {
            allAdminRows.clear();
            applyAdminFilters();
        });

        Thread worker = new Thread(task, "account-admin-load");
        worker.setDaemon(true);
        worker.start();
    }

    // Tải lịch sử stake của user hiện tại trên background thread.
    private void loadStakeDataAsync() {

        Task<List<Stake>> task = new Task<>() {

            @Override
            // Hàm chạy trong background task: lấy lịch sử stake của user hiện tại.
            protected List<Stake> call() throws Exception {

                return stakeService.getUserStakes(UserAccount.getUserId());
            }
        };

        task.setOnSucceeded(event -> {
            allStakeRows.clear();
            allStakeRows.addAll(task.getValue());
            applyStakeFilters();
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
        });
        new Thread(task).start();
    }

    // Lọc danh sách tài khoản theo keyword và role, bảng sẽ tự cuộn thay vì phân trang.
    private void applyAdminFilters() {
        if (adminTable == null) {
            return;
        }

        String query = normalize(accountSearchField == null ? "" : accountSearchField.getText());
        String role = accountRoleFilter == null ? ALL_FILTER : accountRoleFilter.getValue();
        List<AccountService.ManagedAccount> rows = new ArrayList<>();

        for (AccountService.ManagedAccount account : allAdminRows) {
            if (!matchesAccountRole(account, role)) {
                continue;
            }
            if (!matchesAccountSearch(account, query)) {
                continue;
            }
            rows.add(account);
        }

        adminTable.setItems(FXCollections.observableArrayList(rows));
    }

    // Lọc lịch sử stake theo keyword và trạng thái.
    private void applyStakeFilters() {
        if (stakeTable == null) {
            return;
        }

        String query = normalize(stakeSearchField == null ? "" : stakeSearchField.getText());
        String status = stakeStatusFilter == null ? ALL_FILTER : stakeStatusFilter.getValue();
        List<Stake> rows = new ArrayList<>();

        for (Stake stake : allStakeRows) {
            if (!matchesStakeStatus(stake, status)) {
                continue;
            }
            if (!matchesStakeSearch(stake, query)) {
                continue;
            }
            rows.add(stake);
        }

        stakeTable.setItems(FXCollections.observableArrayList(rows));
    }

    // Kiểm tra role tài khoản có khớp bộ lọc Admin không.
    private boolean matchesAccountRole(AccountService.ManagedAccount account, String selectedRole) {
        if (selectedRole == null || selectedRole.equals(ALL_FILTER)) {
            return true;
        }
        return selectedRole.equals(account.getRole());
    }

    // Kiểm tra keyword theo user ID, username, họ tên, role và số dư.
    private boolean matchesAccountSearch(AccountService.ManagedAccount account, String query) {
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
        return normalize(target).contains(query);
    }

    // Kiểm tra trạng thái stake có khớp bộ lọc không.
    private boolean matchesStakeStatus(Stake stake, String selectedStatus) {
        if (selectedStatus == null || selectedStatus.equals(ALL_FILTER)) {
            return true;
        }
        return stake.getStatus() != null && stake.getStatus().name().equals(selectedStatus);
    }

    // Kiểm tra keyword theo ID stake, auction, item, user, số tiền và trạng thái.
    private boolean matchesStakeSearch(Stake stake, String query) {
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
        return normalize(target).contains(query);
    }

    // Chuẩn hóa chuỗi để tìm kiếm không phân biệt hoa thường.
    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    // Quay lại dashboard đúng với role hiện tại.
    public void backToDashboard(ActionEvent actionEvent) throws IOException {
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

    // Điều hướng về dashboard.
    @FXML
    public void goHome(ActionEvent actionEvent) throws IOException {
        backToDashboard(actionEvent);
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

    // Điều hướng sang chính màn tài khoản.
    @FXML
    public void goToAccount(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/account.fxml");
    }

    // Điều hướng sang màn nạp tiền, chỉ cho Bidder.
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

    // Thay root scene hiện tại bằng FXML mới và giữ kích thước chuẩn của ứng dụng.
    private void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
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
