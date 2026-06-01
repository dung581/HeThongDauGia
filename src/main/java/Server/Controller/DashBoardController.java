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
import Server.Controller.model.DashboardModels.AdminDashboardData;
import Server.Controller.model.DashboardModels.BidderDashboardData;
import Server.Controller.model.DashboardModels.EndedSessionRow;
import Server.Controller.model.DashboardModels.ItemOverviewRow;
import Server.Controller.model.DashboardModels.LiveSessionRow;
import Server.Controller.model.DashboardModels.OwnedProductRow;
import Server.Controller.model.DashboardModels.SellerDashboardData;
import Server.Controller.model.DashboardModels.SessionOverviewRow;
import Server.service.AccountService;
import Server.service.AuctionService;
import Server.service.BidService;
import Server.service.ItemService;
import Server.service.StakeService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
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

    @FXML Label availableBalanceLabel;
    @FXML Label lockedBalanceLabel;
    @FXML Label activeStakeCountLabel;
    @FXML Label leadingSessionCountLabel;
    @FXML Label liveSessionSummaryLabel;
    @FXML Label sidebarUserLabel;
    @FXML Label sidebarRoleLabel;
    @FXML Label sidebarLiveLabel;
    @FXML Label endedSessionSummaryLabel;
    @FXML ListView<LiveSessionRow> liveSessionTable;
    @FXML ListView<EndedSessionRow> endedSessionTable;
    @FXML Label ownedProductSummaryLabel;
    @FXML VBox ownedProductList;
    @FXML Label adminActiveSessionsLabel;
    @FXML Label adminPendingItemsLabel;
    @FXML Label adminTotalItemsLabel;
    @FXML Label adminTotalUsersLabel;
    @FXML Label adminSidebarUserLabel;
    @FXML Label adminSidebarPendingLabel;
    @FXML Label adminSessionSummaryLabel;
    @FXML Label adminPendingSummaryLabel;
    @FXML VBox adminSessionList;
    @FXML ListView<ItemOverviewRow> adminPendingItemTable;
    @FXML Label sellerTotalItemsLabel;
    @FXML Label sellerPendingItemsLabel;
    @FXML Label sellerInAuctionItemsLabel;
    @FXML Label sellerSoldItemsLabel;
    @FXML Label sellerSidebarUserLabel;
    @FXML Label sellerSidebarRoleLabel;
    @FXML Label sellerSidebarItemsLabel;
    @FXML Label sellerItemSummaryLabel;
    @FXML Label sellerSessionSummaryLabel;
    @FXML ListView<ItemOverviewRow> sellerItemTable;
    @FXML ListView<SessionOverviewRow> sellerSessionTable;

    final AccountService accountService = new AccountService();
    final AuctionService auctionService = new AuctionService();
    final StakeService stakeService = new StakeService();
    final ItemService itemService = new ItemService();
    final BidService bidService = new BidService();
    Timeline liveCountdownTimeline;
    boolean bidderDashboardLoadRunning;
    long lastBidderSilentRefreshNanos;
    final ObservableList<LiveSessionRow> liveSessionRows = FXCollections.observableArrayList();
    final ObservableList<EndedSessionRow> endedSessionRows = FXCollections.observableArrayList();
    final DashboardNavigation navigation = new DashboardNavigation(this);
    final DashboardTableSection tableSection = new DashboardTableSection(this);
    final DashboardAdminSection adminSection = new DashboardAdminSection(this);
    final DashboardSellerSection sellerSection = new DashboardSellerSection(this);
    final DashboardBidderSection bidderSection = new DashboardBidderSection(this);
    final DashboardRowFactory rowFactory = new DashboardRowFactory(this);
    final DashboardDataMapper dataMapper = new DashboardDataMapper(this);
    final DashboardCountdownSection countdownSection = new DashboardCountdownSection(this);

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
    @FXML
    public void goHome(ActionEvent actionEvent) throws IOException {
        navigation.goHome(actionEvent);
    }

    // Đăng xuất: xóa session user và quay về màn đăng nhập.
    @FXML
    public void goToLogin(ActionEvent actionEvent) throws IOException {
        navigation.goToLogin(actionEvent);
    }

    // Mở màn danh sách phiên đấu giá.
    @FXML
    public void Sandaugia(ActionEvent actionEvent) throws IOException {
        navigation.switchScene(actionEvent, "/com/template/hellfx/danhSachDauGia.fxml");
    }

    // Mở màn sản phẩm của Seller; chặn Bidder vì Bidder không được đăng bán.
    @FXML
    public void dangban(ActionEvent actionEvent) throws IOException {
        navigation.goToSellerProducts(actionEvent);
    }

    // Mở màn tài khoản: Admin xem quản lý account, role khác xem tài khoản cá nhân.
    @FXML
    public void quanlytk(ActionEvent actionEvent) throws IOException {
        navigation.switchScene(actionEvent, "/com/template/hellfx/account.fxml");
    }

    // Mở màn nạp tiền.
    @FXML
    public void goToDeposit(ActionEvent actionEvent) throws IOException {
        navigation.switchScene(actionEvent, "/com/template/hellfx/Deposit.fxml");
    }

    // Mở màn duyệt riêng cho item PENDING của Admin.
    @FXML
    public void duyetsp(ActionEvent actionEvent) throws IOException {
        navigation.switchScene(actionEvent, "/com/template/hellfx/PendingItems.fxml");
    }

    // Mở màn quản lý toàn bộ vật phẩm cho Admin.
    @FXML
    public void quanlyvatpham(ActionEvent actionEvent) throws IOException {
        navigation.switchScene(actionEvent, "/com/template/hellfx/AdminItemManagement.fxml");
    }

    // Hiện popup cảnh báo cho thao tác không hợp lệ.
    void showWarning(String msg) {
        navigation.showWarning(msg);
    }

    // Load FXML mới, dừng timer dashboard nếu có và thay root scene hiện tại.
    void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        navigation.switchScene(actionEvent, fxmlPath);
    }

    // Cấu hình các danh sách riêng của dashboard Bidder.
    private void configureBidderTables() {
        tableSection.configureBidderTables();
    }

    // Cấu hình các danh sách riêng của dashboard Admin.
    private void configureAdminTables() {
        tableSection.configureAdminTables();
    }

    // Cấu hình các danh sách riêng của dashboard Seller.
    private void configureSellerTables() {
        tableSection.configureSellerTables();
    }

    // Tải số liệu dashboard Admin trên background thread để UI không bị lag.
    private void loadAdminDashboardAsync() {
        adminSection.loadDashboardAsync();
    }

    // Đổ dữ liệu đã tải lên các thẻ thống kê và bảng của Admin.
    void renderAdminDashboard(AdminDashboardData data) {
        adminSection.renderDashboard(data);
    }

    // Render phiên gần đây của Admin thành từng thanh mềm, có thể bấm để xem hoạt động phiên.
    void renderAdminSessionList(List<SessionOverviewRow> rows) {
        adminSection.renderSessionList(rows);
    }

    // Tạo một thanh phiên đấu giá gồm tên item, id phiên, giá, leader và trạng thái.
    Node createAdminSessionNode(SessionOverviewRow row) {
        return rowFactory.createAdminSessionNode(row);
    }

    // Mở màn chi tiết phiên từ thanh phiên trên dashboard Admin.
    void openSessionDetailFromNode(Node source, long sessionId) {
        navigation.openSessionDetailFromNode(source, sessionId);
    }

    // Tạo card phiên đang chạy cho dashboard Bidder.
    Node createLiveSessionCard(LiveSessionRow row) {
        return rowFactory.createLiveSessionCard(row);
    }

    // Tạo card phiên đã kết thúc cho dashboard Bidder.
    Node createEndedSessionCard(EndedSessionRow row) {
        return rowFactory.createEndedSessionCard(row);
    }

    // Tạo card item tổng quan dùng cho dashboard Admin/Seller.
    Node createItemOverviewCard(ItemOverviewRow row, boolean showOwner) {
        return rowFactory.createItemOverviewCard(row, showOwner);
    }

    // Tải số liệu dashboard Seller trên background thread.
    private void loadSellerDashboardAsync() {
        sellerSection.loadDashboardAsync();
    }

    // Đổ dữ liệu đã tải lên các thẻ thống kê và bảng của Seller.
    void renderSellerDashboard(SellerDashboardData data) {
        sellerSection.renderDashboard(data);
    }

    // Helper cho dashboard: phiên live là RUNNING và chưa quá end_time.
    boolean isLiveSession(Auction session) {
        return dataMapper.isLiveSession(session);
    }

    // Helper cho dashboard: phiên ended là không RUNNING hoặc đã quá end_time.
    boolean isEndedSession(Auction session) {
        return dataMapper.isEndedSession(session);
    }

    // Đổi trạng thái session thành text ngắn để render ở dashboard.
    String displaySessionStatus(Auction session) {
        return dataMapper.displaySessionStatus(session);
    }

    // Tạo row tổng quan phiên từ dữ liệu service trả về.
    SessionOverviewRow toSessionOverviewRow(Auction session, List<Item> items) {
        return dataMapper.toSessionOverviewRow(session, items);
    }

    // Tạo row tổng quan item cho ListView dashboard.
    ItemOverviewRow toItemOverviewRow(Item item, boolean includeOwner) {
        return dataMapper.toItemOverviewRow(item, includeOwner);
    }

    // Tạo row phiên live của Bidder; nếu user đang dẫn thì hiển thị "Ban".
    LiveSessionRow toLiveSessionRow(
            Auction session,
            long userId,
            List<Item> items,
            List<AccountService.ManagedAccount> accounts
    ) {
        return dataMapper.toLiveSessionRow(session, userId, items, accounts);
    }

    // Tạo row phiên ended của Bidder; nếu user thắng/dẫn cuối thì hiển thị "Ban".
    EndedSessionRow toEndedSessionRow(Auction session, long userId, List<Item> items) {
        return dataMapper.toEndedSessionRow(session, userId, items);
    }

    // Tạo row sản phẩm Bidder đã thắng để render ở khu owned products.
    OwnedProductRow toOwnedProductRow(Auction session, List<Item> items) {
        return dataMapper.toOwnedProductRow(session, items);
    }

    // Tìm tên item theo itemId trong danh sách đã load sẵn.
    String findItemName(List<Item> items, long itemId) {
        return dataMapper.findItemName(items, itemId);
    }

    // Tìm mô tả item theo itemId trong danh sách đã load sẵn.
    String findItemDescription(List<Item> items, long itemId) {
        return dataMapper.findItemDescription(items, itemId);
    }

    // Tìm bước giá tối thiểu, fallback 1 cho dữ liệu cũ chưa có minIncrement.
    String findItemMinIncrement(List<Item> items, long itemId) {
        return dataMapper.findItemMinIncrement(items, itemId);
    }

    // Chuẩn hóa minIncrement cho dữ liệu cũ.
    long getEffectiveMinIncrement(Item item) {
        return dataMapper.getEffectiveMinIncrement(item);
    }

    // Tìm item theo id trong list đã có để không query lặp.
    Item findItem(List<Item> items, long itemId) {
        return dataMapper.findItem(items, itemId);
    }

    // Tải dữ liệu dashboard Bidder: số dư, phiên đang chạy, phiên đã kết thúc và sản phẩm đã thắng.
    private void loadBidderDashboardAsync() {
        bidderSection.loadDashboardAsync();
    }

    // Refresh ngầm dashboard Bidder để giá/tổng bid đổi khi auto bid chạy nền.
    void refreshBidderDashboardSilently() {
        bidderSection.refreshDashboardSilently();
    }

    // Đổ dữ liệu dashboard Bidder lên các thẻ thống kê và bảng.
    void renderBidderDashboard(BidderDashboardData data) {
        bidderSection.renderDashboard(data);
    }

    // Render danh sách sản phẩm đã thắng theo dạng từng thanh sản phẩm, không dùng bảng cứng.
    void renderOwnedProducts(List<OwnedProductRow> products) {
        bidderSection.renderOwnedProducts(products);
    }

    // Tạo một thanh sản phẩm gồm tên, mô tả, id phiên/item, giá thắng và trạng thái.
    Node createOwnedProductNode(OwnedProductRow row) {
        return rowFactory.createOwnedProductNode(row);
    }

    // Mở màn kết quả của phiên khi bấm vào sản phẩm đã thắng.
    void openWinnerFromNode(Node source, long sessionId) {
        navigation.openWinnerFromNode(source, sessionId);
    }

    // Chuyển màn từ một Node bất kỳ, dùng cho các card/list item không phát ActionEvent.
    void switchSceneFromNode(Node source, String fxmlPath) throws IOException {
        navigation.switchSceneFromNode(source, fxmlPath);
    }

    // Khởi động timer cập nhật thời gian còn lại của các phiên đang chạy.
    void startLiveCountdown() {
        countdownSection.startLiveCountdown();
    }

    // Cập nhật countdown từng dòng; phiên hết giờ được chuyển sang bảng đã kết thúc.
    void refreshLiveCountdown() {
        countdownSection.refreshLiveCountdown();
    }

    // Kiểm tra thời gian kết thúc đã qua hay chưa.
    boolean isExpired(LocalDateTime endTime) {
        return countdownSection.isExpired(endTime);
    }

    // Thêm một phiên vào bảng đã kết thúc, tránh trùng và giới hạn số dòng hiển thị.
    void addEndedSession(EndedSessionRow endedRow) {
        countdownSection.addEndedSession(endedRow);
    }

    // Cập nhật các label tổng quan số phiên đang mở và phiên gần đây.
    void updateSessionSummaries() {
        countdownSection.updateSessionSummaries();
    }

    // Dừng timer countdown khi rời dashboard hoặc tải lại màn.
    void stopLiveCountdown() {
        countdownSection.stopLiveCountdown();
    }

    // Set text an toàn cho Label có thể không tồn tại ở từng loại dashboard.
    void setText(Label label, String text) {
        if (label != null) {
            label.setText(text == null ? "" : text);
        }
    }

    // Trả chuỗi dự phòng khi dữ liệu null/rỗng.
    String nullToText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    // Định dạng số tiền có dấu phẩy ngăn cách hàng nghìn.
    String formatMoney(long amount) {
        return NumberFormat.getNumberInstance(Locale.US).format(amount);
    }

    // Định dạng thời gian còn lại của phiên thành HH:mm:ss hoặc mm:ss.
    String formatRemaining(LocalDateTime endTime) {
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

}

