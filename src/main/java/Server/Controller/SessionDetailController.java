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
import Server.Controller.model.SessionDetailData;
import Server.service.AccountService;
import Server.service.AuctionService;
import Server.service.AutoBidService;
import Server.service.AutoBidService.AutoBidActionResult;
import Server.service.BidService;
import Server.service.ItemService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SessionDetailController {

    // Các label hiển thị thông tin chính của phiên đấu giá trên màn session-detail.fxml.
    @FXML Label itemNameLabel;
    @FXML Label itemDescriptionLabel;
    @FXML Label minIncrementLabel;
    @FXML Label sessionIdLabel;
    @FXML Label itemIdLabel;
    @FXML Label currentPriceLabel;
    @FXML Label countdownLabel;
    @FXML Label balanceLabel;
    @FXML Label winnerLabel;
    @FXML Label autoBidStatusLabel;
    @FXML Label bidPanelMessageLabel;
    @FXML Label bidHistorySummaryLabel;
    @FXML Node placeBidBox;
    @FXML Node autoBidBox;

    // Ô nhập giá đặt thủ công và giá tối đa cho auto bid.
    @FXML TextField bidAmountField;
    @FXML TextField autoMaxField;

    // Các nút thao tác của người dùng trong màn chi tiết phiên.
    @FXML Button placeBidBtn;
    @FXML Button activateAutobidBtn;
    @FXML Button deactivateAutobidBtn;
    @FXML Button refreshDataBtn;

    // Danh sách lịch sử đặt giá của item trong phiên hiện tại.
    @FXML ListView<Bid> bidTable;

    // Biểu đồ đường giá theo từng lần bid trong phiên hiện tại.
    @FXML LineChart<String, Number> priceChart;
    @FXML CategoryAxis priceChartXAxis;
    @FXML NumberAxis priceChartYAxis;
    @FXML Label priceChartLatestLabel;
    @FXML Label priceChartMinLabel;
    @FXML Label priceChartMaxLabel;

    // Lưu sessionId tạm thời trước khi FXMLLoader tạo controller mới.
    private static long pendingSessionId;

    // Controller chỉ gọi service, không gọi repository trực tiếp.
    final AuctionService auctionService = new AuctionService();
    final BidService bidService = new BidService();
    final AccountService accountService = new AccountService();
    final AutoBidService autoBidService = new AutoBidService();
    final ItemService itemService = new ItemService();

    Auction session;
    Item item;
    Optional<Autobid> currentAutobid = Optional.empty();
    Timeline countdownTimer;
    Timeline autoRefreshTimer;
    boolean autoRefreshRunning;
    boolean autoBidResumeRunning;
    boolean placeBidRunning;
    long loadRequestToken;
    long autoBidStateVersion;
    final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    final DateTimeFormatter chartTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    // Các nhóm xử lý riêng giúp class chính chỉ giữ vai trò controller của FXML.
    private final SessionDetailLoader sessionDetailLoader = new SessionDetailLoader(this);
    private final SessionDetailTimerSection timerSection = new SessionDetailTimerSection(this);
    private final BidActionSection bidActionSection = new BidActionSection(this);
    private final BidHistorySection bidHistorySection = new BidHistorySection(this);
    private final PriceChartSection priceChartSection = new PriceChartSection(this);

    // Nhận sessionId từ màn danh sách phiên trước khi load session-detail.fxml.
    public static void setSessionId(long sessionId) {
        pendingSessionId = sessionId;
    }

    // JavaFX tự gọi sau khi load FXML: cấu hình UI, bảng bid và tải phiên được chọn.
    @FXML
    public void initialize() {
        configureRoleUi();
        configureBidTable();
        configurePriceChart();

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
        setVisibleManaged(placeBidBox, role == UserRole.BIDDER);
        setVisibleManaged(autoBidBox, role == UserRole.BIDDER);
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
        bidActionSection.placeBidAsync(bidAmount);
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
            activateAutobidAsync(maxPrice);
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
            deactivateAutobidAsync();
        } catch (Exception e) {
            AlertUtil.showError("Tắt auto bid thất bại: " + e.getMessage());
        }
    }

    // Bật auto bid trên background thread để thao tác DB không làm treo UI.
    private void activateAutobidAsync(long maxPrice) {
        bidActionSection.activateAutobidAsync(maxPrice);
    }

    // Khi mở/tải lại phiên, tiếp tục chuỗi autobid còn dang dở trong DB sau khi app bị tắt.
    void resumeAutoBidsSilently() {
        bidActionSection.resumeAutoBidsSilently();
    }

    // Tắt auto bid trên background thread để UI không bị khóa bởi query DB.
    private void deactivateAutobidAsync() {
        bidActionSection.deactivateAutobidAsync();
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

    // Tải dữ liệu phiên qua helper riêng để class controller không ôm logic truy vấn.
    private void loadSessionAsync(long sessionId) {
        sessionDetailLoader.loadSessionAsync(sessionId);
    }

    // Đồng bộ lại dữ liệu mới nhất của phiên hiện tại qua helper load dữ liệu.
    void refreshData() {
        sessionDetailLoader.refreshData();
    }

    // Cập nhật UI ngay bằng bid vừa tạo; refresh nền sẽ đồng bộ lại nếu auto-bid đẩy giá cao hơn.
    void applyBidOptimistically(Bid placedBid) {
        if (placedBid == null || session == null) {
            return;
        }

        if (placedBid.getPrice() >= session.getCurrent_price()) {
            session.setCurrent_user_id(placedBid.getUser_id());
            session.setCurrent_price(placedBid.getPrice());
            setText(currentPriceLabel, formatMoney(placedBid.getPrice()));
            setText(winnerLabel, "Người đang dẫn: " + placedBid.getUser_id());
        }

        List<Bid> bids = new ArrayList<>();
        if (bidTable != null) {
            bids.addAll(bidTable.getItems());
        }
        boolean alreadyShown = placedBid.getId() > 0 && bids.stream().anyMatch(bid -> bid.getId() == placedBid.getId());
        if (!alreadyShown) {
            bids.add(placedBid);
        }
        populateBidHistory(bids);
        populatePriceChart(bids);
        if (isBidder()) {
            setText(balanceLabel, "Đang cập nhật...");
        }
    }

    // Đổ dữ liệu Auction/Item lên các label trong FXML.
    void populateSession() {
        setText(sessionIdLabel, String.valueOf(session.getId()));
        setText(itemIdLabel, String.valueOf(session.getItem_id()));
        setText(currentPriceLabel, formatMoney(session.getCurrent_price()));

        if (item != null) {
            setText(itemNameLabel, item.getFullname());
            setText(itemDescriptionLabel, item.getDescription());
            setText(minIncrementLabel, "Bước giá tối thiểu: " + formatMoney(getEffectiveMinIncrement(item)));
        } else {
            setText(itemNameLabel, "(Không tìm thấy vật phẩm)");
            setText(itemDescriptionLabel, "");
            setText(minIncrementLabel, "");
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

    // Đổ danh sách bid lên bảng lịch sử.
    void populateBidHistory(List<Bid> bids) {
        bidHistorySection.populate(bids);
    }

    // Cấu hình trục và trạng thái hiển thị của biểu đồ đường giá.
    private void configurePriceChart() {
        priceChartSection.configure();
    }

    // Đổ lịch sử bid lên biểu đồ theo đúng thứ tự thời gian đặt giá.
    void populatePriceChart(List<Bid> bids) {
        priceChartSection.populate(bids);
    }

    // So sánh thứ tự bid: ưu tiên thời gian nếu có đủ, fallback theo id cho dữ liệu cũ thiếu created_at.
    int compareBidsByCreatedOrder(Bid left, Bid right) {
        LocalDateTime leftTime = left.getCreated_at();
        LocalDateTime rightTime = right.getCreated_at();
        if (leftTime != null && rightTime != null) {
            int compared = leftTime.compareTo(rightTime);
            if (compared != 0) {
                return compared;
            }
        }
        return Long.compare(left.getId(), right.getId());
    }

    // Hiển thị trạng thái auto bid đã được tải sẵn trong SessionDetailData.
    void setAutoBidStatus(Optional<Autobid> autobid) {
        currentAutobid = autobid == null ? Optional.empty() : autobid;
        if (!isBidder()) {
            setText(autoBidStatusLabel, "Khong ap dung");
            return;
        }
        currentAutobid.ifPresentOrElse(value -> {
            String status = value.is_active() ? "Đang bật" : "Đang tắt";
            setText(autoBidStatusLabel, status + " - tối đa " + formatMoney(value.getMax_price()));
        }, () -> setText(autoBidStatusLabel, "Chưa cấu hình"));
    }

    // Khởi động timer cập nhật thời gian còn lại của phiên.
    void startCountdown() {
        timerSection.startCountdown();
    }

    // Tự tải lại phiên định kỳ để giá mới/bid mới của người khác tự hiện trên biểu đồ và lịch sử.
    void startAutoRefresh() {
        timerSection.startAutoRefresh();
    }

    // Refresh nền không bật popup lỗi, tránh làm phiền khi dữ liệu DB chậm trong chốc lát.
    void refreshLiveDataSilently() {
        sessionDetailLoader.refreshLiveDataSilently();
    }

    // Dừng countdown timer nếu đang chạy.
    void stopTimer() {
        timerSection.stopTimer();
    }

    // Dừng refresh nền khi rời màn hoặc tải phiên khác.
    void stopAutoRefresh() {
        timerSection.stopAutoRefresh();
    }

    // Hủy request load cũ và dừng timer trước khi rời màn.
    private void cancelPendingLoadAndStopTimer() {
        timerSection.cancelPendingLoadAndStopTimer();
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

    // Cấu hình danh sách lịch sử bid thành từng dòng thông tin mềm, dễ đọc hơn bảng cứng.
    private void configureBidTable() {
        bidHistorySection.configure();
    }

    // Cập nhật trạng thái bật/tắt nút theo dữ liệu phiên và role.
    void updateActionState() {
        boolean disabled = session == null || !isBidder() || placeBidRunning;
        disableActions(disabled);
        if (!disabled && deactivateAutobidBtn != null) {
            boolean autoBidActive = currentAutobid.isPresent() && currentAutobid.get().is_active();
            deactivateAutobidBtn.setDisable(!autoBidActive);
        }
    }

    // Bật/tắt các nút có thể làm thay đổi phiên; nút tải lại vẫn mở để người dùng đồng bộ dữ liệu khi đang đặt giá.
    void disableActions(boolean disabled) {
        if (placeBidBtn != null) {
            placeBidBtn.setDisable(disabled);
        }
        if (activateAutobidBtn != null) {
            activateAutobidBtn.setDisable(disabled);
        }
        if (deactivateAutobidBtn != null) {
            deactivateAutobidBtn.setDisable(disabled);
        }
        if (refreshDataBtn != null) {
            refreshDataBtn.setDisable(false);
        }
    }

    // Kiểm tra user hiện tại có phải Bidder hay không.
    boolean isBidder() {
        return UserAccount.getCurrentRole() == UserRole.BIDDER;
    }

    // Xóa ô nhập giá sau khi đặt giá thành công.
    void clearBidInput() {
        if (bidAmountField != null) {
            bidAmountField.clear();
        }
    }

    // Set text an toàn vì một số fx:id có thể chưa tồn tại trong FXML.
    void setText(Label label, String value) {
        if (label != null) {
            label.setText(value == null ? "" : value);
        }
    }

    // Định dạng tiền có dấu phẩy ngăn cách hàng nghìn.
    String formatMoney(long amount) {
        return String.format("%,d", amount);
    }

    // Item cũ có thể chưa có bước giá, fallback 1 để khớp BidService.
    long getEffectiveMinIncrement(Item item) {
        return item == null || item.getMinIncrement() <= 0 ? 1L : item.getMinIncrement();
    }

    // Rút gọn số tiền trên trục biểu đồ để label không quá dài.
    String formatCompactMoney(long amount) {
        long absolute = Math.abs(amount);
        if (absolute >= 1_000_000_000L) {
            return formatCompactUnit(amount / 1_000_000_000.0, "B");
        }
        if (absolute >= 1_000_000L) {
            return formatCompactUnit(amount / 1_000_000.0, "M");
        }
        if (absolute >= 1_000L) {
            return formatCompactUnit(amount / 1_000.0, "K");
        }
        return String.valueOf(amount);
    }

    // Bỏ phần .0 khi số rút gọn là số tròn.
    private String formatCompactUnit(double value, String suffix) {
        String pattern = Math.abs(value) >= 100 ? "%.0f%s" : "%.1f%s";
        return String.format(java.util.Locale.US, pattern, value, suffix).replace(".0" + suffix, suffix);
    }

    // Lấy một node bất kỳ trên scene để tìm Stage hiện tại khi tự động chuyển màn.
    Node getAnyNode() {
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
    void replaceSceneRoot(Stage stage, Parent root) {
        Scene currentScene = stage.getScene();
        if (currentScene == null) {
            stage.setScene(new Scene(root, UILogin.APP_WIDTH, UILogin.APP_HEIGHT));
        } else {
            currentScene.setRoot(root);
        }
        stage.show();
    }

}

// Nhóm tải dữ liệu cho màn chi tiết phiên.
final class SessionDetailLoader {
    private final SessionDetailController controller;

    SessionDetailLoader(SessionDetailController controller) {
        this.controller = controller;
    }

    // Tải toàn bộ dữ liệu của phiên trên background thread để UI không bị đơ.
    void loadSessionAsync(long sessionId) {
        controller.stopTimer();
        controller.stopAutoRefresh();
        long requestToken = ++controller.loadRequestToken;
        controller.disableActions(true);
        controller.setText(controller.itemNameLabel, "Dang tai phien dau gia...");
        controller.setText(controller.itemDescriptionLabel, "");

        Task<SessionDetailData> task = new Task<>() {
            @Override
            protected SessionDetailData call() {
                return fetchSessionDetailData(sessionId);
            }
        };

        task.setOnSucceeded(event -> {
            if (requestToken != controller.loadRequestToken) {
                return;
            }
            applySessionDetailData(task.getValue());
            controller.startCountdown();
            controller.startAutoRefresh();
            controller.resumeAutoBidsSilently();
        });

        task.setOnFailed(event -> {
            if (requestToken != controller.loadRequestToken) {
                return;
            }
            controller.disableActions(true);
            Throwable error = task.getException();
            AlertUtil.showError("Không tải được phiên đấu giá: " + (error == null ? "" : error.getMessage()));
        });

        startDaemonTask(task, "session-detail-load");
    }

    // Người dùng bấm tải lại: lấy lại dữ liệu mới nhất của phiên đang mở.
    void refreshData() {
        if (controller.session == null) {
            return;
        }

        loadSessionAsync(controller.session.getId());
    }

    // Refresh nền không bật popup lỗi, tránh làm phiền khi DB chậm trong chốc lát.
    void refreshLiveDataSilently() {
        if (controller.autoRefreshRunning
                || controller.session == null
                || controller.session.getState() != AuctionState.RUNNING) {
            return;
        }

        long sessionId = controller.session.getId();
        long requestToken = controller.loadRequestToken;
        Item currentItem = controller.item;
        controller.autoRefreshRunning = true;
        Task<SessionDetailData> task = new Task<>() {
            @Override
            protected SessionDetailData call() {
                return fetchLiveSessionDetailData(sessionId, currentItem);
            }
        };

        task.setOnSucceeded(event -> {
            controller.autoRefreshRunning = false;
            if (requestToken == controller.loadRequestToken
                    && controller.session != null
                    && controller.session.getId() == sessionId) {
                applySessionDetailData(task.getValue());
            }
        });

        task.setOnFailed(event -> controller.autoRefreshRunning = false);

        startDaemonTask(task, "session-detail-auto-refresh");
    }

    // Gọi các service cần thiết để lấy phiên, item, lịch sử bid, số dư và auto bid.
    private SessionDetailData fetchSessionDetailData(long sessionId) {
        return fetchSessionDetailData(sessionId, null, true, Optional.empty());
    }

    // Refresh nhanh giữ lại item đã có, nhưng luôn lấy mới phiên/bid/số dư/autobid.
    private SessionDetailData fetchLiveSessionDetailData(
            long sessionId,
            Item knownItem
    ) {
        return fetchSessionDetailData(sessionId, knownItem, true, Optional.empty());
    }

    // Gói dữ liệu từ nhiều service thành SessionDetailData để controller chỉ cần render.
    private SessionDetailData fetchSessionDetailData(
            long sessionId,
            Item knownItem,
            boolean loadAutobid,
            Optional<Autobid> knownAutobid
    ) {
        Auction loadedSession = controller.auctionService.getById(sessionId);
        if (loadedSession == null) {
            throw new RuntimeException("Không tìm thấy phiên đấu giá.");
        }

        Item loadedItem = knownItem;
        if (loadedItem == null || loadedItem.getId() != loadedSession.getItem_id()) {
            loadedItem = controller.itemService.getById(loadedSession.getItem_id());
        }

        List<Bid> bids = controller.bidService.getHistoryBySession(loadedSession.getId());
        long available = 0;
        Optional<Autobid> autobid = knownAutobid == null ? Optional.empty() : knownAutobid;
        if (controller.isBidder()) {
            available = controller.accountService.getAvailable(UserAccount.getUserId());
            if (loadAutobid) {
                autobid = controller.autoBidService.getLatestByUserAndItem(
                        UserAccount.getUserId(),
                        loadedSession.getItem_id()
                );
            }
        }

        return new SessionDetailData(loadedSession, loadedItem, bids, available, autobid);
    }

    // Đổ dữ liệu đã tải lên UI và cập nhật trạng thái nút.
    private void applySessionDetailData(SessionDetailData data) {
        if (data == null || data.session == null) {
            controller.disableActions(true);
            return;
        }

        controller.session = data.session;
        controller.item = data.item;
        controller.currentAutobid = data.autobid;
        controller.populateSession();
        controller.populateBidHistory(data.bids);
        controller.populatePriceChart(data.bids);
        controller.setText(controller.balanceLabel,
                controller.isBidder() ? controller.formatMoney(data.availableBalance) : "N/A");
        controller.setAutoBidStatus(data.autobid);
        controller.updateActionState();
    }

    // Chạy task dưới dạng daemon để task nền không giữ app sống khi người dùng tắt cửa sổ.
    private void startDaemonTask(Task<?> task, String threadName) {
        Thread worker = new Thread(task, threadName);
        worker.setDaemon(true);
        worker.start();
    }
}

// Nhóm timer của màn chi tiết phiên.
final class SessionDetailTimerSection {
    private final SessionDetailController controller;

    SessionDetailTimerSection(SessionDetailController controller) {
        this.controller = controller;
    }

    // Khởi động timer đếm ngược thời gian còn lại của phiên.
    void startCountdown() {
        if (controller.countdownTimer != null) {
            controller.countdownTimer.stop();
        }

        controller.countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateCountdown()));
        controller.countdownTimer.setCycleCount(Timeline.INDEFINITE);
        controller.countdownTimer.play();
        updateCountdown();
    }

    // Tự refresh dữ liệu phiên định kỳ để thấy bid mới mà không cần bấm tải lại.
    void startAutoRefresh() {
        if (controller.autoRefreshTimer != null) {
            controller.autoRefreshTimer.stop();
        }
        if (controller.session == null || controller.session.getState() != AuctionState.RUNNING) {
            return;
        }

        controller.autoRefreshTimer = new Timeline(
                // Mỗi nhịp vừa xử lý thêm một bước autobid nếu cần, vừa refresh lại dữ liệu phiên.
                new KeyFrame(Duration.seconds(1), event -> controller.resumeAutoBidsSilently())
        );
        controller.autoRefreshTimer.setCycleCount(Timeline.INDEFINITE);
        controller.autoRefreshTimer.play();
    }

    // Dừng countdown timer nếu đang chạy.
    void stopTimer() {
        if (controller.countdownTimer != null) {
            controller.countdownTimer.stop();
        }
    }

    // Dừng refresh nền khi rời màn hoặc tải phiên khác.
    void stopAutoRefresh() {
        if (controller.autoRefreshTimer != null) {
            controller.autoRefreshTimer.stop();
        }
        controller.autoRefreshRunning = false;
    }

    // Hủy request load cũ và dừng toàn bộ timer trước khi rời màn.
    void cancelPendingLoadAndStopTimer() {
        controller.loadRequestToken++;
        stopTimer();
        stopAutoRefresh();
    }

    // Cập nhật countdown; nếu hết giờ thì đóng phiên và mở màn Winner.
    private void updateCountdown() {
        if (controller.session == null || controller.session.getEndTime() == null) {
            controller.setText(controller.countdownLabel, "N/A");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(controller.session.getEndTime())) {
            controller.setText(controller.countdownLabel, "Đã kết thúc");
            expireSession();
            return;
        }

        java.time.Duration remaining = java.time.Duration.between(now, controller.session.getEndTime());
        long totalSeconds = remaining.getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        controller.setText(controller.countdownLabel, String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    // Đóng phiên khi countdown hết và chuyển sang màn kết quả người thắng.
    private void expireSession() {
        stopTimer();
        stopAutoRefresh();
        controller.disableActions(true);

        try {
            if (!controller.auctionService.closeIfExpired(controller.session.getId())) {
                return;
            }

            WinnerController.setSessionId(controller.session.getId());
            Node source = controller.getAnyNode();
            if (source != null) {
                Parent root = FXMLLoader.load(
                        controller.getClass().getResource("/com/template/hellfx/Winner.fxml")
                );
                Stage stage = (Stage) source.getScene().getWindow();
                controller.replaceSceneRoot(stage, root);
            }
        } catch (Exception e) {
            AlertUtil.showError("Phiên đã kết thúc nhưng không xử lý được kết quả: " + e.getMessage());
        }
    }
}

// Nhóm thao tác đặt giá và auto bid.
final class BidActionSection {
    private final SessionDetailController controller;

    BidActionSection(SessionDetailController controller) {
        this.controller = controller;
    }

    // Đặt giá thủ công: khóa panel thao tác, gọi BidService ở background rồi cập nhật lại UI.
    void placeBidAsync(long bidAmount) {
        if (controller.session == null) {
            AlertUtil.showError("Chưa tải được phiên đấu giá.");
            return;
        }

        long requestToken = controller.loadRequestToken;
        long sessionId = controller.session.getId();
        long itemId = controller.session.getItem_id();
        controller.placeBidRunning = true;
        controller.stopAutoRefresh();
        controller.disableActions(true);
        controller.setText(controller.bidPanelMessageLabel, "Đang đặt giá...");
        Task<Bid> task = new Task<>() {
            @Override
            // Hàm chạy trong background task: chỉ gửi lệnh đặt giá, không chờ refresh lại toàn màn.
            protected Bid call() {
                return controller.bidService.placeBid(UserAccount.getUserId(), itemId, bidAmount);
            }
        };

        task.setOnSucceeded(event -> {
            controller.placeBidRunning = false;
            if (controller.session == null || controller.session.getId() != sessionId) {
                return;
            }
            controller.clearBidInput();
            controller.setText(controller.bidPanelMessageLabel, "Đặt giá thành công.");
            if (requestToken == controller.loadRequestToken) {
                controller.applyBidOptimistically(task.getValue());
            }
            controller.updateActionState();
            controller.startAutoRefresh();
            resumeAutoBidsSilently();
        });

        task.setOnFailed(event -> {
            controller.placeBidRunning = false;
            if (controller.session == null || controller.session.getId() != sessionId) {
                return;
            }
            controller.updateActionState();
            controller.startAutoRefresh();
            Throwable error = task.getException();
            String message = "Đặt giá thất bại: " + (error == null ? "" : error.getMessage());
            controller.setText(controller.bidPanelMessageLabel, message);
            AlertUtil.showError(message);
        });

        startDaemonTask(task, "place-bid");
    }

    // Bật auto bid cho tài khoản hiện tại, lưu mức giá tối đa rồi kiểm tra auto bid ngay.
    void activateAutobidAsync(long maxPrice) {
        if (controller.session == null) {
            AlertUtil.showError("Chưa tải được phiên đấu giá.");
            return;
        }

        long requestToken = controller.loadRequestToken;
        long version = ++controller.autoBidStateVersion;
        long userId = UserAccount.getUserId();
        long sessionId = controller.session.getId();
        long itemId = controller.session.getItem_id();
        long minIncrement = controller.getEffectiveMinIncrement(controller.item);
        controller.stopAutoRefresh();
        controller.disableActions(true);
        controller.setText(controller.autoBidStatusLabel, "Dang xu ly...");

        Task<Optional<Autobid>> task = new Task<>() {
            @Override
            protected Optional<Autobid> call() {
                return controller.autoBidService.configureAndGetLatest(userId, itemId, maxPrice);
            }
        };

        task.setOnSucceeded(event -> {
            if (requestToken != controller.loadRequestToken || version != controller.autoBidStateVersion) {
                return;
            }
            controller.setAutoBidStatus(task.getValue());
            controller.setText(controller.bidPanelMessageLabel, "Auto bid đã bật.");
            controller.updateActionState();
            controller.startAutoRefresh();
            startImmediateAutobidCheckAsync(requestToken, version, userId, sessionId, itemId, minIncrement);
        });

        task.setOnFailed(event -> {
            if (requestToken != controller.loadRequestToken || version != controller.autoBidStateVersion) {
                return;
            }
            controller.updateActionState();
            controller.startAutoRefresh();
            Throwable error = task.getException();
            AlertUtil.showError("Bật auto bid thất bại: " + (error == null ? "" : error.getMessage()));
        });

        startDaemonTask(task, "activate-auto-bid");
    }

    // Sau khi bật auto bid, chạy một lượt kiểm tra để service tự đặt giá nếu đang cần vượt người khác.
    void startImmediateAutobidCheckAsync(
            long requestToken,
            long version,
            long userId,
            long sessionId,
            long itemId,
            long minIncrement
    ) {
        Task<AutoBidActionResult> task = new Task<>() {
            @Override
            protected AutoBidActionResult call() {
                return controller.autoBidService.resumeForSessionAndGetLatest(userId, sessionId, itemId, minIncrement);
            }
        };

        task.setOnSucceeded(event -> {
            if (requestToken != controller.loadRequestToken || version != controller.autoBidStateVersion) {
                return;
            }
            AutoBidActionResult result = task.getValue();
            controller.setAutoBidStatus(result.getAutobid());
            if (result.getMessage() != null && !result.getMessage().isBlank()) {
                controller.setText(controller.bidPanelMessageLabel, result.getMessage());
            }
            controller.refreshLiveDataSilently();
        });

        task.setOnFailed(event -> {
            if (requestToken != controller.loadRequestToken || version != controller.autoBidStateVersion) {
                return;
            }
            Throwable error = task.getException();
            controller.setText(controller.bidPanelMessageLabel, "Auto bid đã bật nhưng chưa đặt được ngay"
                    + (error == null || error.getMessage() == null ? "." : ": " + error.getMessage()));
            controller.refreshLiveDataSilently();
        });

        startDaemonTask(task, "auto-bid-immediate-check");
    }

    // Tiếp tục xử lý auto bid của toàn phiên sau khi có một lượt bid mới.
    void resumeAutoBidsSilently() {
        if (controller.autoBidResumeRunning
                || controller.session == null
                || controller.session.getState() != AuctionState.RUNNING) {
            return;
        }

        long requestToken = controller.loadRequestToken;
        long sessionId = controller.session.getId();
        long itemId = controller.session.getItem_id();
        long minIncrement = controller.getEffectiveMinIncrement(controller.item);
        controller.autoBidResumeRunning = true;

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return controller.autoBidService.resumeForSession(sessionId, itemId, minIncrement);
            }
        };

        task.setOnSucceeded(event -> {
            controller.autoBidResumeRunning = false;
            if (requestToken != controller.loadRequestToken
                    || controller.session == null
                    || controller.session.getId() != sessionId) {
                return;
            }
            controller.refreshLiveDataSilently();
        });

        task.setOnFailed(event -> controller.autoBidResumeRunning = false);

        startDaemonTask(task, "auto-bid-resume");
    }

    // Tắt auto bid của tài khoản hiện tại cho item đang xem.
    void deactivateAutobidAsync() {
        if (controller.session == null) {
            AlertUtil.showError("Chưa tải được phiên đấu giá.");
            return;
        }

        long requestToken = controller.loadRequestToken;
        long version = ++controller.autoBidStateVersion;
        long userId = UserAccount.getUserId();
        long itemId = controller.session.getItem_id();
        controller.stopAutoRefresh();
        controller.disableActions(true);
        controller.setText(controller.autoBidStatusLabel, "Dang xu ly...");

        Task<Optional<Autobid>> task = new Task<>() {
            @Override
            protected Optional<Autobid> call() {
                return controller.autoBidService.deactivateAndGetLatest(userId, itemId);
            }
        };

        task.setOnSucceeded(event -> {
            if (requestToken != controller.loadRequestToken || version != controller.autoBidStateVersion) {
                return;
            }
            controller.setAutoBidStatus(task.getValue());
            controller.updateActionState();
            controller.startAutoRefresh();
            controller.refreshLiveDataSilently();
        });

        task.setOnFailed(event -> {
            if (requestToken != controller.loadRequestToken || version != controller.autoBidStateVersion) {
                return;
            }
            controller.updateActionState();
            controller.startAutoRefresh();
            Throwable error = task.getException();
            AlertUtil.showError("Tắt auto bid thất bại: " + (error == null ? "" : error.getMessage()));
        });

        startDaemonTask(task, "deactivate-auto-bid");
    }

    // Tạo thread daemon để tác vụ nền không chặn JavaFX Application Thread.
    private void startDaemonTask(Task<?> task, String threadName) {
        Thread worker = new Thread(task, threadName);
        worker.setDaemon(true);
        worker.start();
    }
}

// Nhóm UI lịch sử bid: cấu hình ListView và dựng từng dòng lịch sử bid.
final class BidHistorySection {
    private final SessionDetailController controller;

    BidHistorySection(SessionDetailController controller) {
        this.controller = controller;
    }

    // Gắn placeholder và cell factory cho bảng lịch sử bid.
    void configure() {
        if (controller.bidTable == null) {
            return;
        }
        Label emptyLabel = new Label("Chưa có lượt bid nào trong phiên này.");
        emptyLabel.getStyleClass().add("page-subtitle");
        controller.bidTable.setPlaceholder(emptyLabel);
        controller.bidTable.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Bid bid, boolean empty) {
                super.updateItem(bid, empty);
                setText(null);
                setGraphic(empty || bid == null ? null : createBidCard(bid));
            }
        });
    }

    // Đưa danh sách bid mới vào ListView, sắp xếp bid mới nhất lên đầu.
    void populate(List<Bid> bids) {
        if (controller.bidTable != null) {
            List<Bid> rows = new ArrayList<>(bids == null ? List.of() : bids);
            rows.sort((left, right) -> controller.compareBidsByCreatedOrder(right, left));
            controller.bidTable.getItems().setAll(rows);
        }
        int count = bids == null ? 0 : bids.size();
        controller.setText(controller.bidHistorySummaryLabel, count + " lượt bid");
    }

    // Dựng một dòng lịch sử bid gồm mã bid, bidder, thời gian và giá.
    Node createBidCard(Bid bid) {
        HBox row = new HBox(18);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("bid-history-row");
        row.setMaxWidth(Double.MAX_VALUE);
        row.setPadding(new Insets(12, 16, 12, 16));

        Label title = new Label("Bid #" + bid.getId());
        title.getStyleClass().add("bid-history-id");

        Label bidder = new Label("Bidder #" + bid.getUser_id());
        bidder.getStyleClass().add("bid-history-bidder");

        LocalDateTime createdAt = bid.getCreated_at();
        Label time = new Label(createdAt == null ? "Chua co thoi gian" : createdAt.format(controller.timeFormatter));
        time.getStyleClass().add("bid-history-time");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label price = new Label(controller.formatMoney(bid.getPrice()));
        price.getStyleClass().add("bid-history-price");

        row.getChildren().addAll(title, bidder, time, spacer, price);
        return row;
    }
}

