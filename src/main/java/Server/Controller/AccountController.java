package Server.Controller;

import Client.Controller.UILogin;
import Common.DataBase.entities.Account;
import Common.DataBase.entities.Stake;
import Common.DataBase.entities.User;
import Common.DataBase.repository.AccountRepository;
import Common.DataBase.repository.StakeRepository;
import Common.DataBase.repository.UserRepository;
import Common.Enum.UserRole;
import Common.Model.user.UserAccount;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountController {

    @FXML
    private VBox userInfoPane;
    @FXML
    private VBox adminPane;

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
    private TableView<ManagedAccountRow> adminTable;
    @FXML
    private TableColumn<ManagedAccountRow, Long> colUserId;
    @FXML
    private TableColumn<ManagedAccountRow, String> colUsername;
    @FXML
    private TableColumn<ManagedAccountRow, String> colFullname;
    @FXML
    private TableColumn<ManagedAccountRow, String> colRole;
    @FXML
    private TableColumn<ManagedAccountRow, String> colPassword;
    @FXML
    private TableColumn<ManagedAccountRow, Long> colBalance;
    @FXML
    private TableColumn<ManagedAccountRow, Long> colLocked;
    @FXML
    private Label lblPageInfo;
    @FXML
    private TextField txtPageInput;

    private final UserRepository userRepository = new UserRepository();
    private final AccountRepository accountRepository = new AccountRepository();
    private final List<ManagedAccountRow> allAdminRows = new ArrayList<>();
    private static final int PAGE_SIZE = 8;
    private int currentPage = 1;

    @FXML
    public void initialize() {
        UserRole role = UserAccount.getCurrentRole();
        if (role == UserRole.ADMIN) {
            showAdminView();
        } else {
            showUserInfoView();
        }
    }

    private void showUserInfoView() {
        if (adminPane != null) {
            adminPane.setManaged(false);
            adminPane.setVisible(false);
        }
        if (userInfoPane != null) {
            userInfoPane.setManaged(true);
            userInfoPane.setVisible(true);
        }

        long userId = UserAccount.getUserId();
        String username = UserAccount.getCurrentUsername();
        String fullname = UserAccount.getCurrentFullname();
        UserRole role = UserAccount.getCurrentRole();

        Account account = accountRepository.getAccountByUserId(userId);
        //thong tin tai khoan
        lblTitle.setText("Thông tin tài khoản");
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
        if (adminPane != null) {
            adminPane.setManaged(true);
            adminPane.setVisible(true);
        }

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
        List<ManagedAccountRow> pageRows = fromIndex >= toIndex
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
        Task<List<ManagedAccountRow>> task = new Task<>() {
            @Override
            protected List<ManagedAccountRow> call() {
                List<User> users = userRepository.getallUser();
                List<Account> accounts = accountRepository.getAllAccount();
                Map<Long, Account> accountByUserId = new HashMap<>();
                for (Account account : accounts) {
                    accountByUserId.put(account.getUser_id(), account);
                }

                List<ManagedAccountRow> rows = new ArrayList<>(users.size());
                for (User user : users) {
                    Account account = accountByUserId.get(user.getId());
                    long balance = account == null ? 0 : account.getBalance();
                    long locked = account == null ? 0 : account.getLocked_balance();
                    rows.add(new ManagedAccountRow(
                            user.getId(),
                            user.getUsername(),
                            user.getFullname(),
                            user.getRole() == null ? "" : user.getRole().name(),
                            user.getPassword(),
                            balance,
                            locked
                    ));
                }
                return rows;
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

                StakeRepository repo = new StakeRepository();

                return repo.getByUserId(UserAccount.getUserId());
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

    private void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, UILogin.APP_WIDTH, UILogin.APP_HEIGHT));
        stage.show();
    }

    public static class ManagedAccountRow {
        private final Long userId;
        private final String username;
        private final String fullname;
        private final String role;
        private final String password;
        private final Long balance;
        private final Long lockedBalance;

        public ManagedAccountRow(Long userId, String username, String fullname, String role, String password, Long balance, Long lockedBalance) {
            this.userId = userId;
            this.username = username;
            this.fullname = fullname;
            this.role = role;
            this.password = password;
            this.balance = balance;
            this.lockedBalance = lockedBalance;
        }

        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getFullname() { return fullname; }
        public String getRole() { return role; }
        public String getPassword() { return password; }
        public Long getBalance() { return balance; }
        public Long getLockedBalance() { return lockedBalance; }
    }
}