// Nhóm điều hướng của dashboard: đổi màn, mở chi tiết phiên/kết quả và hiển thị cảnh báo.
final class DashboardNavigation {
    private final DashBoardController controller;

    DashboardNavigation(DashBoardController controller) {
        this.controller = controller;
    }

    // Điều hướng về dashboard đúng với role hiện tại.
    void goHome(ActionEvent actionEvent) throws IOException {
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
    void goToLogin(ActionEvent actionEvent) throws IOException {
        UserAccount.clearSession();
        switchScene(actionEvent, "/com/template/hellfx/UILogin.fxml");
    }

    // Mở màn sản phẩm của Seller; chặn Bidder vì Bidder không được đăng bán.
    void goToSellerProducts(ActionEvent actionEvent) throws IOException {
        UserRole role = UserAccount.getCurrentRole();
        if (role == UserRole.BIDDER) {
            showWarning("Bidder chỉ được đấu giá, không được đăng bán.");
            return;
        }
        switchScene(actionEvent, "/com/template/hellfx/SellerProducts.fxml");
    }

    // Hiện popup cảnh báo cho thao tác không hợp lệ.
    void showWarning(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // Load FXML mới, dừng timer dashboard nếu có và thay root scene hiện tại.
    void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        URL resource = controller.getClass().getResource(fxmlPath);
        if (resource == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi điều hướng");
            alert.setHeaderText("Không tìm thấy màn hình");
            alert.setContentText(fxmlPath);
            alert.showAndWait();
            return;
        }

        controller.stopLiveCountdown();
        Parent root = FXMLLoader.load(resource);
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        replaceSceneRoot(stage, root);
    }

    // Mở màn chi tiết phiên từ một card/list item bất kỳ.
    void openSessionDetailFromNode(Node source, long sessionId) {
        if (sessionId <= 0) {
            showWarning("Phiên không hợp lệ.");
            return;
        }

        try {
            SessionDetailController.setSessionId(sessionId);
            switchSceneFromNode(source, "/com/template/hellfx/session-detail.fxml");
        } catch (IOException e) {
            showWarning("Không mở được hoạt động phiên: " + e.getMessage());
        }
    }

    // Mở màn kết quả của phiên khi bấm vào sản phẩm đã thắng.
    void openWinnerFromNode(Node source, long sessionId) {
        try {
            WinnerController.setSessionId(sessionId);
            switchSceneFromNode(source, "/com/template/hellfx/Winner.fxml");
        } catch (IOException e) {
            showWarning("Không mở được kết quả phiên: " + e.getMessage());
        }
    }

    // Chuyển màn từ một Node bất kỳ, dùng cho các card/list item không phát ActionEvent.
    void switchSceneFromNode(Node source, String fxmlPath) throws IOException {
        URL resource = controller.getClass().getResource(fxmlPath);
        if (resource == null) {
            showWarning("Không tìm thấy màn hình: " + fxmlPath);
            return;
        }

        controller.stopLiveCountdown();
        Parent root = FXMLLoader.load(resource);
        Stage stage = (Stage) source.getScene().getWindow();
        replaceSceneRoot(stage, root);
    }

    // Thay root scene hiện tại và giữ kích thước cửa sổ thống nhất.
    private void replaceSceneRoot(Stage stage, Parent root) {
        Scene currentScene = stage.getScene();
        if (currentScene == null) {
            stage.setScene(new Scene(root, UILogin.APP_WIDTH, UILogin.APP_HEIGHT));
        } else {
            currentScene.setRoot(root);
        }
        stage.show();
    }
}

// Nhóm cấu hình ListView cho dashboard Bidder/Admin/Seller.
final class DashboardTableSection {
    private final DashBoardController controller;

