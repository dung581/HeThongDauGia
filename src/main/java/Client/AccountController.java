package Client;

import Common.DataBase.entities.Account;
import Common.DataBase.entities.User;
import Common.DataBase.repository.AccountRepository;
import Common.DataBase.repository.UserRepository;
import Common.Enum.UserRole;
import Common.Model.user.UserAccount;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

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
    private TableColumn<ManagedAccountRow, Long> colBalance;
    @FXML
    private TableColumn<ManagedAccountRow, Long> colLocked;

    private final UserRepository userRepository = new UserRepository();
    private final AccountRepository accountRepository = new AccountRepository();

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

        lblTitle.setText("Thong tin tai khoan");
        lblUsername.setText(username == null ? "" : username);
        lblFullname.setText(fullname == null ? "" : fullname);
        lblRole.setText(role == null ? "" : role.name());

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

        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullname.setCellValueFactory(new PropertyValueFactory<>("fullname"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
        colLocked.setCellValueFactory(new PropertyValueFactory<>("lockedBalance"));

        List<User> users = userRepository.getallUser();
        ObservableList<ManagedAccountRow> rows = FXCollections.observableArrayList();
        for (User user : users) {
            Account account = accountRepository.getAccountByUserId(user.getId());
            long balance = account == null ? 0 : account.getBalance();
            long locked = account == null ? 0 : account.getLocked_balance();
            rows.add(new ManagedAccountRow(
                    user.getId(),
                    user.getUsername(),
                    user.getFullname(),
                    user.getRole() == null ? "" : user.getRole().name(),
                    balance,
                    locked
            ));
        }
        adminTable.setItems(rows);
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
        stage.setScene(new Scene(root));
        stage.show();
    }

    public static class ManagedAccountRow {
        private final Long userId;
        private final String username;
        private final String fullname;
        private final String role;
        private final Long balance;
        private final Long lockedBalance;

        public ManagedAccountRow(Long userId, String username, String fullname, String role, Long balance, Long lockedBalance) {
            this.userId = userId;
            this.username = username;
            this.fullname = fullname;
            this.role = role;
            this.balance = balance;
            this.lockedBalance = lockedBalance;
        }

        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getFullname() { return fullname; }
        public String getRole() { return role; }
        public Long getBalance() { return balance; }
        public Long getLockedBalance() { return lockedBalance; }
    }
}
