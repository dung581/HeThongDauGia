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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    private TableView<Stake> stakeTable;
    @FXML
    private TableColumn<Stake, Long> colStakeId;
    @FXML
    private TableColumn<Stake, Long> colAuctionId;
    @FXML
    private TableColumn<Stake, Long> colItemId;
    @FXML
    private TableColumn<Stake, Long> colStakeUserId;
    @FXML
    private TableColumn<Stake, Long> colAmount;
    @FXML
    private TableColumn<Stake, String> colStakeStatus;


    @FXML
    private TableView<AccountService.ManagedAccount> adminTable;
    @FXML
    private TableColumn<AccountService.ManagedAccount, Long> colUserId;
    @FXML
    private TableColumn<AccountService.ManagedAccount, String> colUsername;
    @FXML
    private TableColumn<AccountService.ManagedAccount, String> colFullname;
    @FXML
    private TableColumn<AccountService.ManagedAccount, String> colRole;
    @FXML
    private TableColumn<AccountService.ManagedAccount, String> colPassword;
    @FXML
    private TableColumn<AccountService.ManagedAccount, Long> colBalance;
    @FXML
    private TableColumn<AccountService.ManagedAccount, Long> colLocked;
    @FXML
    private Label lblPageInfo;
    @FXML
    private TextField txtPageInput;

    private final AccountService accountService = new AccountService();
    private final StakeService stakeService = new StakeService();
    private final List<AccountService.ManagedAccount> allAdminRows = new ArrayList<>();
    private static final int PAGE_SIZE = 8;
    private int currentPage = 1;

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

    private void setVisibleManaged(Node node, boolean visible) {
        if (node == null) return;
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void setLabel(Label label, String text) {
        if (label != null) {
            label.setText(text);
        }
    }

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
        //thong tin tai khoan
        lblTitle.setText("Thông tin tài khoản");
        setLabel(lblBreadcrumb, "BidNow / My Account");
        setLabel(lblSubtitle, "Theo doi so du, tien dang khoa va lich su stake cua tai khoan.");
        setLabel(lblFooter, "BidNow Desktop | JavaFX 21 | Account workspace");
        lblUsername.setText(username == null ? "" : username);
        lblFullname.setText(fullname == null ? "" : fullname);
        lblRole.setText(role == null ? "" : role.name());

        //cot lich su dau gia
        colStakeId.setCellValueFactory(new PropertyValueFactory<>("id")
        );

        colAuctionId.setCellValueFactory(new PropertyValueFactory<>("auction_id")
        );

        colItemId.setCellValueFactory(new PropertyValueFactory<>("item_id")
        );

        colStakeUserId.setCellValueFactory(new PropertyValueFactory<>("user_id")
        );

        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount")
        );

        colStakeStatus.setCellValueFactory(new PropertyValueFactory<>("status")
        );


        // LOAD DATA
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

        ensurePasswordColumn();

        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullname.setCellValueFactory(new PropertyValueFactory<>("fullname"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colPassword.setCellValueFactory(new PropertyValueFactory<>("password"));
        colBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
        colLocked.setCellValueFactory(new PropertyValueFactory<>("lockedBalance"));

        loadAdminDataAsync();
    }

    @FXML
    public void goFirstPage() {
        renderPage(1);
    }

    @FXML
    public void goPrevPage() {
        renderPage(currentPage - 1);
    }

    @FXML
    public void goNextPage() {
        renderPage(currentPage + 1);
    }

    @FXML
    public void goLastPage() {
        renderPage(getTotalPages());
    }

    @FXML
    public void goToPage() {
        if (txtPageInput == null || txtPageInput.getText() == null) {
            return;
        }
        try {
            int page = Integer.parseInt(txtPageInput.getText().trim());
            renderPage(page);
        } catch (NumberFormatException ignored) {
            renderPage(currentPage);
        }
    }

    private void renderPage(int page) {
        int totalPages = getTotalPages();
        if (totalPages <= 0) {
            totalPages = 1;
        }
        if (page < 1) {
            page = 1;
        }
        if (page > totalPages) {
            page = totalPages;
        }
        currentPage = page;

        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, allAdminRows.size());
        List<AccountService.ManagedAccount> pageRows = fromIndex >= toIndex
                ? List.of()
                : allAdminRows.subList(fromIndex, toIndex);

        adminTable.setItems(FXCollections.observableArrayList(pageRows));
        if (lblPageInfo != null) {
            lblPageInfo.setText("Trang " + currentPage + " / " + totalPages);
        }
    }

    private int getTotalPages() {
        if (allAdminRows.isEmpty()) {
            return 1;
        }
        return (allAdminRows.size() + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    private void loadAdminDataAsync() {
        Task<List<AccountService.ManagedAccount>> task = new Task<>() {
            @Override
            protected List<AccountService.ManagedAccount> call() {
                return accountService.listManagedAccounts();
            }
        };

        task.setOnSucceeded(event -> {
            allAdminRows.clear();
            allAdminRows.addAll(task.getValue());
            renderPage(1);
        });

        task.setOnFailed(event -> {
            allAdminRows.clear();
            renderPage(1);
        });

        Thread worker = new Thread(task, "account-admin-load");
        worker.setDaemon(true);
        worker.start();
    }

    private void loadStakeDataAsync() {

        Task<List<Stake>> task = new Task<>() {

            @Override
            protected List<Stake> call() throws Exception {

                return stakeService.getUserStakes(UserAccount.getUserId());
            }
        };

        task.setOnSucceeded(event -> {
            stakeTable.getItems().setAll(task.getValue()
            );
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
        });
        new Thread(task).start();
    }

    private void ensurePasswordColumn() {
        if (colPassword == null) {
            colPassword = new TableColumn<>("Password");
            colPassword.setPrefWidth(130.0);
        }

        if (adminTable == null || adminTable.getColumns().contains(colPassword)) {
            return;
        }

        int roleIndex = adminTable.getColumns().indexOf(colRole);
        int insertIndex = roleIndex >= 0 ? roleIndex + 1 : adminTable.getColumns().size();
        adminTable.getColumns().add(insertIndex, colPassword);
    }

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

    @FXML
    public void goHome(ActionEvent actionEvent) throws IOException {
        backToDashboard(actionEvent);
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
            stage.setScene(new Scene(root, UILogin.APP_WIDTH, UILogin.APP_HEIGHT));
        } else {
            currentScene.setRoot(root);
        }
        stage.show();
    }
}