    DashboardTableSection(DashBoardController controller) {
        this.controller = controller;
    }

    // Cấu hình các danh sách riêng của dashboard Bidder.
    void configureBidderTables() {
        if (controller.liveSessionTable != null) {
            controller.liveSessionTable.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(LiveSessionRow row, boolean empty) {
                    super.updateItem(row, empty);
                    setText(null);
                    setGraphic(empty || row == null ? null : controller.createLiveSessionCard(row));
                }
            });
        }

        if (controller.endedSessionTable != null) {
            controller.endedSessionTable.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(EndedSessionRow row, boolean empty) {
                    super.updateItem(row, empty);
                    setText(null);
                    setGraphic(empty || row == null ? null : controller.createEndedSessionCard(row));
                }
            });
        }
    }

    // Cấu hình danh sách item đang chờ duyệt của Admin.
    void configureAdminTables() {
        if (controller.adminPendingItemTable != null) {
            controller.adminPendingItemTable.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(ItemOverviewRow row, boolean empty) {
                    super.updateItem(row, empty);
                    setText(null);
                    setGraphic(empty || row == null ? null : controller.createItemOverviewCard(row, true));
                }
            });
        }
    }

    // Cấu hình danh sách item và phiên liên quan của Seller.
    void configureSellerTables() {
        if (controller.sellerItemTable != null) {
            controller.sellerItemTable.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(ItemOverviewRow row, boolean empty) {
                    super.updateItem(row, empty);
                    setText(null);
                    setGraphic(empty || row == null ? null : controller.createItemOverviewCard(row, false));
                }
            });
        }

        if (controller.sellerSessionTable != null) {
            controller.sellerSessionTable.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(SessionOverviewRow row, boolean empty) {
                    super.updateItem(row, empty);
                    setText(null);
                    setGraphic(empty || row == null ? null : controller.createAdminSessionNode(row));
                }
            });
        }
    }
}

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

