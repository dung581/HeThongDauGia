package Server.Controller.dashboard;

import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Item;
import Common.Enum.ItemStatus;
import Common.Model.user.UserAccount;
import Server.Controller.model.DashboardModels.ItemOverviewRow;
import Server.Controller.model.DashboardModels.SellerDashboardData;
import Server.Controller.model.DashboardModels.SessionOverviewRow;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Nhóm tải và render dashboard Seller.
final class DashboardSellerSection {
    private final DashBoardController controller;

    DashboardSellerSection(DashBoardController controller) {
        this.controller = controller;
    }

    // Tải số liệu dashboard Seller trên background thread.
    void loadDashboardAsync() {
        controller.setText(controller.sellerTotalItemsLabel, "-");
        controller.setText(controller.sellerPendingItemsLabel, "-");
        controller.setText(controller.sellerInAuctionItemsLabel, "-");
        controller.setText(controller.sellerSoldItemsLabel, "-");
        controller.setText(controller.sellerSidebarItemsLabel, "-");
        controller.setText(controller.sellerItemSummaryLabel, "Dang tai du lieu...");
        controller.setText(controller.sellerSessionSummaryLabel, "Dang tai du lieu...");

        Task<SellerDashboardData> task = new Task<>() {
            @Override
            protected SellerDashboardData call() {
                long sellerId = UserAccount.getUserId();
                List<Item> items = controller.itemService.listByOwner(sellerId);
                Set<Long> itemIds = items.stream()
                        .map(Item::getId)
                        .collect(Collectors.toSet());
                List<Auction> sessions = controller.auctionService.getAll().stream()
                        .filter(session -> itemIds.contains(session.getItem_id()))
                        .toList();

                long pending = items.stream()
                        .filter(item -> item.getStatus() == ItemStatus.PENDING)
                        .count();
                long inAuction = items.stream()
                        .filter(item -> item.getStatus() == ItemStatus.IN_AUCTION)
                        .count();
                long sold = items.stream()
                        .filter(item -> item.getStatus() == ItemStatus.SOLD)
                        .count();

                List<ItemOverviewRow> itemRows = items.stream()
                        .sorted(Comparator.comparingLong(Item::getId).reversed())
                        .map(item -> controller.toItemOverviewRow(item, false))
                        .toList();

                List<SessionOverviewRow> sessionRows = sessions.stream()
                        .sorted(Comparator.comparingLong(Auction::getId).reversed())
                        .limit(10)
                        .map(session -> controller.toSessionOverviewRow(session, items))
                        .toList();

                return new SellerDashboardData(items.size(), pending, inAuction, sold, itemRows, sessionRows);
            }
        };

        task.setOnSucceeded(event -> renderDashboard(task.getValue()));
        task.setOnFailed(event -> {
            controller.setText(controller.sellerTotalItemsLabel, "0");
            controller.setText(controller.sellerPendingItemsLabel, "0");
            controller.setText(controller.sellerInAuctionItemsLabel, "0");
            controller.setText(controller.sellerSoldItemsLabel, "0");
            controller.setText(controller.sellerSidebarItemsLabel, "0");
            controller.setText(controller.sellerItemSummaryLabel, "Khong tai duoc du lieu.");
            controller.setText(controller.sellerSessionSummaryLabel, "Khong tai duoc du lieu.");
            if (controller.sellerItemTable != null) {
                controller.sellerItemTable.setItems(FXCollections.observableArrayList());
            }
            if (controller.sellerSessionTable != null) {
                controller.sellerSessionTable.setItems(FXCollections.observableArrayList());
            }
        });

        startDaemonTask(task, "seller-dashboard-load");
    }

    // Đổ dữ liệu đã tải lên các thẻ thống kê và bảng của Seller.
    void renderDashboard(SellerDashboardData data) {
        controller.setText(controller.sellerTotalItemsLabel, String.valueOf(data.totalItems));
        controller.setText(controller.sellerPendingItemsLabel, String.valueOf(data.pendingItems));
        controller.setText(controller.sellerInAuctionItemsLabel, String.valueOf(data.inAuctionItems));
        controller.setText(controller.sellerSoldItemsLabel, String.valueOf(data.soldItems));
        controller.setText(controller.sellerSidebarItemsLabel, String.valueOf(data.totalItems));
        controller.setText(controller.sellerItemSummaryLabel, "Tat ca " + data.itemRows.size() + " item da dang ban");
        controller.setText(controller.sellerSessionSummaryLabel, data.sessionRows.size() + " phien lien quan");

        if (controller.sellerItemTable != null) {
            controller.sellerItemTable.setItems(FXCollections.observableArrayList(data.itemRows));
        }
        if (controller.sellerSessionTable != null) {
            controller.sellerSessionTable.setItems(FXCollections.observableArrayList(data.sessionRows));
        }
    }

    // Tạo daemon thread cho task nền của dashboard.
    private void startDaemonTask(Task<?> task, String threadName) {
        Thread worker = new Thread(task, threadName);
        worker.setDaemon(true);
        worker.start();
    }
}