// Nhóm UI biểu đồ giá: cấu hình chart, nạp dữ liệu bid và cập nhật thống kê giá.
final class PriceChartSection {
    private final SessionDetailController controller;

    PriceChartSection(SessionDetailController controller) {
        this.controller = controller;
    }

    // Cấu hình chart một lần khi màn hình được khởi tạo.
    void configure() {
        if (controller.priceChart != null) {
            controller.priceChart.setAnimated(false);
            controller.priceChart.setLegendVisible(false);
            controller.priceChart.setCreateSymbols(false);
        }
        if (controller.priceChartXAxis != null) {
            controller.priceChartXAxis.setLabel("Thời điểm");
            controller.priceChartXAxis.setTickLabelRotation(0);
        }
        if (controller.priceChartYAxis != null) {
            controller.priceChartYAxis.setLabel("Giá");
            controller.priceChartYAxis.setForceZeroInRange(false);
            controller.priceChartYAxis.setTickLabelFormatter(new StringConverter<>() {
                @Override
                public String toString(Number value) {
                    if (value == null) {
                        return "";
                    }
                    double rawValue = value.doubleValue();
                    if (!Double.isFinite(rawValue)) {
                        return "";
                    }
                    return controller.formatCompactMoney(Math.round(rawValue));
                }

                @Override
                public Number fromString(String value) {
                    return 0;
                }
            });
        }
    }