// Nhóm dựng Node/Card dùng chung cho các bảng dashboard.
final class DashboardRowFactory {
    private final DashBoardController controller;

    DashboardRowFactory(DashBoardController controller) {
        this.controller = controller;
    }

    // Tạo một thanh phiên đấu giá gồm tên item, id phiên, giá, leader và trạng thái.
    Node createAdminSessionNode(SessionOverviewRow row) {
        boolean live = isLiveLikeStatus(row.getStatus());
        return createMarketCard(
                row.getItemName(),
                controller.nullToText(row.getDescription(), "Không có mô tả"),
                "Phiên #" + row.getSessionId(),
                "Sản phẩm #" + row.getItemId(),
                statusPillText(row.getStatus()),
                "CURRENT BID",
                row.getPrice(),
                "LEADER",
                row.getLeader(),
                "STEP",
                row.getMinIncrement(),
                live ? "Join ->" : "Xem ->",
                source -> controller.openSessionDetailFromNode(source, row.getSessionId()),
                live
        );
    }

    // Tạo card phiên đang chạy cho dashboard Bidder.
    Node createLiveSessionCard(LiveSessionRow row) {
        HBox card = new HBox(20.0);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(18.0, 24.0, 18.0, 24.0));
        card.getStyleClass().add("live-auction-card");
        card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(event -> {
            // Nếu click xuất phát từ nút Join thì để nút tự xử lý, tránh mở chi tiết phiên 2 lần liên tiếp.
            if (!isButtonTarget(event.getTarget())) {
                controller.openSessionDetailFromNode(card, row.getSessionId());
            }
        });

