package Server.Controller.account;

import Common.Enum.UserRole;
import Server.service.AccountService;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

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
