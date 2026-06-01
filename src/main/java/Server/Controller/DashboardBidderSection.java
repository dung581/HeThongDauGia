package Server.Controller;

import Common.DataBase.entities.Account;
import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Item;
import Common.DataBase.entities.Stake;
import Common.Enum.AuctionState;
import Common.Enum.StakeStatus;
import Common.Model.user.UserAccount;
import Server.Controller.model.DashboardModels.BidderDashboardData;
import Server.Controller.model.DashboardModels.EndedSessionRow;
import Server.Controller.model.DashboardModels.LiveSessionRow;
import Server.Controller.model.DashboardModels.OwnedProductRow;
import Server.service.AccountService;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.List;

// Nhóm tải và render dashboard Bidder.
final class DashboardBidderSection {
    private final DashBoardController controller;

    DashboardBidderSection(DashBoardController controller) {
        this.controller = controller;
    }

    // Tải dữ liệu dashboard Bidder: số dư, phiên đang chạy, phiên đã kết thúc và sản phẩm đã thắng.
    void loadDashboardAsync() {
        loadDashboardAsync(true);
    }

    // Refresh ngầm dùng khi auto bid chạy nền; không đổi label sang "Đang tải..." để tránh giật UI.
    void refreshDashboardSilently() {
        loadDashboardAsync(false);
    }