        VBox main = new VBox(10.0);
        main.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(main, Priority.ALWAYS);

        Label title = new Label(row.getItemName());
        title.getStyleClass().add("live-auction-title");
        title.setWrapText(true);

        HBox meta = new HBox(16.0);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label seller = new Label("by " + row.getSeller());
        seller.getStyleClass().add("live-auction-meta");
        Label separator = new Label("|");
        separator.getStyleClass().add("live-auction-meta");
        Label session = new Label("Session #" + row.getSessionId());
        session.getStyleClass().add("live-auction-meta");
        meta.getChildren().addAll(seller, separator, session);

        HBox stats = new HBox(58.0);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(
                createLiveStat("LIVE", "START PRICE", row.getStartPrice(), "live-auction-start-price"),
                createLiveStat(null, "CURRENT BID", row.getCurrentPrice(), "live-auction-current-price"),
                createLiveStat(null, "TOTAL BIDS", String.valueOf(row.getTotalBids()), "live-auction-total-bids")
        );

        main.getChildren().addAll(title, meta, stats);

        VBox actionBox = new VBox(26.0);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        Button joinButton = new Button("Join ->");
        joinButton.getStyleClass().add("live-auction-join-button");
        joinButton.setOnAction(event -> {
            controller.openSessionDetailFromNode(joinButton, row.getSessionId());
            event.consume();
        });

        VBox timerBox = new VBox(2.0);
        timerBox.setAlignment(Pos.CENTER);
        timerBox.getStyleClass().add("live-auction-timer-box");
        Label timerTitle = new Label("ENDS IN");
        timerTitle.getStyleClass().add("live-auction-stat-label");
        Label time = new Label(row.getTimeLeft());
        time.getStyleClass().add("live-auction-timer");
        timerBox.getChildren().addAll(timerTitle, time);
        actionBox.getChildren().addAll(joinButton, timerBox);

