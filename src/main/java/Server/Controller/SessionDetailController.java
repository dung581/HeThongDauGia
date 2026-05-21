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
import javafx.scene.layout.VBox;
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
    @FXML private Label bidHistorySummaryLabel;
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

    // Danh sách lịch sử đặt giá của item trong phiên hiện tại.
    @FXML private ListView<Bid> bidTable;

    // Biểu đồ đường giá theo từng lần bid trong phiên hiện tại.
    @FXML private LineChart<String, Number> priceChart;
    @FXML private CategoryAxis priceChartXAxis;
    @FXML private NumberAxis priceChartYAxis;
    @FXML private Label priceChartLatestLabel;
    @FXML private Label priceChartMinLabel;
    @FXML private Label priceChartMaxLabel;

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
    private Optional<Autobid> currentAutobid = Optional.empty();
    private Timeline countdownTimer;
    private Timeline autoRefreshTimer;
    private boolean autoRefreshRunning;
    private long loadRequestToken;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final DateTimeFormatter chartTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

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
        if (session == null) {
            AlertUtil.showError("Chưa tải được phiên đấu giá.");
            return;
        }

        long requestToken = loadRequestToken;
        long itemId = session.getItem_id();
        stopAutoRefresh();
        disableActions(true);
        Task<Bid> task = new Task<>() {
            @Override
            // Hàm chạy trong background task: chỉ gửi lệnh đặt giá, không chờ refresh lại toàn màn.
            protected Bid call() {
                return bidService.placeBid(UserAccount.getUserId(), itemId, bidAmount);
            }
        };

        task.setOnSucceeded(event -> {
            if (requestToken != loadRequestToken) {
                return;
            }
            clearBidInput();
            applyBidOptimistically(task.getValue());
            updateActionState();
            startAutoRefresh();
            refreshLiveDataSilently();
        });

        task.setOnFailed(event -> {
            if (requestToken != loadRequestToken) {
                return;
            }
            updateActionState();
            startAutoRefresh();
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
        stopAutoRefresh();
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
            startAutoRefresh();
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
        return fetchSessionDetailData(sessionId, null, true, Optional.empty());
    }

    // Tải dữ liệu live sau khi đặt giá/auto-refresh, giữ lại item và auto bid đã biết để giảm query.
    private SessionDetailData fetchLiveSessionDetailData(
            long sessionId,
            Item knownItem,
            Optional<Autobid> knownAutobid
    ) {
        return fetchSessionDetailData(sessionId, knownItem, false, knownAutobid);
    }

    // Gọi các service cần thiết, cho phép bỏ qua dữ liệu ít thay đổi trong các lần refresh nhanh.
    private SessionDetailData fetchSessionDetailData(
            long sessionId,
            Item knownItem,
            boolean loadAutobid,
            Optional<Autobid> knownAutobid
    ) {
        Auction loadedSession = auctionService.getById(sessionId);
        if (loadedSession == null) {
            throw new RuntimeException("Không tìm thấy phiên đấu giá.");
        }

        Item loadedItem = knownItem;
        if (loadedItem == null || loadedItem.getId() != loadedSession.getItem_id()) {
            loadedItem = itemService.getById(loadedSession.getItem_id());
        }

        List<Bid> bids = bidService.getHistoryBySession(loadedSession.getId());
        long available = 0;
        Optional<Autobid> autobid = knownAutobid == null ? Optional.empty() : knownAutobid;
        if (isBidder()) {
            available = accountService.getAvailable(UserAccount.getUserId());
            if (loadAutobid) {
                autobid = autoBidService.getLatestByUserAndItem(
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
            disableActions(true);
            return;
        }

        session = data.session;
        item = data.item;
        currentAutobid = data.autobid;
        populateSession();
        populateBidHistory(data.bids);
        populatePriceChart(data.bids);
        setText(balanceLabel, isBidder() ? formatMoney(data.availableBalance) : "N/A");
        setAutoBidStatus(data.autobid);
        updateActionState();
    }

    // Cập nhật UI ngay bằng bid vừa tạo; refresh nền sẽ đồng bộ lại nếu auto-bid đẩy giá cao hơn.
    private void applyBidOptimistically(Bid placedBid) {
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
        populatePriceChart(bids);
    }

    // Đổ danh sách bid lên bảng lịch sử.
    private void populateBidHistory(List<Bid> bids) {
        if (bidTable != null) {
            List<Bid> rows = new ArrayList<>(bids == null ? List.of() : bids);
            rows.sort((left, right) -> compareBidsByCreatedOrder(right, left));
            bidTable.getItems().setAll(rows);
        }
        int count = bids == null ? 0 : bids.size();
        setText(bidHistorySummaryLabel, count + " lượt bid");
    }

    // Cấu hình trục và trạng thái hiển thị của biểu đồ đường giá.
    private void configurePriceChart() {
        if (priceChart != null) {
            priceChart.setAnimated(false);
            priceChart.setLegendVisible(false);
            priceChart.setCreateSymbols(false);
        }
        if (priceChartXAxis != null) {
            priceChartXAxis.setLabel("Thời điểm");
            priceChartXAxis.setTickLabelRotation(0);
        }
        if (priceChartYAxis != null) {
            priceChartYAxis.setLabel("Giá");
            priceChartYAxis.setForceZeroInRange(false);
            priceChartYAxis.setTickLabelFormatter(new StringConverter<>() {
                @Override
                public String toString(Number value) {
                    if (value == null) {
                        return "";
                    }
                    double rawValue = value.doubleValue();
                    if (!Double.isFinite(rawValue)) {
                        return "";
                    }
                    return formatCompactMoney(Math.round(rawValue));
                }

                @Override
                public Number fromString(String value) {
                    return 0;
                }
            });
        }
    }

    // Đổ lịch sử bid lên biểu đồ theo đúng thứ tự thời gian đặt giá.
    private void populatePriceChart(List<Bid> bids) {
        if (priceChart == null) {
            return;
        }

        List<Bid> orderedBids = new ArrayList<>(bids == null ? List.of() : bids);
        orderedBids.sort(this::compareBidsByCreatedOrder);

        if (orderedBids.isEmpty()) {
            priceChart.getData().clear();
            long basePrice = session == null ? 1L : Math.max(1L, session.getCurrent_price());
            configurePriceChartYAxis(basePrice, basePrice);
            updatePriceChartStats(0, 0, 0);
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
            series.getData().add(new XYChart.Data<>(buildPriceChartXLabel(bid, bidIndex), price));
            bidIndex++;
        }

        configurePriceChartYAxis(minPrice, maxPrice);
        updatePriceChartStats(latestPrice, minPrice, maxPrice);
        priceChart.getData().setAll(series);
    }

    // Tạo nhãn trục X: ưu tiên giờ bid, fallback sang số thứ tự nếu dữ liệu cũ chưa có thời gian.
    private String buildPriceChartXLabel(Bid bid, int bidIndex) {
        LocalDateTime createdAt = bid.getCreated_at();
        if (createdAt == null) {
            return "#" + bidIndex;
        }
        return "#" + bidIndex + " " + createdAt.format(chartTimeFormatter);
    }

    // So sánh thứ tự bid: ưu tiên thời gian nếu có đủ, fallback theo id cho dữ liệu cũ thiếu created_at.
    private int compareBidsByCreatedOrder(Bid left, Bid right) {
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

    // Chỉnh khoảng hiển thị trục giá để đường biểu đồ không bị dính sát mép.
    private void configurePriceChartYAxis(double minPrice, double maxPrice) {
        if (priceChartYAxis == null) {
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

        priceChartYAxis.setAutoRanging(false);
        priceChartYAxis.setLowerBound(lowerBound);
        priceChartYAxis.setUpperBound(upperBound);
        priceChartYAxis.setTickUnit(tickUnit);
    }

    // Tính tick đẹp cho trục giá thay vì để JavaFX tự chia quá dày hoặc quá lẻ.
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

    // Cập nhật cụm Last/Low/High trên header của biểu đồ.
    private void updatePriceChartStats(long latestPrice, long minPrice, long maxPrice) {
        if (latestPrice <= 0) {
            setText(priceChartLatestLabel, "-");
            setText(priceChartMinLabel, "-");
            setText(priceChartMaxLabel, "-");
            return;
        }
        setText(priceChartLatestLabel, formatCompactMoney(latestPrice));
        setText(priceChartMinLabel, formatCompactMoney(minPrice));
        setText(priceChartMaxLabel, formatCompactMoney(maxPrice));
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
            currentAutobid = Optional.empty();
            setText(autoBidStatusLabel, "Khong ap dung");
            return;
        }
        currentAutobid = autoBidService.getLatestByUserAndItem(UserAccount.getUserId(), session.getItem_id());
        currentAutobid.ifPresentOrElse(value -> {
            String status = value.is_active() ? "Đang bật" : "Đang tắt";
            setText(autoBidStatusLabel, status + " - tối đa " + formatMoney(value.getMax_price()));
        }, () -> setText(autoBidStatusLabel, "Chưa cấu hình"));
    }

    // Hiển thị trạng thái auto bid đã được tải sẵn trong SessionDetailData.
    private void setAutoBidStatus(Optional<Autobid> autobid) {
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
    private void startCountdown() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateCountdown()));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
        updateCountdown();
    }

    // Tự tải lại phiên định kỳ để giá mới/bid mới của người khác tự hiện trên biểu đồ và lịch sử.
    private void startAutoRefresh() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
        }
        if (session == null || session.getState() != AuctionState.RUNNING) {
            return;
        }

        autoRefreshTimer = new Timeline(new KeyFrame(Duration.seconds(3), event -> refreshLiveDataSilently()));
        autoRefreshTimer.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimer.play();
    }

    // Refresh nền không bật popup lỗi, tránh làm phiền khi dữ liệu DB chậm trong chốc lát.
    private void refreshLiveDataSilently() {
        if (autoRefreshRunning || session == null || session.getState() != AuctionState.RUNNING) {
            return;
        }

        long sessionId = session.getId();
        long requestToken = loadRequestToken;
        Item currentItem = item;
        Optional<Autobid> autobidSnapshot = currentAutobid;
        autoRefreshRunning = true;
        Task<SessionDetailData> task = new Task<>() {
            @Override
            protected SessionDetailData call() {
                return fetchLiveSessionDetailData(sessionId, currentItem, autobidSnapshot);
            }
        };

        task.setOnSucceeded(event -> {
            autoRefreshRunning = false;
            if (requestToken == loadRequestToken && session != null && session.getId() == sessionId) {
                applySessionDetailData(task.getValue());
            }
        });

        task.setOnFailed(event -> autoRefreshRunning = false);

        Thread worker = new Thread(task, "session-detail-auto-refresh");
        worker.setDaemon(true);
        worker.start();
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
        stopAutoRefresh();

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

    // Dừng refresh nền khi rời màn hoặc tải phiên khác.
    private void stopAutoRefresh() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
        }
        autoRefreshRunning = false;
    }

    // Hủy request load cũ và dừng timer trước khi rời màn.
    private void cancelPendingLoadAndStopTimer() {
        loadRequestToken++;
        stopTimer();
        stopAutoRefresh();
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
        if (bidTable == null) {
            return;
        }
        Label emptyLabel = new Label("Chưa có lượt bid nào trong phiên này.");
        emptyLabel.getStyleClass().add("page-subtitle");
        bidTable.setPlaceholder(emptyLabel);
        bidTable.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Bid bid, boolean empty) {
                super.updateItem(bid, empty);
                setText(null);
                setGraphic(empty || bid == null ? null : createBidCard(bid));
            }
        });
    }

    // Tạo một dòng bid gồm mã bid, người đặt, giá đặt và thời điểm đặt.
    private Node createBidCard(Bid bid) {
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
        Label time = new Label(createdAt == null ? "Chua co thoi gian" : createdAt.format(timeFormatter));
        time.getStyleClass().add("bid-history-time");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label price = new Label(formatMoney(bid.getPrice()));
        price.getStyleClass().add("bid-history-price");

        row.getChildren().addAll(title, bidder, time, spacer, price);
        return row;
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

    // Rút gọn số tiền trên trục biểu đồ để label không quá dài.
    private String formatCompactMoney(long amount) {
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