    // Tải dashboard có tùy chọn hiện trạng thái loading hay giữ nguyên UI cũ.
    private void loadDashboardAsync(boolean showLoading) {
        if (controller.bidderDashboardLoadRunning) {
            return;
        }
        controller.bidderDashboardLoadRunning = true;

        if (showLoading) {
            controller.setText(controller.availableBalanceLabel, "Dang tai...");
            controller.setText(controller.lockedBalanceLabel, "Dang tai...");
            controller.setText(controller.activeStakeCountLabel, "-");
            controller.setText(controller.leadingSessionCountLabel, "-");
            controller.setText(controller.liveSessionSummaryLabel, "Dang tai du lieu...");
            controller.setText(controller.endedSessionSummaryLabel, "Dang tai du lieu...");
            controller.setText(controller.ownedProductSummaryLabel, "Dang tai du lieu...");
            controller.setText(controller.sidebarLiveLabel, "-");
        }

        Task<BidderDashboardData> task = new Task<>() {
            @Override
            protected BidderDashboardData call() {
                long userId = UserAccount.getUserId();
                Account account = controller.accountService.getBalance(userId);
                List<Stake> stakes = controller.stakeService.getUserStakes(userId);
                List<Auction> allSessions = controller.auctionService.getAll();
                List<Item> allItems = controller.itemService.listAll();
                List<AccountService.ManagedAccount> managedAccounts = controller.accountService.listManagedAccounts();
                List<Auction> liveSessions = allSessions.stream()
                        .filter(controller::isLiveSession)
                        .toList();
                List<Auction> endedSessions = allSessions.stream()
                        .filter(controller::isEndedSession)
                        .toList();

                long lockedStakeCount = stakes.stream()
                        .filter(stake -> stake.getStatus() == StakeStatus.LOCKED)
                        .count();
                long leadingCount = liveSessions.stream()
                        .filter(session -> session.getCurrent_user_id() == userId)
                        .count();

                List<LiveSessionRow> liveRows = liveSessions.stream()
                        .sorted(Comparator.comparing(Auction::getEndTime, Comparator.nullsLast(Comparator.naturalOrder())))
                        .limit(8)
                        .map(session -> controller.toLiveSessionRow(session, userId, allItems, managedAccounts))
                        .toList();

                List<EndedSessionRow> endedRows = endedSessions.stream()
                        .sorted(Comparator.comparing(Auction::getEndTime, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(8)
                        .map(session -> controller.toEndedSessionRow(session, userId, allItems))
                        .toList();

                List<OwnedProductRow> ownedProducts = endedSessions.stream()
                        .filter(session -> session.getCurrent_user_id() == userId)
                        .filter(session -> session.getState() != AuctionState.CANCELED)
                        .sorted(Comparator.comparing(Auction::getEndTime, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(8)
                        .map(session -> controller.toOwnedProductRow(session, allItems))
                        .toList();

                long total = account.getBalance();
                long locked = account.getLocked_balance();
                long available = total - locked;
                return new BidderDashboardData(
                        available,
                        locked,
                        lockedStakeCount,
                        leadingCount,
                        liveSessions.size(),
                        endedSessions.size(),
                        liveRows,
                        endedRows,
                        ownedProducts
                );
            }
        };

        task.setOnSucceeded(event -> {
            controller.bidderDashboardLoadRunning = false;
            renderDashboard(task.getValue());
        });
        task.setOnFailed(event -> {
            controller.bidderDashboardLoadRunning = false;
            if (!showLoading) {
                return;
            }
            controller.setText(controller.availableBalanceLabel, "N/A");
            controller.setText(controller.lockedBalanceLabel, "N/A");
            controller.setText(controller.activeStakeCountLabel, "0");
            controller.setText(controller.leadingSessionCountLabel, "0");
            controller.setText(controller.liveSessionSummaryLabel, "Khong tai duoc du lieu dashboard.");
            controller.setText(controller.endedSessionSummaryLabel, "Khong tai duoc du lieu dashboard.");
            controller.setText(controller.ownedProductSummaryLabel, "Khong tai duoc du lieu.");
            controller.setText(controller.sidebarLiveLabel, "0");
            controller.stopLiveCountdown();
            if (controller.liveSessionTable != null) {
                controller.liveSessionTable.setItems(FXCollections.observableArrayList());
            }
            if (controller.endedSessionTable != null) {
                controller.endedSessionTable.setItems(FXCollections.observableArrayList());
            }
            renderOwnedProducts(List.of());
        });

        startDaemonTask(task, "bidder-dashboard-load");
    }

    // Đổ dữ liệu dashboard Bidder lên các thẻ thống kê và bảng.
    void renderDashboard(BidderDashboardData data) {
        controller.setText(controller.availableBalanceLabel, controller.formatMoney(data.availableBalance));
        controller.setText(controller.lockedBalanceLabel, controller.formatMoney(data.lockedBalance));
        controller.setText(controller.activeStakeCountLabel, String.valueOf(data.lockedStakeCount));
        controller.setText(controller.leadingSessionCountLabel, String.valueOf(data.leadingSessionCount));
        controller.setText(controller.liveSessionSummaryLabel, data.liveSessionCount + " phien dang mo");
        controller.setText(controller.endedSessionSummaryLabel, data.endedSessionCount + " phien da ket thuc");
        controller.setText(controller.sidebarLiveLabel, String.valueOf(data.liveSessionCount));

        if (controller.liveSessionTable != null) {
            controller.liveSessionRows.setAll(data.liveSessions);
            controller.liveSessionTable.setItems(controller.liveSessionRows);
            controller.startLiveCountdown();
        }
        if (controller.endedSessionTable != null) {
            controller.endedSessionRows.setAll(data.endedSessions);
            controller.endedSessionTable.setItems(controller.endedSessionRows);
        }
        renderOwnedProducts(data.ownedProducts);
    }

    // Render danh sách sản phẩm đã thắng theo dạng từng thanh sản phẩm, không dùng bảng cứng.
    void renderOwnedProducts(List<OwnedProductRow> products) {
        if (controller.ownedProductList == null) {
            return;
        }

        controller.ownedProductList.getChildren().clear();
        List<OwnedProductRow> rows = products == null ? List.of() : products;
        controller.setText(controller.ownedProductSummaryLabel, rows.size() + " san pham");

        if (rows.isEmpty()) {
            VBox empty = new VBox(6.0);
            empty.setPadding(new Insets(18.0));
            empty.getStyleClass().add("product-empty");
            Label title = new Label("Chua co san pham nao");
            title.getStyleClass().add("product-title");
            Label subtitle = new Label("San pham ban thang dau gia se hien thi o day.");
            subtitle.getStyleClass().add("page-subtitle");
            subtitle.setWrapText(true);
            empty.getChildren().addAll(title, subtitle);
            controller.ownedProductList.getChildren().add(empty);
            return;
        }

        for (OwnedProductRow row : rows) {
            controller.ownedProductList.getChildren().add(controller.createOwnedProductNode(row));
        }
    }

    // Tạo daemon thread cho task nền của dashboard.
    private void startDaemonTask(Task<?> task, String threadName) {
        Thread worker = new Thread(task, threadName);
        worker.setDaemon(true);
        worker.start();
    }
}
