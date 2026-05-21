package Server.Controller;

import Client.Controller.UILogin;

import Client.util.AlertUtil;
import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Autobid;
import Common.DataBase.entities.Bid;
import Common.DataBase.entities.Item;
import Common.Enum.AuctionState;
import Common.Enum.UserRole;
import Common.Model.user.UserAccount;
import Server.service.AccountService;
import Server.service.AuctionService;
import Server.service.AutoBidService;
import Server.service.BidService;
import Server.service.ItemService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class SessionDetailController {

    // Các label hiển thị thông tin chính của phiên đấu giá trên màn session-detail.fxml.
    @FXML private Label itemNameLabel;
    @FXML private Label itemDescriptionLabel;
    @FXML private Label sessionIdLabel;
    @FXML private Label itemIdLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label countdownLabel;
    @FXML private Label balanceLabel;
    @FXML private Label winnerLabel;
    @FXML private Label autoBidStatusLabel;
    @FXML private Label accountSectionLabel;
    @FXML private Label bidPanelMessageLabel;
    @FXML private Node browseItemsNav;
    @FXML private Node depositNav;
    @FXML private Node placeBidBox;
    @FXML private Node autoBidBox;
    @FXML private Button accountNavButton;

    // Ô nhập giá đặt thủ công và giá tối đa cho auto bid.
    @FXML private TextField bidAmountField;
    @FXML private TextField autoMaxField;

    // Các nút thao tác của người dùng trong màn chi tiết phiên.
    @FXML private Button placeBidBtn;
    @FXML private Button activateAutobidBtn;
    @FXML private Button deactivateAutobidBtn;

    // Bảng lịch sử đặt giá của item trong phiên hiện tại.
    @FXML private TableView<Bid> bidTable;
    @FXML private TableColumn<Bid, Long> colBidId;
    @FXML private TableColumn<Bid, Long> colBidUserId;
    @FXML private TableColumn<Bid, Long> colBidPrice;
    @FXML private TableColumn<Bid, String> colBidTime;

    // Lưu sessionId tạm thời trước khi FXMLLoader tạo controller mới.
    private static long pendingSessionId;

    // Controller chỉ gọi service, không gọi repository trực tiếp.
    private final AuctionService auctionService = new AuctionService();
    private final BidService bidService = new BidService();
    private final AccountService accountService = new AccountService();
    private final AutoBidService autoBidService = new AutoBidService();
    private final ItemService itemService = new ItemService();

    private Auction session;
    private Item item;
    private Timeline countdownTimer;
    private long loadRequestToken;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Nhận sessionId từ màn danh sách phiên trước khi load session-detail.fxml.
    public static void setSessionId(long sessionId) {
        pendingSessionId = sessionId;
    }

    // JavaFX tự gọi sau khi load FXML: cấu hình UI, bảng bid và tải phiên được chọn.
    @FXML
    public void initialize() {
        configureRoleUi();
        configureBidTable();

        if (pendingSessionId <= 0) {
            disableActions(true);
            AlertUtil.showError("Chưa chọn phiên đấu giá.");
            return;
        }

        loadSessionAsync(pendingSessionId);
    }

    // Ẩn/hiện panel đặt giá theo role và đổi nhãn điều hướng tài khoản.
    private void configureRoleUi() {
        UserRole role = UserAccount.getCurrentRole();
        setVisibleManaged(browseItemsNav, role == UserRole.ADMIN);
        setVisibleManaged(depositNav, role == UserRole.BIDDER);
        setVisibleManaged(placeBidBox, role == UserRole.BIDDER);
        setVisibleManaged(autoBidBox, role == UserRole.BIDDER);
        if (accountNavButton != null) {
            accountNavButton.setText(role == UserRole.ADMIN ? "Account Management" : "My Account");
        }
        if (accountSectionLabel != null) {
            accountSectionLabel.setText(role == UserRole.ADMIN ? "MANAGEMENT" : "ACCOUNT");
        }
        setText(bidPanelMessageLabel, role == UserRole.BIDDER
                ? ""
                : "Tai khoan nay chi duoc xem phien. Dang nhap BIDDER de dat gia.");
    }

    // Set đồng thời visible và managed để node ẩn không chiếm chỗ layout.
    private void setVisibleManaged(Node node, boolean visible) {
        if (node == null) return;
        node.setVisible(visible);
        node.setManaged(visible);
    }

    // Nhận entity Auction từ controller khác rồi tải lại chi tiết phiên theo id.
    public void setSession(Auction session) {
        if (session == null) {
            return;
        }
        pendingSessionId = session.getId();
        loadSessionAsync(session.getId());
    }

    // Nhận sessionId theo instance controller và tải chi tiết phiên.
    public void setSessionIdInstance(long sessionId) {
        pendingSessionId = sessionId;
        loadSessionAsync(sessionId);
    }

    // Xử lý nút đặt giá: đọc input và gọi BidService trên background thread.
    @FXML
    public void onPlaceBidClick(ActionEvent event) {
        if (!isBidder()) {
            AlertUtil.showError("Chuc nang dat gia chi danh cho nguoi dau gia.");
            return;
        }
        try {
            long bidAmount = parsePositiveLong(bidAmountField, "Giá đặt không hợp lệ.");
            placeBidAsync(bidAmount);
        } catch (Exception e) {
            AlertUtil.showError("Đặt giá thất bại: " + e.getMessage());
        }
    }

    // Gửi lệnh đặt giá trên background thread để thao tác DB không làm treo UI.
    private void placeBidAsync(long bidAmount) {
        disableActions(true);
        Task<Bid> task = new Task<>() {
            @Override
            // Hàm chạy trong background task: gọi BidService để đặt giá.
            protected Bid call() {
                return bidService.placeBid(UserAccount.getUserId(), session.getItem_id(), bidAmount);
            }
        };

        task.setOnSucceeded(event -> {
            clearBidInput();
            refreshData();
        });

        task.setOnFailed(event -> {
            updateActionState();
            Throwable error = task.getException();
            AlertUtil.showError("Đặt giá thất bại: " + (error == null ? "" : error.getMessage()));
        });

        Thread worker = new Thread(task, "place-bid");
        worker.setDaemon(true);
        worker.start();
    }

    // Xử lý nút bật auto bid: đọc giá tối đa và gọi AutoBidService.
    @FXML
    public void onActivateAutobidClick(ActionEvent event) {
        if (!isBidder()) {
            AlertUtil.showError("Chuc nang auto bid chi danh cho nguoi dau gia.");
            return;
        }
        try {
            long maxPrice = parsePositiveLong(autoMaxField, "Giá tối đa auto bid không hợp lệ.");
            autoBidService.configureAndActivate(UserAccount.getUserId(), session.getItem_id(), maxPrice);
            refreshAutoBidStatus();
        } catch (Exception e) {
            AlertUtil.showError("Bật auto bid thất bại: " + e.getMessage());
        }
    }

    // Xử lý nút tắt auto bid của user hiện tại trên item đang đấu giá.
    @FXML
    public void onDeactivateAutobidClick(ActionEvent event) {
        if (!isBidder()) {
            AlertUtil.showError("Chuc nang auto bid chi danh cho nguoi dau gia.");
            return;
        }
        try {
            autoBidService.deactivateByUserAndItem(UserAccount.getUserId(), session.getItem_id());
            refreshAutoBidStatus();
        } catch (Exception e) {
            AlertUtil.showError("Tắt auto bid thất bại: " + e.getMessage());
        }
    }

    // Tải lại dữ liệu phiên, lịch sử bid, số dư và trạng thái auto bid.
    @FXML
    public void onRefreshClick(ActionEvent event) {
        refreshData();
    }

    // Quay lại màn danh sách đấu giá.
    @FXML
    public void onBackClick(ActionEvent event) throws IOException {
        cancelPendingLoadAndStopTimer();
        switchScene(event, "/com/template/hellfx/danhSachDauGia.fxml");
    }

    // Điều hướng sang màn danh sách đấu giá.
    @FXML
    public void goToSessions(ActionEvent event) throws IOException {
        cancelPendingLoadAndStopTimer();
        switchScene(event, "/com/template/hellfx/danhSachDauGia.fxml");
    }

    // Điều hướng sang màn duyệt/quản lý item, chỉ cho Admin.
    @FXML
    public void goToBrowseItems(ActionEvent event) throws IOException {
        if (UserAccount.getCurrentRole() != UserRole.ADMIN) {
            AlertUtil.showError("Chuc nang nay chi danh cho Admin.");
            return;
        }
        cancelPendingLoadAndStopTimer();
        switchScene(event, "/com/template/hellfx/ItemBrowse.fxml");
    }

    // Điều hướng sang màn tài khoản.
    @FXML
    public void goToAccount(ActionEvent event) throws IOException {
        cancelPendingLoadAndStopTimer();
        switchScene(event, "/com/template/hellfx/account.fxml");
    }

    // Điều hướng sang màn nạp tiền, chỉ cho Bidder.
    @FXML
    public void goToDeposit(ActionEvent event) throws IOException {
        if (UserAccount.getCurrentRole() != UserRole.BIDDER) {
            AlertUtil.showError("Chuc nang nay chi danh cho nguoi dau gia.");
            return;
        }
        cancelPendingLoadAndStopTimer();
        switchScene(event, "/com/template/hellfx/Deposit.fxml");
    }

    // Tải dữ liệu session-detail trên background thread để FXMLLoader không khóa UI.
    private void loadSessionAsync(long sessionId) {
        stopTimer();
        long requestToken = ++loadRequestToken;
        disableActions(true);
        setText(itemNameLabel, "Dang tai phien dau gia...");
        setText(itemDescriptionLabel, "");

        Task<SessionDetailData> task = new Task<>() {
            @Override
            // Hàm chạy trong background task: tải toàn bộ dữ liệu cần cho màn chi tiết phiên.
            protected SessionDetailData call() {
                return fetchSessionDetailData(sessionId);
            }
        };

        task.setOnSucceeded(event -> {
            if (requestToken != loadRequestToken) {
                return;
            }
            applySessionDetailData(task.getValue());
            startCountdown();
        });

        task.setOnFailed(event -> {
            if (requestToken != loadRequestToken) {
                return;
            }
            disableActions(true);
            Throwable error = task.getException();
            AlertUtil.showError("Không tải được phiên đấu giá: " + (error == null ? "" : error.getMessage()));
        });

        Thread worker = new Thread(task, "session-detail-load");
        worker.setDaemon(true);
        worker.start();
    }

    // Đồng bộ lại dữ liệu mới nhất của phiên hiện tại.
    private void refreshData() {
        if (session == null) {
            return;
        }

        loadSessionAsync(session.getId());
    }

    // Gọi các service cần thiết để lấy phiên, item, lịch sử bid, số dư và auto bid.
    private SessionDetailData fetchSessionDetailData(long sessionId) {
        Auction loadedSession = auctionService.getById(sessionId);
        if (loadedSession == null) {
            throw new RuntimeException("Không tìm thấy phiên đấu giá.");
        }

        Item loadedItem = itemService.getById(loadedSession.getItem_id());
        List<Bid> bids = bidService.getHistoryBySession(loadedSession.getId());
        long available = 0;
        Optional<Autobid> autobid = Optional.empty();
        if (isBidder()) {
            available = accountService.getAvailable(UserAccount.getUserId());
            autobid = autoBidService.getLatestByUserAndItem(
                    UserAccount.getUserId(),
                    loadedSession.getItem_id()
            );
        }

        return new SessionDetailData(loadedSession, loadedItem, bids, available, autobid);
    }

    // Đổ dữ liệu đã tải lên UI và cập nhật trạng thái nút.
    private void applySessionDetailData(SessionDetailData data) {
        if (data == null || data.session == null) {
            disableActions(true);
            return;
        }

        session = data.session;
        item = data.item;
        populateSession();
        populateBidHistory(data.bids);
        setText(balanceLabel, isBidder() ? formatMoney(data.availableBalance) : "N/A");
        setAutoBidStatus(data.autobid);
        updateActionState();
    }

    // Đổ dữ liệu Auction/Item lên các label trong FXML.
    private void populateSession() {
        setText(sessionIdLabel, String.valueOf(session.getId()));
        setText(itemIdLabel, String.valueOf(session.getItem_id()));
        setText(currentPriceLabel, formatMoney(session.getCurrent_price()));

        if (item != null) {
            setText(itemNameLabel, item.getFullname());
            setText(itemDescriptionLabel, item.getDescription());
        } else {
            setText(itemNameLabel, "(Không tìm thấy vật phẩm)");
            setText(itemDescriptionLabel, "");
        }

        if (session.getState() != AuctionState.RUNNING && session.getCurrent_user_id() > 0) {
            setText(winnerLabel, "Người thắng: " + session.getCurrent_user_id());
        } else if (session.getState() != AuctionState.RUNNING) {
            setText(winnerLabel, "Không có người thắng");
        } else if (session.getCurrent_user_id() > 0) {
            setText(winnerLabel, "Người đang dẫn: " + session.getCurrent_user_id());
        } else {
            setText(winnerLabel, "Chưa có người đặt giá");
        }
    }

    // Tải lại lịch sử bid theo phiên hiện tại.
    private void loadBidHistory() {
        if (bidTable == null) {
            return;
        }

        List<Bid> bids = bidService.getHistoryBySession(session.getId());
        populateBidHistory(bids);
    }

    // Đổ danh sách bid lên bảng lịch sử.
    private void populateBidHistory(List<Bid> bids) {
        if (bidTable != null) {
            bidTable.getItems().setAll(bids == null ? List.of() : bids);
        }
    }

    // Cập nhật số dư khả dụng của Bidder hiện tại.
    private void refreshBalance() {
        if (!isBidder()) {
            setText(balanceLabel, "N/A");
            return;
        }
        try {
            long available = accountService.getAvailable(UserAccount.getUserId());
            setText(balanceLabel, formatMoney(available));
        } catch (Exception e) {
            setText(balanceLabel, "N/A");
        }
    }

    // Tải và hiển thị trạng thái auto bid mới nhất của user trên item hiện tại.
    private void refreshAutoBidStatus() {
        if (!isBidder()) {
            setText(autoBidStatusLabel, "Khong ap dung");
            return;
        }
        autoBidService.getLatestByUserAndItem(UserAccount.getUserId(), session.getItem_id())
                .ifPresentOrElse(value -> {
            String status = value.is_active() ? "Đang bật" : "Đang tắt";
            setText(autoBidStatusLabel, status + " - tối đa " + formatMoney(value.getMax_price()));
        }, () -> setText(autoBidStatusLabel, "Chưa cấu hình"));
    }

    // Hiển thị trạng thái auto bid đã được tải sẵn trong SessionDetailData.
    private void setAutoBidStatus(Optional<Autobid> autobid) {
        if (!isBidder()) {
            setText(autoBidStatusLabel, "Khong ap dung");
            return;
        }
        autobid.ifPresentOrElse(value -> {
            String status = value.is_active() ? "Đang bật" : "Đang tắt";
            setText(autoBidStatusLabel, status + " - tối đa " + formatMoney(value.getMax_price()));
        }, () -> setText(autoBidStatusLabel, "Chưa cấu hình"));
    }

    // Khởi động timer cập nhật thời gian còn lại của phiên.
    private void startCountdown() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateCountdown()));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
        updateCountdown();
    }

    // Cập nhật countdown; nếu hết giờ thì đóng phiên và mở màn Winner.
    private void updateCountdown() {
        if (session == null || session.getEndTime() == null) {
            setText(countdownLabel, "N/A");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(session.getEndTime())) {
            setText(countdownLabel, "Đã kết thúc");
            expireSession();
            return;
        }

        java.time.Duration remaining = java.time.Duration.between(now, session.getEndTime());
        long totalSeconds = remaining.getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        setText(countdownLabel, String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    // Đóng phiên khi countdown hết và chuyển sang màn kết quả người thắng.
    private void expireSession() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        disableActions(true);

        try {
            auctionService.closeSession(session.getId());

            WinnerController.setSessionId(session.getId());
            Node source = getAnyNode();
            if (source != null) {
                Parent root = FXMLLoader.load(getClass().getResource("/com/template/hellfx/Winner.fxml"));
                Stage stage = (Stage) source.getScene().getWindow();
                replaceSceneRoot(stage, root);
            }
        } catch (Exception e) {
            AlertUtil.showError("Phiên đã kết thúc nhưng không xử lý được kết quả: " + e.getMessage());
        }
    }

    // Dừng countdown timer nếu đang chạy.
    private void stopTimer() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
    }

    // Hủy request load cũ và dừng timer trước khi rời màn.
    private void cancelPendingLoadAndStopTimer() {
        loadRequestToken++;
        stopTimer();
    }

    // Chuyển text input thành số dương, ném lỗi nếu rỗng/không phải số/<=0.
    private long parsePositiveLong(TextField field, String errorMessage) {
        if (field == null || field.getText() == null || field.getText().trim().isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }

        long value = Long.parseLong(field.getText().trim());
        if (value <= 0) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }

    // Gán các cột TableView với field trong entity Bid.
    private void configureBidTable() {
        if (colBidId != null) {
            colBidId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getId()));
        }
        if (colBidUserId != null) {
            colBidUserId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getUser_id()));
        }
        if (colBidPrice != null) {
            colBidPrice.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getPrice()));
        }
        if (colBidTime != null) {
            colBidTime.setCellValueFactory(data -> {
                LocalDateTime createdAt = data.getValue().getCreated_at();
                String value = createdAt == null ? "" : createdAt.format(timeFormatter);
                return new SimpleStringProperty(value);
            });
        }
    }

    // Cập nhật trạng thái bật/tắt nút theo dữ liệu phiên và role.
    private void updateActionState() {
        disableActions(session == null || !isBidder());
    }

    // Bật/tắt đồng loạt các nút có thể làm thay đổi phiên.
    private void disableActions(boolean disabled) {
        if (placeBidBtn != null) {
            placeBidBtn.setDisable(disabled);
        }
        if (activateAutobidBtn != null) {
            activateAutobidBtn.setDisable(disabled);
        }
        if (deactivateAutobidBtn != null) {
            deactivateAutobidBtn.setDisable(disabled);
        }
    }

    // Kiểm tra user hiện tại có phải Bidder hay không.
    private boolean isBidder() {
        return UserAccount.getCurrentRole() == UserRole.BIDDER;
    }

    // Xóa ô nhập giá sau khi đặt giá thành công.
    private void clearBidInput() {
        if (bidAmountField != null) {
            bidAmountField.clear();
        }
    }

    // Set text an toàn vì một số fx:id có thể chưa tồn tại trong FXML.
    private void setText(Label label, String value) {
        if (label != null) {
            label.setText(value == null ? "" : value);
        }
    }

    // Định dạng tiền có dấu phẩy ngăn cách hàng nghìn.
    private String formatMoney(long amount) {
        return String.format("%,d", amount);
    }

    // Lấy một node bất kỳ trên scene để tìm Stage hiện tại khi tự động chuyển màn.
    private Node getAnyNode() {
        if (countdownLabel != null) {
            return countdownLabel;
        }
        if (placeBidBtn != null) {
            return placeBidBtn;
        }
        return bidTable;
    }

    // Load FXML mới và thay root scene hiện tại.
    private void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
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

    private static class SessionDetailData {
        private final Auction session;
        private final Item item;
        private final List<Bid> bids;
        private final long availableBalance;
        private final Optional<Autobid> autobid;

        // Gói dữ liệu cần thiết để render màn session detail sau khi background task tải xong.
        private SessionDetailData(
                Auction session,
                Item item,
                List<Bid> bids,
                long availableBalance,
                Optional<Autobid> autobid
        ) {
            this.session = session;
            this.item = item;
            this.bids = bids;
            this.availableBalance = availableBalance;
            this.autobid = autobid == null ? Optional.empty() : autobid;
        }
    }
}
