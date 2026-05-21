package Server.Controller;

import Client.Controller.UILogin;
import Common.DataBase.entities.Account;
import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Item;
import Common.DataBase.entities.Stake;
import Common.Enum.AuctionState;
import Common.Enum.ItemStatus;
import Common.Enum.StakeStatus;
import Common.Enum.UserRole;
import Common.Model.user.UserAccount;
import Server.service.AccountService;
import Server.service.AuctionService;
import Server.service.ItemService;
import Server.service.StakeService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class DashBoardController {

    @FXML private Label availableBalanceLabel;
    @FXML private Label lockedBalanceLabel;
    @FXML private Label activeStakeCountLabel;
    @FXML private Label leadingSessionCountLabel;
    @FXML private Label liveSessionSummaryLabel;
    @FXML private Label sidebarUserLabel;
    @FXML private Label sidebarRoleLabel;
    @FXML private Label sidebarLiveLabel;
    @FXML private Label endedSessionSummaryLabel;
    @FXML private TableView<LiveSessionRow> liveSessionTable;
    @FXML private TableColumn<LiveSessionRow, String> liveItemColumn;
    @FXML private TableColumn<LiveSessionRow, String> livePriceColumn;
    @FXML private TableColumn<LiveSessionRow, String> liveLeaderColumn;
    @FXML private TableColumn<LiveSessionRow, String> liveTimeColumn;
    @FXML private TableView<EndedSessionRow> endedSessionTable;
    @FXML private TableColumn<EndedSessionRow, String> endedItemColumn;
    @FXML private TableColumn<EndedSessionRow, String> endedPriceColumn;
    @FXML private TableColumn<EndedSessionRow, String> endedWinnerColumn;
    @FXML private TableColumn<EndedSessionRow, String> endedStatusColumn;
    @FXML private TableView<StakeRow> recentStakeTable;
    @FXML private TableColumn<StakeRow, Long> stakeSessionColumn;
    @FXML private TableColumn<StakeRow, Long> stakeItemColumn;
    @FXML private TableColumn<StakeRow, String> stakeAmountColumn;
    @FXML private TableColumn<StakeRow, String> stakeStatusColumn;
    @FXML private Label adminActiveSessionsLabel;
    @FXML private Label adminPendingItemsLabel;
    @FXML private Label adminTotalItemsLabel;
    @FXML private Label adminTotalUsersLabel;
    @FXML private Label adminSidebarUserLabel;
    @FXML private Label adminSidebarPendingLabel;
    @FXML private Label adminSessionSummaryLabel;
    @FXML private Label adminPendingSummaryLabel;
    @FXML private TableView<SessionOverviewRow> adminSessionTable;
    @FXML private TableColumn<SessionOverviewRow, String> adminSessionItemColumn;
    @FXML private TableColumn<SessionOverviewRow, String> adminSessionPriceColumn;
    @FXML private TableColumn<SessionOverviewRow, String> adminSessionLeaderColumn;
    @FXML private TableColumn<SessionOverviewRow, String> adminSessionStatusColumn;
    @FXML private TableView<ItemOverviewRow> adminPendingItemTable;
    @FXML private TableColumn<ItemOverviewRow, Long> adminPendingIdColumn;
    @FXML private TableColumn<ItemOverviewRow, String> adminPendingNameColumn;
    @FXML private TableColumn<ItemOverviewRow, String> adminPendingSellerColumn;
    @FXML private TableColumn<ItemOverviewRow, String> adminPendingPriceColumn;
    @FXML private Label sellerTotalItemsLabel;
    @FXML private Label sellerPendingItemsLabel;
    @FXML private Label sellerInAuctionItemsLabel;
    @FXML private Label sellerSoldItemsLabel;
    @FXML private Label sellerSidebarUserLabel;
    @FXML private Label sellerSidebarRoleLabel;
    @FXML private Label sellerSidebarItemsLabel;
    @FXML private Label sellerItemSummaryLabel;
    @FXML private Label sellerSessionSummaryLabel;
    @FXML private TableView<ItemOverviewRow> sellerItemTable;
    @FXML private TableColumn<ItemOverviewRow, Long> sellerItemIdColumn;
    @FXML private TableColumn<ItemOverviewRow, String> sellerItemNameColumn;
    @FXML private TableColumn<ItemOverviewRow, String> sellerItemPriceColumn;
    @FXML private TableColumn<ItemOverviewRow, String> sellerItemStatusColumn;
    @FXML private TableView<SessionOverviewRow> sellerSessionTable;
    @FXML private TableColumn<SessionOverviewRow, String> sellerSessionItemColumn;
    @FXML private TableColumn<SessionOverviewRow, String> sellerSessionPriceColumn;
    @FXML private TableColumn<SessionOverviewRow, String> sellerSessionLeaderColumn;
    @FXML private TableColumn<SessionOverviewRow, String> sellerSessionStatusColumn;

    private final AccountService accountService = new AccountService();
    private final AuctionService auctionService = new AuctionService();
    private final StakeService stakeService = new StakeService();
    private final ItemService itemService = new ItemService();
    private Timeline liveCountdownTimeline;
    private final ObservableList<LiveSessionRow> liveSessionRows = FXCollections.observableArrayList();
    private final ObservableList<EndedSessionRow> endedSessionRows = FXCollections.observableArrayList();

    // JavaFX tự gọi sau khi load FXML: nhận diện dashboard theo fx:id rồi tải dữ liệu tương ứng.
    @FXML
    public void initialize() {
        if (availableBalanceLabel != null) {
            configureBidderTables();
            setText(sidebarUserLabel, UserAccount.getCurrentUsername());
            setText(sidebarRoleLabel, UserAccount.getCurrentRole() == null ? "" : UserAccount.getCurrentRole().name());
            loadBidderDashboardAsync();
        } else if (adminActiveSessionsLabel != null) {
            configureAdminTables();
            setText(adminSidebarUserLabel, UserAccount.getCurrentUsername());
            loadAdminDashboardAsync();
        } else if (sellerTotalItemsLabel != null) {
            configureSellerTables();
            setText(sellerSidebarUserLabel, UserAccount.getCurrentUsername());
            setText(sellerSidebarRoleLabel, UserAccount.getCurrentRole() == null ? "" : UserAccount.getCurrentRole().name());
            loadSellerDashboardAsync();
        }
    }

    // Điều hướng về dashboard đúng với role hiện tại.
    public void goHome(ActionEvent actionEvent) throws IOException {
        UserRole role = UserAccount.getCurrentRole();
        if (role == UserRole.ADMIN) {
            switchScene(actionEvent, "/com/template/hellfx/dashboard - Admin.fxml");
        } else if (role == UserRole.SELLER) {
            switchScene(actionEvent, "/com/template/hellfx/dashboard - Seller.fxml");
        } else {
            switchScene(actionEvent, "/com/template/hellfx/dashboard-Bidder.fxml");
        }
    }

    // Đăng xuất: xóa session user và quay về màn đăng nhập.
    public void goToLogin(ActionEvent actionEvent) throws IOException {
        UserAccount.clearSession();
        switchScene(actionEvent, "/com/template/hellfx/UILogin.fxml");
    }

    // Mở màn danh sách phiên đấu giá.
    public void Sandaugia(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/danhSachDauGia.fxml");
    }

    // Mở màn sản phẩm của Seller; chặn Bidder vì Bidder không được đăng bán.
    public void dangban(ActionEvent actionEvent) throws IOException {
        UserRole role = UserAccount.getCurrentRole();
        if (role == UserRole.BIDDER) {
            showWarning("Bidder chỉ được đấu giá, không được đăng bán.");
            return;
        }
        switchScene(actionEvent, "/com/template/hellfx/SellerProducts.fxml");
    }

    // Mở màn tài khoản: Admin xem quản lý account, role khác xem tài khoản cá nhân.
    public void quanlytk(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/account.fxml");
    }

    // Mở màn nạp tiền.
    public void goToDeposit(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/Deposit.fxml");
    }

    // Mở màn duyệt sản phẩm đang chờ cho Admin.
    public void duyetsp(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/ApproveItem.fxml");
    }

    // Mở màn quản lý toàn bộ vật phẩm cho Admin.
    public void quanlyvatpham(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/AdminItemManagement.fxml");
    }

    // Hiện popup cảnh báo cho thao tác không hợp lệ.
    private void showWarning(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // Load FXML mới, dừng timer dashboard nếu có và thay root scene hiện tại.
    private void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        URL resource = getClass().getResource(fxmlPath);
        if (resource == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi điều hướng");
            alert.setHeaderText("Không tìm thấy màn hình");
            alert.setContentText(fxmlPath);
            alert.showAndWait();
            return;
        }

        stopLiveCountdown();
        Parent root = FXMLLoader.load(resource);
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene currentScene = stage.getScene();
        if (currentScene == null) {
            stage.setScene(new Scene(root, UILogin.APP_WIDTH, UILogin.APP_HEIGHT));
        } else {
            currentScene.setRoot(root);
        }
        stage.show();
    }

    // Cấu hình các cột bảng riêng của dashboard Bidder.
    private void configureBidderTables() {
        if (liveItemColumn != null) {
            liveItemColumn.setCellValueFactory(new PropertyValueFactory<>("itemName"));
            livePriceColumn.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
            liveLeaderColumn.setCellValueFactory(new PropertyValueFactory<>("leader"));
            liveTimeColumn.setCellValueFactory(cellData -> cellData.getValue().timeLeftProperty());
        }

        if (endedItemColumn != null) {
            endedItemColumn.setCellValueFactory(new PropertyValueFactory<>("itemName"));
            endedPriceColumn.setCellValueFactory(new PropertyValueFactory<>("finalPrice"));
            endedWinnerColumn.setCellValueFactory(new PropertyValueFactory<>("winner"));
            endedStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        }

        if (stakeSessionColumn != null) {
            stakeSessionColumn.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
            stakeItemColumn.setCellValueFactory(new PropertyValueFactory<>("itemId"));
            stakeAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
            stakeStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        }
    }

    // Cấu hình các cột bảng riêng của dashboard Admin.
    private void configureAdminTables() {
        if (adminSessionItemColumn != null) {
            adminSessionItemColumn.setCellValueFactory(new PropertyValueFactory<>("itemName"));
            adminSessionPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
            adminSessionLeaderColumn.setCellValueFactory(new PropertyValueFactory<>("leader"));
            adminSessionStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        }

        if (adminPendingIdColumn != null) {
            adminPendingIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
            adminPendingNameColumn.setCellValueFactory(new PropertyValueFactory<>("itemName"));
            adminPendingSellerColumn.setCellValueFactory(new PropertyValueFactory<>("owner"));
            adminPendingPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        }
    }

    // Cấu hình các cột bảng riêng của dashboard Seller.
    private void configureSellerTables() {
        if (sellerItemIdColumn != null) {
            sellerItemIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
            sellerItemNameColumn.setCellValueFactory(new PropertyValueFactory<>("itemName"));
            sellerItemPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
            sellerItemStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        }

        if (sellerSessionItemColumn != null) {
            sellerSessionItemColumn.setCellValueFactory(new PropertyValueFactory<>("itemName"));
            sellerSessionPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
            sellerSessionLeaderColumn.setCellValueFactory(new PropertyValueFactory<>("leader"));
            sellerSessionStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        }
    }

    // Tải số liệu dashboard Admin trên background thread để UI không bị lag.
    private void loadAdminDashboardAsync() {
        setText(adminActiveSessionsLabel, "-");
        setText(adminPendingItemsLabel, "-");
        setText(adminTotalItemsLabel, "-");
        setText(adminTotalUsersLabel, "-");
        setText(adminSidebarPendingLabel, "-");
        setText(adminSessionSummaryLabel, "Dang tai du lieu...");
        setText(adminPendingSummaryLabel, "Dang tai du lieu...");

        Task<AdminDashboardData> task = new Task<>() {
            @Override
            // Hàm chạy trong background task: tổng hợp dữ liệu dashboard Admin.
            protected AdminDashboardData call() {
                List<Item> items = itemService.listAll();
                List<Auction> sessions = auctionService.getAll();
                List<AccountService.ManagedAccount> accounts = accountService.listManagedAccounts();

                long activeSessions = sessions.stream()
                        .filter(DashBoardController.this::isLiveSession)
                        .count();
                long pendingItems = items.stream()
                        .filter(item -> item.getStatus() == ItemStatus.PENDING)
                        .count();

                List<SessionOverviewRow> sessionRows = sessions.stream()
                        .sorted(Comparator.comparingLong(Auction::getId).reversed())
                        .limit(10)
                        .map(session -> toSessionOverviewRow(session, items))
                        .toList();

                List<ItemOverviewRow> pendingRows = items.stream()
                        .filter(item -> item.getStatus() == ItemStatus.PENDING)
                        .sorted(Comparator.comparingLong(Item::getId).reversed())
                        .limit(10)
                        .map(item -> toItemOverviewRow(item, true))
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

        task.setOnSucceeded(event -> renderAdminDashboard(task.getValue()));
        task.setOnFailed(event -> {
            setText(adminActiveSessionsLabel, "0");
            setText(adminPendingItemsLabel, "0");
            setText(adminTotalItemsLabel, "0");
            setText(adminTotalUsersLabel, "0");
            setText(adminSidebarPendingLabel, "0");
            setText(adminSessionSummaryLabel, "Khong tai duoc du lieu.");
            setText(adminPendingSummaryLabel, "Khong tai duoc du lieu.");
            if (adminSessionTable != null) {
                adminSessionTable.setItems(FXCollections.observableArrayList());
            }
            if (adminPendingItemTable != null) {
                adminPendingItemTable.setItems(FXCollections.observableArrayList());
            }
        });

        Thread worker = new Thread(task, "admin-dashboard-load");
        worker.setDaemon(true);
        worker.start();
    }

    // Đổ dữ liệu đã tải lên các thẻ thống kê và bảng của Admin.
    private void renderAdminDashboard(AdminDashboardData data) {
        setText(adminActiveSessionsLabel, String.valueOf(data.activeSessions));
        setText(adminPendingItemsLabel, String.valueOf(data.pendingItems));
        setText(adminTotalItemsLabel, String.valueOf(data.totalItems));
        setText(adminTotalUsersLabel, String.valueOf(data.totalUsers));
        setText(adminSidebarPendingLabel, String.valueOf(data.pendingItems));
        setText(adminSessionSummaryLabel, data.sessionRows.size() + " phien gan day");
        setText(adminPendingSummaryLabel, data.pendingRows.size() + " item dang cho duyet");

        if (adminSessionTable != null) {
            adminSessionTable.setItems(FXCollections.observableArrayList(data.sessionRows));
        }
        if (adminPendingItemTable != null) {
            adminPendingItemTable.setItems(FXCollections.observableArrayList(data.pendingRows));
        }
    }

    // Tải số liệu dashboard Seller trên background thread.
    private void loadSellerDashboardAsync() {
        setText(sellerTotalItemsLabel, "-");
        setText(sellerPendingItemsLabel, "-");
        setText(sellerInAuctionItemsLabel, "-");
        setText(sellerSoldItemsLabel, "-");
        setText(sellerSidebarItemsLabel, "-");
        setText(sellerItemSummaryLabel, "Dang tai du lieu...");
        setText(sellerSessionSummaryLabel, "Dang tai du lieu...");

        Task<SellerDashboardData> task = new Task<>() {
            @Override
            // Hàm chạy trong background task: tổng hợp dữ liệu dashboard Seller.
            protected SellerDashboardData call() {
                long sellerId = UserAccount.getUserId();
                List<Item> items = itemService.listByOwner(sellerId);
                Set<Long> itemIds = items.stream()
                        .map(Item::getId)
                        .collect(Collectors.toSet());
                List<Auction> sessions = auctionService.getAll().stream()
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
                        .limit(10)
                        .map(item -> toItemOverviewRow(item, false))
                        .toList();

                List<SessionOverviewRow> sessionRows = sessions.stream()
                        .sorted(Comparator.comparingLong(Auction::getId).reversed())
                        .limit(10)
                        .map(session -> toSessionOverviewRow(session, items))
                        .toList();

                return new SellerDashboardData(items.size(), pending, inAuction, sold, itemRows, sessionRows);
            }
        };

        task.setOnSucceeded(event -> renderSellerDashboard(task.getValue()));
        task.setOnFailed(event -> {
            setText(sellerTotalItemsLabel, "0");
            setText(sellerPendingItemsLabel, "0");
            setText(sellerInAuctionItemsLabel, "0");
            setText(sellerSoldItemsLabel, "0");
            setText(sellerSidebarItemsLabel, "0");
            setText(sellerItemSummaryLabel, "Khong tai duoc du lieu.");
            setText(sellerSessionSummaryLabel, "Khong tai duoc du lieu.");
            if (sellerItemTable != null) {
                sellerItemTable.setItems(FXCollections.observableArrayList());
            }
            if (sellerSessionTable != null) {
                sellerSessionTable.setItems(FXCollections.observableArrayList());
            }
        });

        Thread worker = new Thread(task, "seller-dashboard-load");
        worker.setDaemon(true);
        worker.start();
    }

    // Đổ dữ liệu đã tải lên các thẻ thống kê và bảng của Seller.
    private void renderSellerDashboard(SellerDashboardData data) {
        setText(sellerTotalItemsLabel, String.valueOf(data.totalItems));
        setText(sellerPendingItemsLabel, String.valueOf(data.pendingItems));
        setText(sellerInAuctionItemsLabel, String.valueOf(data.inAuctionItems));
        setText(sellerSoldItemsLabel, String.valueOf(data.soldItems));
        setText(sellerSidebarItemsLabel, String.valueOf(data.totalItems));
        setText(sellerItemSummaryLabel, data.itemRows.size() + " item gan day");
        setText(sellerSessionSummaryLabel, data.sessionRows.size() + " phien lien quan");

        if (sellerItemTable != null) {
            sellerItemTable.setItems(FXCollections.observableArrayList(data.itemRows));
        }
        if (sellerSessionTable != null) {
            sellerSessionTable.setItems(FXCollections.observableArrayList(data.sessionRows));
        }
    }

    // Kiểm tra một phiên có đang chạy thực sự hay không dựa trên state và thời gian kết thúc.
    private boolean isLiveSession(Auction session) {
        return session.getState() == AuctionState.RUNNING
                && session.getEndTime() != null
                && session.getEndTime().isAfter(LocalDateTime.now());
    }

    // Chuyển state của phiên sang chuỗi dễ đọc trên UI.
    private String displaySessionStatus(Auction session) {
        if (session.getState() == AuctionState.RUNNING && !isLiveSession(session)) {
            return "ENDED";
        }
        return session.getState() == null ? "" : session.getState().name();
    }

    // Tìm tên item theo itemId trong danh sách item đã tải.
    private String findItemName(List<Item> items, long itemId) {
        if (items == null) {
            return "Item " + itemId;
        }
        return items.stream()
                .filter(item -> item.getId() == itemId)
                .map(Item::getFullname)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse("Item " + itemId);
    }

    // Chuyển entity Auction thành dòng hiển thị tổng quan phiên.
    private SessionOverviewRow toSessionOverviewRow(Auction session, List<Item> items) {
        String leader = session.getCurrent_user_id() == 0 ? "-" : String.valueOf(session.getCurrent_user_id());
        return new SessionOverviewRow(
                findItemName(items, session.getItem_id()),
                formatMoney(session.getCurrent_price()),
                leader,
                displaySessionStatus(session)
        );
    }

    // Chuyển entity Item thành dòng hiển thị tổng quan item.
    private ItemOverviewRow toItemOverviewRow(Item item, boolean includeOwner) {
        return new ItemOverviewRow(
                item.getId(),
                item.getFullname(),
                includeOwner ? String.valueOf(item.getOwner_user_id()) : "",
                formatMoney(item.getBeginPrice()),
                item.getStatus() == null ? "" : item.getStatus().name()
        );
    }

    // Tải dữ liệu dashboard Bidder: số dư, phiên đang chạy, phiên đã kết thúc và stake gần đây.
    private void loadBidderDashboardAsync() {
        setText(availableBalanceLabel, "Dang tai...");
        setText(lockedBalanceLabel, "Dang tai...");
        setText(activeStakeCountLabel, "-");
        setText(leadingSessionCountLabel, "-");
        setText(liveSessionSummaryLabel, "Dang tai du lieu...");
        setText(endedSessionSummaryLabel, "Dang tai du lieu...");
        setText(sidebarLiveLabel, "-");

        Task<BidderDashboardData> task = new Task<>() {
            @Override
            // Hàm chạy trong background task: tổng hợp số dư, stake và phiên của Bidder.
            protected BidderDashboardData call() {
                long userId = UserAccount.getUserId();
                Account account = accountService.getBalance(userId);
                List<Stake> stakes = stakeService.getUserStakes(userId);
                List<Auction> allSessions = auctionService.getAll();
                List<Auction> liveSessions = allSessions.stream()
                        .filter(this::isLiveSession)
                        .toList();
                List<Auction> endedSessions = allSessions.stream()
                        .filter(this::isEndedSession)
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
                        .map(session -> toLiveSessionRow(session, userId))
                        .toList();

                List<EndedSessionRow> endedRows = endedSessions.stream()
                        .sorted(Comparator.comparing(Auction::getEndTime, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(8)
                        .map(session -> toEndedSessionRow(session, userId))
                        .toList();

                List<StakeRow> stakeRows = stakes.stream()
                        .sorted(Comparator.comparingLong(Stake::getId).reversed())
                        .limit(10)
                        .map(this::toStakeRow)
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
                        stakeRows
                );
            }

            // Kiểm tra phiên live trong phạm vi dữ liệu tải của Bidder.
            private boolean isLiveSession(Auction session) {
                return session.getState() == AuctionState.RUNNING
                        && session.getEndTime() != null
                        && session.getEndTime().isAfter(LocalDateTime.now());
            }

            // Kiểm tra phiên đã kết thúc trong phạm vi dữ liệu tải của Bidder.
            private boolean isEndedSession(Auction session) {
                return session.getState() != AuctionState.RUNNING
                        || session.getEndTime() == null
                        || !session.getEndTime().isAfter(LocalDateTime.now());
            }

            // Chuyển Auction đang chạy thành dòng hiển thị trong bảng live sessions.
            private LiveSessionRow toLiveSessionRow(Auction session, long userId) {
                String itemName = "Item " + session.getItem_id();
                try {
                    Item item = itemService.getById(session.getItem_id());
                    if (item.getFullname() != null && !item.getFullname().isBlank()) {
                        itemName = item.getFullname();
                    }
                } catch (Exception ignored) {
                }

                String leader = session.getCurrent_user_id() == 0
                        ? "-"
                        : session.getCurrent_user_id() == userId ? "Ban" : String.valueOf(session.getCurrent_user_id());
                return new LiveSessionRow(
                        session.getId(),
                        itemName,
                        formatMoney(session.getCurrent_price()),
                        leader,
                        session.getEndTime()
                );
            }

            // Chuyển Auction đã kết thúc thành dòng hiển thị trong bảng ended sessions.
            private EndedSessionRow toEndedSessionRow(Auction session, long userId) {
                String itemName = "Item " + session.getItem_id();
                try {
                    Item item = itemService.getById(session.getItem_id());
                    if (item.getFullname() != null && !item.getFullname().isBlank()) {
                        itemName = item.getFullname();
                    }
                } catch (Exception ignored) {
                }

                String winner = session.getCurrent_user_id() == 0
                        ? "-"
                        : session.getCurrent_user_id() == userId ? "Ban" : String.valueOf(session.getCurrent_user_id());
                String status = session.getState() == AuctionState.RUNNING ? "ENDED" : session.getState().name();
                return new EndedSessionRow(
                        session.getId(),
                        itemName,
                        formatMoney(session.getCurrent_price()),
                        winner,
                        status
                );
            }

            // Chuyển Stake thành dòng hiển thị trong bảng lịch sử stake gần đây.
            private StakeRow toStakeRow(Stake stake) {
                return new StakeRow(
                        stake.getAution_id(),
                        stake.getLocked_item_id(),
                        formatMoney(stake.getAmount()),
                        stake.getStatus() == null ? "" : stake.getStatus().name()
                );
            }
        };

        task.setOnSucceeded(event -> renderBidderDashboard(task.getValue()));
        task.setOnFailed(event -> {
            setText(availableBalanceLabel, "N/A");
            setText(lockedBalanceLabel, "N/A");
            setText(activeStakeCountLabel, "0");
            setText(leadingSessionCountLabel, "0");
            setText(liveSessionSummaryLabel, "Khong tai duoc du lieu dashboard.");
            setText(endedSessionSummaryLabel, "Khong tai duoc du lieu dashboard.");
            setText(sidebarLiveLabel, "0");
            stopLiveCountdown();
            if (liveSessionTable != null) {
                liveSessionTable.setItems(FXCollections.observableArrayList());
            }
            if (recentStakeTable != null) {
                recentStakeTable.setItems(FXCollections.observableArrayList());
            }
            if (endedSessionTable != null) {
                endedSessionTable.setItems(FXCollections.observableArrayList());
            }
        });

        Thread worker = new Thread(task, "bidder-dashboard-load");
        worker.setDaemon(true);
        worker.start();
    }

    // Đổ dữ liệu dashboard Bidder lên các thẻ thống kê và bảng.
    private void renderBidderDashboard(BidderDashboardData data) {
        setText(availableBalanceLabel, formatMoney(data.availableBalance));
        setText(lockedBalanceLabel, formatMoney(data.lockedBalance));
        setText(activeStakeCountLabel, String.valueOf(data.lockedStakeCount));
        setText(leadingSessionCountLabel, String.valueOf(data.leadingSessionCount));
        setText(liveSessionSummaryLabel, data.liveSessionCount + " phien dang mo");
        setText(endedSessionSummaryLabel, data.endedSessionCount + " phien da ket thuc");
        setText(sidebarLiveLabel, String.valueOf(data.liveSessionCount));

        if (liveSessionTable != null) {
            liveSessionRows.setAll(data.liveSessions);
            liveSessionTable.setItems(liveSessionRows);
            startLiveCountdown();
        }
        if (endedSessionTable != null) {
            endedSessionRows.setAll(data.endedSessions);
            endedSessionTable.setItems(endedSessionRows);
        }
        if (recentStakeTable != null) {
            recentStakeTable.setItems(FXCollections.observableArrayList(data.recentStakes));
        }
    }

    // Khởi động timer cập nhật thời gian còn lại của các phiên đang chạy.
    private void startLiveCountdown() {
        stopLiveCountdown();
        refreshLiveCountdown();

        liveCountdownTimeline = new Timeline(new KeyFrame(
                javafx.util.Duration.seconds(1),
                event -> refreshLiveCountdown()
        ));
        liveCountdownTimeline.setCycleCount(Timeline.INDEFINITE);
        liveCountdownTimeline.play();
    }

    // Cập nhật countdown từng dòng; phiên hết giờ được chuyển sang bảng đã kết thúc.
    private void refreshLiveCountdown() {
        if (liveSessionRows == null) {
            return;
        }

        List<LiveSessionRow> expiredRows = new ArrayList<>();
        for (LiveSessionRow row : liveSessionRows) {
            if (isExpired(row.getEndTime())) {
                expiredRows.add(row);
                continue;
            }
            row.setTimeLeft(formatRemaining(row.getEndTime()));
        }

        for (LiveSessionRow row : expiredRows) {
            liveSessionRows.remove(row);
            addEndedSession(row.toEndedSessionRow());
        }
        updateSessionSummaries();
    }

    // Kiểm tra thời gian kết thúc đã qua hay chưa.
    private boolean isExpired(LocalDateTime endTime) {
        return endTime == null || !endTime.isAfter(LocalDateTime.now());
    }

    // Thêm một phiên vào bảng đã kết thúc, tránh trùng và giới hạn số dòng hiển thị.
    private void addEndedSession(EndedSessionRow endedRow) {
        boolean exists = endedSessionRows.stream()
                .anyMatch(row -> row.getSessionId() == endedRow.getSessionId());
        if (!exists) {
            endedSessionRows.add(0, endedRow);
        }
        while (endedSessionRows.size() > 8) {
            endedSessionRows.remove(endedSessionRows.size() - 1);
        }
    }

    // Cập nhật các label tổng quan số phiên đang mở và phiên gần đây.
    private void updateSessionSummaries() {
        setText(liveSessionSummaryLabel, liveSessionRows.size() + " phien dang mo");
        setText(sidebarLiveLabel, String.valueOf(liveSessionRows.size()));
        setText(endedSessionSummaryLabel, endedSessionRows.size() + " phien gan day");
    }

    // Dừng timer countdown khi rời dashboard hoặc tải lại màn.
    private void stopLiveCountdown() {
        if (liveCountdownTimeline != null) {
            liveCountdownTimeline.stop();
            liveCountdownTimeline = null;
        }
    }

    // Set text an toàn cho Label có thể không tồn tại ở từng loại dashboard.
    private void setText(Label label, String text) {
        if (label != null) {
            label.setText(text == null ? "" : text);
        }
    }

    // Định dạng số tiền có dấu phẩy ngăn cách hàng nghìn.
    private String formatMoney(long amount) {
        return NumberFormat.getNumberInstance(Locale.US).format(amount);
    }

    // Định dạng thời gian còn lại của phiên thành HH:mm:ss hoặc mm:ss.
    private String formatRemaining(LocalDateTime endTime) {
        if (endTime == null) {
            return "-";
        }

        long seconds = java.time.Duration.between(LocalDateTime.now(), endTime).getSeconds();
        if (seconds <= 0) {
            return "00:00";
        }

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        }
        return String.format("%02d:%02d", minutes, secs);
    }

    public static class LiveSessionRow {
        private final long sessionId;
        private final String itemName;
        private final String currentPrice;
        private final String leader;
        private final LocalDateTime endTime;
        private final StringProperty timeLeft = new SimpleStringProperty("-");

        // Tạo dòng hiển thị cho bảng phiên đang chạy của Bidder.
        public LiveSessionRow(long sessionId, String itemName, String currentPrice, String leader, LocalDateTime endTime) {
            this.sessionId = sessionId;
            this.itemName = itemName;
            this.currentPrice = currentPrice;
            this.leader = leader;
            this.endTime = endTime;
        }

        // Trả về id phiên để TableView hoặc helper khác có thể đọc.
        public long getSessionId() {
            return sessionId;
        }

        // Trả về tên item hiển thị trong bảng.
        public String getItemName() {
            return itemName;
        }

        // Trả về giá hiện tại đã được format.
        public String getCurrentPrice() {
            return currentPrice;
        }

        // Trả về người đang dẫn phiên.
        public String getLeader() {
            return leader;
        }

        // Trả về thời gian còn lại hiện tại.
        public String getTimeLeft() {
            return timeLeft.get();
        }

        // Trả về property countdown để TableView tự cập nhật khi timer chạy.
        public StringProperty timeLeftProperty() {
            return timeLeft;
        }

        // Cập nhật thời gian còn lại cho dòng live session.
        public void setTimeLeft(String timeLeft) {
            this.timeLeft.set(timeLeft == null ? "" : timeLeft);
        }

        // Trả về thời điểm kết thúc phiên để timer kiểm tra hết giờ.
        public LocalDateTime getEndTime() {
            return endTime;
        }

        // Chuyển dòng live thành dòng ended khi countdown hết.
        public EndedSessionRow toEndedSessionRow() {
            return new EndedSessionRow(sessionId, itemName, currentPrice, leader, "ENDED");
        }
    }

    public static class EndedSessionRow {
        private final long sessionId;
        private final String itemName;
        private final String finalPrice;
        private final String winner;
        private final String status;

        // Tạo dòng hiển thị cho bảng phiên đã kết thúc.
        public EndedSessionRow(long sessionId, String itemName, String finalPrice, String winner, String status) {
            this.sessionId = sessionId;
            this.itemName = itemName;
            this.finalPrice = finalPrice;
            this.winner = winner;
            this.status = status;
        }

        // Trả về id phiên để tránh thêm trùng dòng ended.
        public long getSessionId() {
            return sessionId;
        }

        // Trả về tên item của phiên đã kết thúc.
        public String getItemName() {
            return itemName;
        }

        // Trả về giá cuối cùng của phiên.
        public String getFinalPrice() {
            return finalPrice;
        }

        // Trả về người thắng hoặc người đang dẫn cuối cùng.
        public String getWinner() {
            return winner;
        }

        // Trả về trạng thái cuối của phiên.
        public String getStatus() {
            return status;
        }
    }

    public static class StakeRow {
        private final Long auctionId;
        private final Long itemId;
        private final String amount;
        private final String status;

        // Tạo dòng hiển thị cho bảng lịch sử stake gần đây.
        public StakeRow(Long auctionId, Long itemId, String amount, String status) {
            this.auctionId = auctionId;
            this.itemId = itemId;
            this.amount = amount;
            this.status = status;
        }

        // Trả về id phiên liên quan tới stake.
        public Long getAuctionId() {
            return auctionId;
        }

        // Trả về id item bị khóa tiền stake.
        public Long getItemId() {
            return itemId;
        }

        // Trả về số tiền stake đã format.
        public String getAmount() {
            return amount;
        }

        // Trả về trạng thái stake.
        public String getStatus() {
            return status;
        }
    }

    public static class SessionOverviewRow {
        private final String itemName;
        private final String price;
        private final String leader;
        private final String status;

        // Tạo dòng tổng quan phiên dùng cho dashboard Admin/Seller.
        public SessionOverviewRow(String itemName, String price, String leader, String status) {
            this.itemName = itemName;
            this.price = price;
            this.leader = leader;
            this.status = status;
        }

        // Trả về tên item của phiên.
        public String getItemName() {
            return itemName;
        }

        // Trả về giá hiện tại/cuối cùng của phiên.
        public String getPrice() {
            return price;
        }

        // Trả về user đang dẫn phiên.
        public String getLeader() {
            return leader;
        }

        // Trả về trạng thái phiên.
        public String getStatus() {
            return status;
        }
    }

    public static class ItemOverviewRow {
        private final Long id;
        private final String itemName;
        private final String owner;
        private final String price;
        private final String status;

        // Tạo dòng tổng quan item dùng cho dashboard Admin/Seller.
        public ItemOverviewRow(Long id, String itemName, String owner, String price, String status) {
            this.id = id;
            this.itemName = itemName;
            this.owner = owner;
            this.price = price;
            this.status = status;
        }

        // Trả về id item.
        public Long getId() {
            return id;
        }

        // Trả về tên item.
        public String getItemName() {
            return itemName;
        }

        // Trả về chủ sở hữu item nếu màn cần hiển thị.
        public String getOwner() {
            return owner;
        }

        // Trả về giá khởi điểm đã format.
        public String getPrice() {
            return price;
        }

        // Trả về trạng thái item.
        public String getStatus() {
            return status;
        }
    }

    private static class AdminDashboardData {
        private final long activeSessions;
        private final long pendingItems;
        private final long totalItems;
        private final long totalUsers;
        private final List<SessionOverviewRow> sessionRows;
        private final List<ItemOverviewRow> pendingRows;

        // Gói dữ liệu dashboard Admin lấy từ background task.
        private AdminDashboardData(
                long activeSessions,
                long pendingItems,
                long totalItems,
                long totalUsers,
                List<SessionOverviewRow> sessionRows,
                List<ItemOverviewRow> pendingRows
        ) {
            this.activeSessions = activeSessions;
            this.pendingItems = pendingItems;
            this.totalItems = totalItems;
            this.totalUsers = totalUsers;
            this.sessionRows = sessionRows == null ? new ArrayList<>() : sessionRows;
            this.pendingRows = pendingRows == null ? new ArrayList<>() : pendingRows;
        }
    }

    private static class SellerDashboardData {
        private final long totalItems;
        private final long pendingItems;
        private final long inAuctionItems;
        private final long soldItems;
        private final List<ItemOverviewRow> itemRows;
        private final List<SessionOverviewRow> sessionRows;

        // Gói dữ liệu dashboard Seller lấy từ background task.
        private SellerDashboardData(
                long totalItems,
                long pendingItems,
                long inAuctionItems,
                long soldItems,
                List<ItemOverviewRow> itemRows,
                List<SessionOverviewRow> sessionRows
        ) {
            this.totalItems = totalItems;
            this.pendingItems = pendingItems;
            this.inAuctionItems = inAuctionItems;
            this.soldItems = soldItems;
            this.itemRows = itemRows == null ? new ArrayList<>() : itemRows;
            this.sessionRows = sessionRows == null ? new ArrayList<>() : sessionRows;
        }
    }

    private static class BidderDashboardData {
        private final long availableBalance;
        private final long lockedBalance;
        private final long lockedStakeCount;
        private final long leadingSessionCount;
        private final int liveSessionCount;
        private final int endedSessionCount;
        private final List<LiveSessionRow> liveSessions;
        private final List<EndedSessionRow> endedSessions;
        private final List<StakeRow> recentStakes;

        // Gói dữ liệu dashboard Bidder lấy từ background task.
        private BidderDashboardData(
                long availableBalance,
                long lockedBalance,
                long lockedStakeCount,
                long leadingSessionCount,
                int liveSessionCount,
                int endedSessionCount,
                List<LiveSessionRow> liveSessions,
                List<EndedSessionRow> endedSessions,
                List<StakeRow> recentStakes
        ) {
            this.availableBalance = availableBalance;
            this.lockedBalance = lockedBalance;
            this.lockedStakeCount = lockedStakeCount;
            this.leadingSessionCount = leadingSessionCount;
            this.liveSessionCount = liveSessionCount;
            this.endedSessionCount = endedSessionCount;
            this.liveSessions = liveSessions == null ? new ArrayList<>() : liveSessions;
            this.endedSessions = endedSessions == null ? new ArrayList<>() : endedSessions;
            this.recentStakes = recentStakes == null ? new ArrayList<>() : recentStakes;
        }
    }
}