        card.getChildren().addAll(main, actionBox);
        return card;
    }

    // Kiểm tra target click có nằm trong Button hay không, vì target đôi khi là text con của Button.
    private boolean isButtonTarget(Object target) {
        if (!(target instanceof Node node)) {
            return false;
        }
        while (node != null) {
            if (node instanceof Button) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    // Tạo một cụm chỉ số trong card phiên đang chạy: nhãn nhỏ ở trên, giá trị lớn ở dưới.
    private VBox createLiveStat(String pillText, String labelText, String valueText, String valueStyleClass) {
        VBox box = new VBox(4.0);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMinWidth(145.0);

        if (pillText != null && !pillText.isBlank()) {
            Label pill = new Label(pillText);
            pill.getStyleClass().add("live-auction-live-pill");
            box.getChildren().add(pill);
        }

        Label label = new Label(labelText);
        label.getStyleClass().add("live-auction-stat-label");
        Label value = new Label(valueText);
        value.getStyleClass().add(valueStyleClass);
        box.getChildren().addAll(label, value);
        return box;
    }

    // Tạo card phiên đã kết thúc cho dashboard Bidder.
    Node createEndedSessionCard(EndedSessionRow row) {
        return createMarketCard(
                row.getItemName(),
                controller.nullToText(row.getDescription(), "Không có mô tả"),
                "Winner " + row.getWinner(),
                "Phiên #" + row.getSessionId(),
                statusPillText(row.getStatus()),
                "FINAL PRICE",
                row.getFinalPrice(),
                "WINNER",
                row.getWinner(),
                "STEP",
                row.getMinIncrement(),
                "Xem ->",
                source -> controller.openWinnerFromNode(source, row.getSessionId()),
                false
        );
    }

    // Tạo card item tổng quan dùng cho dashboard Admin/Seller.
    Node createItemOverviewCard(ItemOverviewRow row, boolean showOwner) {
        String ownerMeta = showOwner && row.getOwner() != null && !row.getOwner().isBlank()
                ? "Seller #" + row.getOwner()
                : "Sản phẩm #" + row.getId();
        return createMarketCard(
                row.getItemName(),
                controller.nullToText(row.getDescription(), "Không có mô tả"),
                ownerMeta,
                "Sản phẩm #" + row.getId(),
                statusPillText(row.getStatus()),
                "START PRICE",
                row.getPrice(),
                "STEP",
                row.getMinIncrement(),
                "ITEM ID",
                "#" + row.getId(),
                null,
                null,
                isLiveLikeStatus(row.getStatus())
        );
    }

    // Tạo một thanh sản phẩm gồm tên, mô tả, id phiên/item, giá thắng và trạng thái.
    Node createOwnedProductNode(OwnedProductRow row) {
        return createMarketCard(
                row.getItemName(),
                controller.nullToText(row.getDescription(), "Không có mô tả"),
                "Phiên #" + row.getSessionId(),
                "Sản phẩm #" + row.getItemId(),
                statusPillText(row.getStatus()),
                "FINAL PRICE",
                row.getFinalPrice(),
                "SESSION",
                "#" + row.getSessionId(),
                "ITEM",
                "#" + row.getItemId(),
                "Xem ->",
                source -> controller.openWinnerFromNode(source, row.getSessionId()),
                false
        );
    }

    // Tạo card compact dùng chung cho item/phiên phụ: giống mẫu auction card nhưng không thay đổi nghiệp vụ click.
    private Node createMarketCard(
            String titleText,
            String descriptionText,
            String metaLeftText,
            String metaRightText,
            String pillText,
            String firstLabel,
            String firstValue,
            String secondLabel,
            String secondValue,
            String thirdLabel,
            String thirdValue,
            String actionText,
            java.util.function.Consumer<Node> action,
            boolean live
    ) {
        VBox card = new VBox(10.0);
        card.setPadding(new Insets(18.0, 24.0, 18.0, 24.0));
        card.getStyleClass().addAll("market-card", live ? "live-auction-card" : "auction-card-muted");
        card.setCursor(action == null ? Cursor.DEFAULT : Cursor.HAND);
        if (action != null) {
            card.setOnMouseClicked(event -> {
                // Card vẫn click được, nhưng click lên nút con không được bắn thêm event mở màn lần hai.
                if (!isButtonTarget(event.getTarget())) {
                    action.accept(card);
                }
            });
        }

        HBox header = new HBox(14.0);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(controller.nullToText(titleText, "Item"));
        title.getStyleClass().add("live-auction-title");
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);
        header.getChildren().add(title);

        if (action != null && actionText != null && !actionText.isBlank()) {
            Button actionButton = new Button(actionText);
            actionButton.getStyleClass().add("live-auction-join-button");
            actionButton.setOnAction(event -> {
                action.accept(actionButton);
                event.consume();
            });
            header.getChildren().add(actionButton);
        }

        Label description = new Label(controller.nullToText(descriptionText, "Không có mô tả"));
        description.getStyleClass().add("product-description");
        description.setWrapText(true);

        HBox meta = new HBox(14.0);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label left = new Label(controller.nullToText(metaLeftText, ""));
        left.getStyleClass().add("live-auction-meta");
        meta.getChildren().add(left);
        if (metaRightText != null && !metaRightText.isBlank()) {
            Label separator = new Label("|");
            separator.getStyleClass().add("live-auction-meta");
            Label right = new Label(metaRightText);
            right.getStyleClass().add("live-auction-meta");
            meta.getChildren().addAll(separator, right);
        }

        HBox stats = new HBox(24.0);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(
                createMarketStat(pillText, firstLabel, firstValue, "live-auction-start-price"),
                createMarketStat(null, secondLabel, secondValue, "live-auction-current-price"),
                createMarketStat(null, thirdLabel, thirdValue, "live-auction-total-bids")
        );

        card.getChildren().addAll(header, meta, description, stats);
        return card;
    }

    // Cụm số liệu compact cho các card phụ, nhỏ hơn card phiên live để không tràn ở cột phải.
    private VBox createMarketStat(String pillText, String labelText, String valueText, String valueStyleClass) {
        VBox box = new VBox(4.0);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMinWidth(92.0);

        if (pillText != null && !pillText.isBlank()) {
            Label pill = new Label(pillText);
            pill.getStyleClass().add("live-auction-live-pill");
            box.getChildren().add(pill);
        }

        Label label = new Label(controller.nullToText(labelText, ""));
        label.getStyleClass().add("live-auction-stat-label");
        Label value = new Label(controller.nullToText(valueText, "-"));
        value.getStyleClass().add(valueStyleClass);
        value.setWrapText(true);
        box.getChildren().addAll(label, value);
        return box;
    }

    // Các trạng thái này đại diện cho card đang diễn ra nên dùng viền xanh như mẫu.
    private boolean isLiveLikeStatus(String status) {
        if (status == null) {
            return false;
        }
        return status.equalsIgnoreCase("RUNNING") || status.equalsIgnoreCase("IN_AUCTION");
    }

    // Pill trên card: phiên/item đang chạy hiện LIVE, trạng thái còn lại giữ nguyên để không mất thông tin.
    private String statusPillText(String status) {
        return isLiveLikeStatus(status) ? "LIVE" : controller.nullToText(status, "STATUS");
    }
}

// Nhóm chuyển entity sang model hiển thị của dashboard.
final class DashboardDataMapper {
    private final DashBoardController controller;

    DashboardDataMapper(DashBoardController controller) {
        this.controller = controller;
    }

