package Server.Controller.dashboard;

import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Item;
import Common.Enum.ItemStatus;
import Server.Controller.model.DashboardModels.AdminDashboardData;
import Server.Controller.model.DashboardModels.ItemOverviewRow;
import Server.Controller.model.DashboardModels.SessionOverviewRow;
import Server.service.AccountService;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.List;

// Nhóm tải và render dashboard Admin.
final class DashboardAdminSection {
    private final DashBoardController controller;

    DashboardAdminSection(DashBoardController controller) {
        this.controller = controller;
    }

    // Tải số liệu dashboard Admin trên background thread để UI không bị lag.
    void loadDashboardAsync() {
        controller.setText(controller.adminActiveSessionsLabel, "-");
        controller.setText(controller.adminPendingItemsLabel, "-");
        controller.setText(controller.adminTotalItemsLabel, "-");
        controller.setText(controller.adminTotalUsersLabel, "-");
        controller.setText(controller.adminSidebarPendingLabel, "-");
        controller.setText(controller.adminSessionSummaryLabel, "Dang tai du lieu...");
        controller.setText(controller.adminPendingSummaryLabel, "Dang tai du lieu...");

        Task<AdminDashboardData> task = new Task<>() {
            @Override
            protected AdminDashboardData call() {
                List<Item> items = controller.itemService.listAll();
                List<Auction> sessions = controller.auctionService.getAll();
                List<AccountService.ManagedAccount> accounts = controller.accountService.listManagedAccounts();

                long activeSessions = sessions.stream()
                        .filter(controller::isLiveSession)
                        .count();
                long pendingItems = items.stream()
                        .filter(item -> item.getStatus() == ItemStatus.PENDING)
                        .count();

                List<SessionOverviewRow> sessionRows = sessions.stream()
                        .sorted(Comparator.comparingLong(Auction::getId).reversed())
                        .limit(10)
                        .map(session -> controller.toSessionOverviewRow(session, items))
                        .toList();

                List<ItemOverviewRow> pendingRows = items.stream()
                        .filter(item -> item.getStatus() == ItemStatus.PENDING)
                        .sorted(Comparator.comparingLong(Item::getId).reversed())
                        .limit(10)
                        .map(item -> controller.toItemOverviewRow(item, true))
                        .toList();

                return new AdminDashboardData(
                        activeSessions,
                        pendingItems,
                        items.size(),
                        accounts.size(),
                        sessionRows,
                        pendingRows
                );
            }
        };

        task.setOnSucceeded(event -> renderDashboard(task.getValue()));
        task.setOnFailed(event -> {
            controller.setText(controller.adminActiveSessionsLabel, "0");
            controller.setText(controller.adminPendingItemsLabel, "0");
            controller.setText(controller.adminTotalItemsLabel, "0");
            controller.setText(controller.adminTotalUsersLabel, "0");
            controller.setText(controller.adminSidebarPendingLabel, "0");
            controller.setText(controller.adminSessionSummaryLabel, "Khong tai duoc du lieu.");
            controller.setText(controller.adminPendingSummaryLabel, "Khong tai duoc du lieu.");
            renderSessionList(List.of());
            if (controller.adminPendingItemTable != null) {
                controller.adminPendingItemTable.setItems(FXCollections.observableArrayList());
            }
        });

        startDaemonTask(task, "admin-dashboard-load");
    }

    // Đổ dữ liệu đã tải lên các thẻ thống kê và bảng của Admin.
    void renderDashboard(AdminDashboardData data) {
        controller.setText(controller.adminActiveSessionsLabel, String.valueOf(data.activeSessions));
        controller.setText(controller.adminPendingItemsLabel, String.valueOf(data.pendingItems));
        controller.setText(controller.adminTotalItemsLabel, String.valueOf(data.totalItems));
        controller.setText(controller.adminTotalUsersLabel, String.valueOf(data.totalUsers));
        controller.setText(controller.adminSidebarPendingLabel, String.valueOf(data.pendingItems));
        controller.setText(controller.adminSessionSummaryLabel, data.sessionRows.size() + " phien gan day");
        controller.setText(controller.adminPendingSummaryLabel, data.pendingRows.size() + " item dang cho duyet");

        renderSessionList(data.sessionRows);
        if (controller.adminPendingItemTable != null) {
            controller.adminPendingItemTable.setItems(FXCollections.observableArrayList(data.pendingRows));
        }
    }

    // Render phiên gần đây của Admin thành từng thanh mềm, có thể bấm để xem hoạt động phiên.
    void renderSessionList(List<SessionOverviewRow> rows) {
        if (controller.adminSessionList == null) {
            return;
        }

        controller.adminSessionList.getChildren().clear();
        List<SessionOverviewRow> sessions = rows == null ? List.of() : rows;
        if (sessions.isEmpty()) {
            VBox empty = new VBox(6.0);
            empty.setPadding(new Insets(18.0));
            empty.getStyleClass().add("product-empty");
            Label title = new Label("Chưa có phiên gần đây");
            title.getStyleClass().add("product-title");
            Label subtitle = new Label("Các phiên đấu giá mới tạo hoặc vừa kết thúc sẽ hiển thị ở đây.");
            subtitle.getStyleClass().add("page-subtitle");
            subtitle.setWrapText(true);
            empty.getChildren().addAll(title, subtitle);
            controller.adminSessionList.getChildren().add(empty);
            return;
        }

        for (SessionOverviewRow row : sessions) {
            controller.adminSessionList.getChildren().add(controller.createAdminSessionNode(row));
        }
    }

    // Tạo daemon thread cho task nền của dashboard.
    private void startDaemonTask(Task<?> task, String threadName) {
        Thread worker = new Thread(task, threadName);
        worker.setDaemon(true);
        worker.start();
    }
}
