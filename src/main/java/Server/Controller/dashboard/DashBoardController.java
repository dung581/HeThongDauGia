package Server.Controller.dashboard;

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
    @FXML Label topbarBalanceLabel;
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