    // Helper cho dashboard: phiên live là RUNNING và chưa quá end_time.
    boolean isLiveSession(Auction session) {
        return session.getState() == AuctionState.RUNNING
                && session.getEndTime() != null
                && session.getEndTime().isAfter(LocalDateTime.now());
    }

    // Helper cho dashboard: phiên ended là không RUNNING hoặc đã quá end_time.
    boolean isEndedSession(Auction session) {
        return session.getState() != AuctionState.RUNNING
                || session.getEndTime() == null
                || !session.getEndTime().isAfter(LocalDateTime.now());
    }

    // Đổi trạng thái session thành text ngắn để render ở dashboard.
    String displaySessionStatus(Auction session) {
        if (session.getState() == AuctionState.RUNNING && !isLiveSession(session)) {
            return "ENDED";
        }
        return session.getState() == null ? "" : session.getState().name();
    }

    // Tạo row tổng quan phiên từ dữ liệu service trả về.
    SessionOverviewRow toSessionOverviewRow(Auction session, List<Item> items) {
        String leader = session.getCurrent_user_id() == 0 ? "-" : String.valueOf(session.getCurrent_user_id());
        return new SessionOverviewRow(
                session.getId(),
                session.getItem_id(),
                findItemName(items, session.getItem_id()),
                findItemDescription(items, session.getItem_id()),
                controller.formatMoney(session.getCurrent_price()),
                findItemMinIncrement(items, session.getItem_id()),
                leader,
                displaySessionStatus(session)
        );
    }

    // Tạo row tổng quan item cho ListView dashboard.
    ItemOverviewRow toItemOverviewRow(Item item, boolean includeOwner) {
        return new ItemOverviewRow(
                item.getId(),
                item.getFullname(),
                controller.nullToText(item.getDescription(), "Không có mô tả"),
                includeOwner ? String.valueOf(item.getOwner_user_id()) : "",
                controller.formatMoney(item.getBeginPrice()),
                controller.formatMoney(getEffectiveMinIncrement(item)),
                item.getStatus() == null ? "" : item.getStatus().name()
        );
    }

    // Tạo row phiên live của Bidder; nếu user đang dẫn thì hiển thị "Ban".
    LiveSessionRow toLiveSessionRow(
            Auction session,
            long userId,
            List<Item> items,
            List<AccountService.ManagedAccount> accounts
    ) {
        String leader = session.getCurrent_user_id() == 0
                ? "-"
                : session.getCurrent_user_id() == userId ? "Ban" : String.valueOf(session.getCurrent_user_id());
        Item item = findItem(items, session.getItem_id());
        int totalBids = controller.bidService.getHistoryBySession(session.getId()).size();
        return new LiveSessionRow(
                session.getId(),
                findItemName(items, session.getItem_id()),
                findItemDescription(items, session.getItem_id()),
                findSellerDisplay(item, accounts),
                item == null ? "0" : controller.formatMoney(item.getBeginPrice()),
                controller.formatMoney(session.getCurrent_price()),
                findItemMinIncrement(items, session.getItem_id()),
                leader,
                totalBids,
                session.getEndTime()
        );
    }

    // Tạo row phiên ended của Bidder; nếu user thắng/dẫn cuối thì hiển thị "Ban".
    EndedSessionRow toEndedSessionRow(Auction session, long userId, List<Item> items) {
        String winner = session.getCurrent_user_id() == 0
                ? "-"
                : session.getCurrent_user_id() == userId ? "Ban" : String.valueOf(session.getCurrent_user_id());
        String status = session.getState() == AuctionState.RUNNING ? "ENDED" : session.getState().name();
        return new EndedSessionRow(
                session.getId(),
                findItemName(items, session.getItem_id()),
                findItemDescription(items, session.getItem_id()),
                controller.formatMoney(session.getCurrent_price()),
                findItemMinIncrement(items, session.getItem_id()),
                winner,
                status
        );
    }

    // Tạo row sản phẩm Bidder đã thắng để render ở khu owned products.
    OwnedProductRow toOwnedProductRow(Auction session, List<Item> items) {
        Item item = findItem(items, session.getItem_id());
        String itemName = item == null || item.getFullname() == null || item.getFullname().isBlank()
                ? "Item " + session.getItem_id()
                : item.getFullname();
        String description = item == null || item.getDescription() == null || item.getDescription().isBlank()
                ? "San pham da thang tu phien #" + session.getId()
                : item.getDescription();
        String status = session.getState() == AuctionState.PAID ? "DA THANG" : "WON";
        return new OwnedProductRow(
                session.getItem_id(),
                session.getId(),
                itemName,
                description,
                controller.formatMoney(session.getCurrent_price()),
                status
        );
    }