    // Vẽ lại biểu đồ từ danh sách bid hiện tại của phiên.
    void populate(List<Bid> bids) {
        if (controller.priceChart == null) {
            return;
        }

        List<Bid> orderedBids = new ArrayList<>(bids == null ? List.of() : bids);
        orderedBids.sort(controller::compareBidsByCreatedOrder);

        if (orderedBids.isEmpty()) {
            controller.priceChart.getData().clear();
            long basePrice = controller.session == null ? 1L : Math.max(1L, controller.session.getCurrent_price());
            configureYAxis(basePrice, basePrice);
            updateStats(0, 0, 0);
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        int bidIndex = 1;
        long minPrice = Long.MAX_VALUE;
        long maxPrice = Long.MIN_VALUE;
        long latestPrice = 0;
        for (Bid bid : orderedBids) {
            long price = bid.getPrice();
            minPrice = Math.min(minPrice, price);
            maxPrice = Math.max(maxPrice, price);
            latestPrice = price;
            series.getData().add(new XYChart.Data<>(buildXLabel(bid, bidIndex), price));
            bidIndex++;
        }

        configureYAxis(minPrice, maxPrice);
        updateStats(latestPrice, minPrice, maxPrice);
        controller.priceChart.getData().setAll(series);
    }

    // Tạo nhãn trục X theo thứ tự bid, kèm thời gian nếu dữ liệu có created_at.
    private String buildXLabel(Bid bid, int bidIndex) {
        LocalDateTime createdAt = bid.getCreated_at();
        if (createdAt == null) {
            return "#" + bidIndex;
        }
        return "#" + bidIndex + " " + createdAt.format(controller.chartTimeFormatter);
    }

    // Tự tính khoảng hiển thị trục Y để đường giá không bị sát mép biểu đồ.
    private void configureYAxis(double minPrice, double maxPrice) {
        if (controller.priceChartYAxis == null) {
            return;
        }

        double range = Math.max(1.0, maxPrice - minPrice);
        double padding = Math.max(5.0, range * 0.12);

        double rawLowerBound = Math.max(0.0, minPrice - padding);
        double rawUpperBound = Math.max(rawLowerBound + 1.0, maxPrice + padding);
        double tickUnit = calculateNiceTickUnit(rawLowerBound, rawUpperBound);
        double lowerBound = Math.max(0.0, Math.floor(rawLowerBound / tickUnit) * tickUnit);
        double upperBound = Math.ceil(rawUpperBound / tickUnit) * tickUnit;
        if (upperBound <= lowerBound) {
            upperBound = lowerBound + tickUnit;
        }

        controller.priceChartYAxis.setAutoRanging(false);
        controller.priceChartYAxis.setLowerBound(lowerBound);
        controller.priceChartYAxis.setUpperBound(upperBound);
        controller.priceChartYAxis.setTickUnit(tickUnit);
    }

    // Làm tròn bước chia trục Y về các mốc dễ đọc như 1, 2, 5, 10, 20, 50...
    private double calculateNiceTickUnit(double lowerBound, double upperBound) {
        double rawTick = (upperBound - lowerBound) / 4.0;
        if (rawTick <= 0) {
            return 1.0;
        }

        double magnitude = Math.pow(10, Math.floor(Math.log10(rawTick)));
        double normalized = rawTick / magnitude;
        double niceNormalized;
        if (normalized <= 1) {
            niceNormalized = 1;
        } else if (normalized <= 2) {
            niceNormalized = 2;
        } else if (normalized <= 5) {
            niceNormalized = 5;
        } else {
            niceNormalized = 10;
        }
        return niceNormalized * magnitude;
    }

    // Cập nhật ba chỉ số nhỏ phía trên biểu đồ: giá mới nhất, thấp nhất, cao nhất.
    private void updateStats(long latestPrice, long minPrice, long maxPrice) {
        if (latestPrice <= 0) {
            controller.setText(controller.priceChartLatestLabel, "-");
            controller.setText(controller.priceChartMinLabel, "-");
            controller.setText(controller.priceChartMaxLabel, "-");
            return;
        }
        controller.setText(controller.priceChartLatestLabel, controller.formatCompactMoney(latestPrice));
        controller.setText(controller.priceChartMinLabel, controller.formatCompactMoney(minPrice));
        controller.setText(controller.priceChartMaxLabel, controller.formatCompactMoney(maxPrice));
    }
}