    // Tìm tên item theo itemId trong danh sách đã load sẵn.
    String findItemName(List<Item> items, long itemId) {
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

    // Tìm mô tả item theo itemId trong danh sách đã load sẵn.
    String findItemDescription(List<Item> items, long itemId) {
        if (items == null) {
            return "Không có mô tả";
        }
        return items.stream()
                .filter(item -> item.getId() == itemId)
                .map(Item::getDescription)
                .filter(description -> description != null && !description.isBlank())
                .findFirst()
                .orElse("Không có mô tả");
    }

    // Tìm bước giá tối thiểu, fallback 1 cho dữ liệu cũ chưa có minIncrement.
    String findItemMinIncrement(List<Item> items, long itemId) {
        return controller.formatMoney(getEffectiveMinIncrement(findItem(items, itemId)));
    }

    // Chuẩn hóa minIncrement cho dữ liệu cũ.
    long getEffectiveMinIncrement(Item item) {
        return item == null || item.getMinIncrement() <= 0 ? 1L : item.getMinIncrement();
    }

    // Tìm item theo id trong list đã có để không query lặp.
    Item findItem(List<Item> items, long itemId) {
        if (items == null) {
            return null;
        }
        return items.stream()
                .filter(item -> item.getId() == itemId)
                .findFirst()
                .orElse(null);
    }

    // Hiển thị username seller trong card phiên live; fallback về owner id nếu chưa tìm được account.
    String findSellerDisplay(Item item, List<AccountService.ManagedAccount> accounts) {
        if (item == null || item.getOwner_user_id() <= 0) {
            return "@seller";
        }
        if (accounts != null) {
            for (AccountService.ManagedAccount account : accounts) {
                if (account.getUserId() != null
                        && account.getUserId() == item.getOwner_user_id()
                        && account.getUsername() != null
                        && !account.getUsername().isBlank()) {
                    return "@" + account.getUsername();
                }
            }
        }
        return "@seller_" + item.getOwner_user_id();
    }
}

// Nhóm countdown cho các phiên live trong dashboard Bidder.
final class DashboardCountdownSection {
    private final DashBoardController controller;

    DashboardCountdownSection(DashBoardController controller) {
        this.controller = controller;
    }

    // Khởi động timer cập nhật thời gian còn lại của các phiên đang chạy.
    void startLiveCountdown() {
        stopLiveCountdown();
        controller.lastBidderSilentRefreshNanos = System.nanoTime();
        refreshLiveCountdown();

        controller.liveCountdownTimeline = new Timeline(new KeyFrame(
                javafx.util.Duration.seconds(1),
                event -> refreshLiveCountdown()
        ));
        controller.liveCountdownTimeline.setCycleCount(Timeline.INDEFINITE);
        controller.liveCountdownTimeline.play();
    }

    // Cập nhật countdown từng dòng; phiên hết giờ được chuyển sang bảng đã kết thúc.
    void refreshLiveCountdown() {
        if (controller.liveSessionRows == null) {
            return;
        }

        List<LiveSessionRow> expiredRows = new ArrayList<>();
        for (LiveSessionRow row : controller.liveSessionRows) {
            if (isExpired(row.getEndTime())) {
                expiredRows.add(row);
                continue;
            }
            row.setTimeLeft(controller.formatRemaining(row.getEndTime()));
        }

        for (LiveSessionRow row : expiredRows) {
            controller.liveSessionRows.remove(row);
            addEndedSession(row.toEndedSessionRow());
        }
        if (controller.liveSessionTable != null) {
            controller.liveSessionTable.refresh();
        }
        refreshBidderDashboardAfterAutoBidTick();
        updateSessionSummaries();
    }

    // Dashboard chỉ render dữ liệu có sẵn; refresh ngầm định kỳ để thấy bid mới do runner auto bid tạo ra.
    private void refreshBidderDashboardAfterAutoBidTick() {
        if (controller.liveSessionRows.isEmpty()) {
            return;
        }
        long now = System.nanoTime();
        if (now - controller.lastBidderSilentRefreshNanos < 2_000_000_000L) {
            return;
        }
        controller.lastBidderSilentRefreshNanos = now;
        controller.refreshBidderDashboardSilently();
    }

    // Kiểm tra thời gian kết thúc đã qua hay chưa.
    boolean isExpired(LocalDateTime endTime) {
        return endTime == null || !endTime.isAfter(LocalDateTime.now());
    }

    // Thêm một phiên vào bảng đã kết thúc, tránh trùng và giới hạn số dòng hiển thị.
    void addEndedSession(EndedSessionRow endedRow) {
        boolean exists = controller.endedSessionRows.stream()
                .anyMatch(row -> row.getSessionId() == endedRow.getSessionId());
        if (!exists) {
            controller.endedSessionRows.add(0, endedRow);
        }
        while (controller.endedSessionRows.size() > 8) {
            controller.endedSessionRows.remove(controller.endedSessionRows.size() - 1);
        }
    }

    // Cập nhật các label tổng quan số phiên đang mở và phiên gần đây.
    void updateSessionSummaries() {
        controller.setText(controller.liveSessionSummaryLabel, controller.liveSessionRows.size() + " phien dang mo");
        controller.setText(controller.sidebarLiveLabel, String.valueOf(controller.liveSessionRows.size()));
        controller.setText(controller.endedSessionSummaryLabel, controller.endedSessionRows.size() + " phien gan day");
    }

    // Dừng timer countdown khi rời dashboard hoặc tải lại màn.
    void stopLiveCountdown() {
        if (controller.liveCountdownTimeline != null) {
            controller.liveCountdownTimeline.stop();
            controller.liveCountdownTimeline = null;
        }
    }
}
