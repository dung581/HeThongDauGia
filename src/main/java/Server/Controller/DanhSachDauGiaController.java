package Server.Controller;

import Client.Controller.UILogin;
import Client.util.AlertUtil;
import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Item;
import Common.Enum.AuctionState;
import Common.Enum.UserRole;
import Common.Model.user.UserAccount;
import Server.service.AccountService;
import Server.service.AuctionService;
import Server.service.BidService;
import Server.service.ItemService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DanhSachDauGiaController {
    @FXML private ListView<Auction> table;

    @FXML private Label idLabel;
    @FXML private Label tenLabel;
    @FXML private Label giaLabel;
    @FXML private Label minIncrementLabel;
    @FXML private Label trangthaiLabel;
    @FXML private Label thongtinLabel;
    @FXML private Label accountSectionLabel;
    @FXML private Label liveSessionsLabel;
    @FXML private Label totalBidsLabel;
    @FXML private Label leadingSessionsLabel;

    @FXML private Node uploadItemNav;
    @FXML private Node depositNav;
    @FXML private Button accountNavButton;
    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> stateFilter;

    private final AccountService accountService = new AccountService();
    private final AuctionService auctionService = new AuctionService();
    private final ItemService itemService = new ItemService();
    private final BidService bidService = new BidService();
    private final List<Auction> allAuctions = new ArrayList<>();
    private final Map<Long, Item> itemById = new HashMap<>();
    private final Map<Long, String> itemNameById = new HashMap<>();
    private final Map<Long, Integer> bidCountBySession = new HashMap<>();
    private final Map<Long, String> sellerNameById = new HashMap<>();
    private Timeline realtimeTimer;
    private boolean loadingAuctions;
    private int realtimeTicks;

    private static final String ALL_FILTER = "Tat ca";

    // JavaFX tự gọi sau khi load FXML: cấu hình UI, bảng, selection và tải danh sách phiên.
    @FXML
    public void initialize() {
        configureRoleUi();
        configureTable();
        configureSelection();
        configureFilters();
        loadAuctionDataAsync();
        startRealtimeTimer();
    }

    // Ẩn/hiện các mục điều hướng theo role hiện tại của user.
    private void configureRoleUi() {
        UserRole role = UserAccount.getCurrentRole();
        setVisibleManaged(uploadItemNav, role == UserRole.SELLER);
        setVisibleManaged(depositNav, role == UserRole.BIDDER);
        if (accountNavButton != null) {
            accountNavButton.setText(role == UserRole.ADMIN ? "Account Management" : "My Account");
        }
        if (accountSectionLabel != null) {
            accountSectionLabel.setText(role == UserRole.ADMIN ? "MANAGEMENT" : "ACCOUNT");
        }
    }

    // Set đồng thời visible và managed để node ẩn không chiếm chỗ layout.
    private void setVisibleManaged(Node node, boolean visible) {
        if (node == null) return;
        node.setVisible(visible);
        node.setManaged(visible);
    }

    // Cấu hình ListView phiên đấu giá theo dạng từng thanh mềm thay cho bảng cứng.
    private void configureTable() {
        table.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Auction auction, boolean empty) {
                super.updateItem(auction, empty);
                if (empty || auction == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(null);
                setGraphic(createAuctionCard(auction));
            }
        });
    }

    // Tạo card phiên theo layout auction: tên lớn, meta seller/session, giá và countdown.
    private Node createAuctionCard(Auction auction) {
        Item itemData = itemById.get(auction.getItem_id());
        boolean live = isLiveAuction(auction);

        HBox card = new HBox(20.0);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(18.0, 24.0, 18.0, 24.0));
        card.getStyleClass().add(live ? "live-auction-card" : "auction-card-muted");
        card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(event -> {
            table.getSelectionModel().select(auction);
            if (event.getClickCount() >= 2) {
                openAuctionDetailFromNode(card, auction.getId());
            }
        });

        VBox main = new VBox(10.0);
        main.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(main, Priority.ALWAYS);

        Label title = new Label(itemNameById.getOrDefault(auction.getItem_id(), "Item " + auction.getItem_id()));
        title.getStyleClass().add("live-auction-title");
        title.setWrapText(true);

        Label description = new Label(itemData == null ? "Không có mô tả" : nullToText(itemData.getDescription(), "Không có mô tả"));
        description.getStyleClass().add("product-description");
        description.setWrapText(true);

        HBox meta = new HBox(16.0);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label seller = new Label("by " + formatSeller(itemData));
        seller.getStyleClass().add("live-auction-meta");
        Label separator = new Label("|");
        separator.getStyleClass().add("live-auction-meta");
        Label session = new Label("Session #" + auction.getId());
        session.getStyleClass().add("live-auction-meta");
        meta.getChildren().addAll(seller, separator, session);

        HBox stats = new HBox(58.0);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(
                createAuctionStat(statusPillText(auction), "START PRICE", formatCurrency(itemData == null ? auction.getCurrent_price() : itemData.getBeginPrice()), "live-auction-start-price"),
                createAuctionStat(null, "CURRENT BID", formatCurrency(auction.getCurrent_price()), "live-auction-current-price"),
                createAuctionStat(null, "TOTAL BIDS", String.valueOf(bidCountBySession.getOrDefault(auction.getId(), 0)), "live-auction-total-bids")
        );

        main.getChildren().addAll(title, description, meta, stats);

        VBox actionBox = new VBox(26.0);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button joinButton = new Button(live ? "Join ->" : "Xem ->");
        joinButton.getStyleClass().add("live-auction-join-button");
        joinButton.setOnAction(event -> {
            table.getSelectionModel().select(auction);
            openAuctionDetailFromNode(joinButton, auction.getId());
            event.consume();
        });

        VBox timerBox = new VBox(2.0);
        timerBox.setAlignment(Pos.CENTER);
        timerBox.getStyleClass().add("live-auction-timer-box");
        Label timerTitle = new Label(live ? "ENDS IN" : "STATUS");
        timerTitle.getStyleClass().add("live-auction-stat-label");
        Label time = new Label(live ? formatRemaining(auction.getEndTime()) : displayAuctionState(auction));
        time.getStyleClass().add(live ? "live-auction-timer" : "auction-card-status-text");
        timerBox.getChildren().addAll(timerTitle, time);

        actionBox.getChildren().addAll(joinButton, timerBox);
        card.getChildren().addAll(main, actionBox);
        return card;
    }

    // Tạo một cụm số liệu trong card phiên, ví dụ START PRICE hoặc CURRENT BID.
    private VBox createAuctionStat(String pillText, String labelText, String valueText, String valueStyleClass) {
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

    // Mở chi tiết phiên từ node bên trong card, dùng cho nút Join và double click card.
    private void openAuctionDetailFromNode(Node source, long sessionId) {
        try {
            SessionDetailController.setSessionId(sessionId);
            Parent root = FXMLLoader.load(getClass().getResource("/com/template/hellfx/session-detail.fxml"));
            Stage stage = (Stage) source.getScene().getWindow();
            replaceSceneRoot(stage, root);
        } catch (IOException e) {
            AlertUtil.showError("Khong mo duoc chi tiet phien: " + e.getMessage());
        }
    }

    // Phiên live là phiên RUNNING và chưa quá thời gian kết thúc.
    private boolean isLiveAuction(Auction auction) {
        return auction.getState() == AuctionState.RUNNING
                && auction.getEndTime() != null
                && auction.getEndTime().isAfter(LocalDateTime.now());
    }

    // Text pill đầu card: live giữ chữ LIVE, các trạng thái khác hiện đúng state.
    private String statusPillText(Auction auction) {
        return isLiveAuction(auction) ? "LIVE" : displayAuctionState(auction);
    }

    // Chuẩn hóa state null để UI không hiện rỗng.
    private String displayAuctionState(Auction auction) {
        return auction.getState() == null ? "UNKNOWN" : auction.getState().name();
    }

    // Hiển thị seller bằng username nếu đã load được account, fallback về owner id.
    private String formatSeller(Item item) {
        if (item == null || item.getOwner_user_id() <= 0) {
            return "@seller";
        }
        String username = sellerNameById.get(item.getOwner_user_id());
        if (username == null || username.isBlank()) {
            return "@seller_" + item.getOwner_user_id();
        }
        return "@" + username;
    }

    // Countdown của card phiên, cùng format với dashboard bidder.
    private String formatRemaining(LocalDateTime endTime) {
        if (endTime == null) {
            return "--:--";
        }
        long seconds = Duration.between(LocalDateTime.now(), endTime).getSeconds();
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

    // Cho countdown trong danh sách phiên chạy theo thời gian thật và thỉnh thoảng kéo lại dữ liệu DB.
    private void startRealtimeTimer() {
        stopRealtimeTimer();
        realtimeTimer = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
            realtimeTicks++;
            table.refresh();
            updateSummary(table.getItems());
            if (realtimeTicks % 5 == 0) {
                loadAuctionDataAsync();
            }
        }));
        realtimeTimer.setCycleCount(Timeline.INDEFINITE);
        realtimeTimer.play();
    }

    // Dừng timer khi rời màn để không giữ thread/UI update thừa.
    private void stopRealtimeTimer() {
        if (realtimeTimer != null) {
            realtimeTimer.stop();
            realtimeTimer = null;
        }
    }

    // Thêm ký hiệu tiền giống mẫu thiết kế.
    private String formatCurrency(long amount) {
        return "D " + formatMoney(amount);
    }

    // Lắng nghe dòng phiên được chọn để hiển thị thông tin item ở panel bên phải.
    private void configureSelection() {
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedAuction) -> {
            if (selectedAuction == null) {
                clearSelectedItem();
                return;
            }

            Item item = itemById.get(selectedAuction.getItem_id());
            if (item == null) {
                clearSelectedItem();
                return;
            }

            idLabel.setText(String.valueOf(item.getId()));
            tenLabel.setText(item.getFullname());
            thongtinLabel.setText(item.getDescription());
            giaLabel.setText(formatMoney(item.getBeginPrice()));
            minIncrementLabel.setText(formatMoney(getEffectiveMinIncrement(item)));
            trangthaiLabel.setText(item.getStatus() == null ? "" : item.getStatus().toString());
        });
    }

    // Cấu hình ô tìm kiếm và bộ lọc trạng thái, dữ liệu được lọc trực tiếp trong bảng cuộn.
    private void configureFilters() {
        if (stateFilter != null) {
            List<String> options = new ArrayList<>();
            options.add(ALL_FILTER);
            for (AuctionState state : AuctionState.values()) {
                options.add(state.name());
            }
            stateFilter.setItems(FXCollections.observableArrayList(options));
            stateFilter.setValue(ALL_FILTER);
            stateFilter.setOnAction(event -> applyFilters());
        }

        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        }
    }

    // Xóa thông tin item đang chọn khỏi panel bên phải.
    private void clearSelectedItem() {
        idLabel.setText(" ");
        tenLabel.setText(" ");
        giaLabel.setText(" ");
        minIncrementLabel.setText(" ");
        trangthaiLabel.setText(" ");
        thongtinLabel.setText(" ");
    }

    // Tải lại dữ liệu phiên đấu giá từ service.
    @FXML
    public void loadData() {
        loadAuctionDataAsync();
    }

    // Tải danh sách phiên và item trên background thread để không làm lag UI.
    @FXML
    private void loadAuctionDataAsync() {
        if (loadingAuctions) {
            return;
        }
        loadingAuctions = true;

        Task<LoadAuctionData> task = new Task<>() {
            @Override
            // Hàm chạy trong background task: lấy phiên active và map thông tin item liên quan.
            protected LoadAuctionData call() {
                List<Auction> auctions = auctionService.getActive();
                List<Item> items = itemService.listAll();
                List<AccountService.ManagedAccount> accounts = accountService.listManagedAccounts();
                Map<Long, String> itemNames = new HashMap<>();
                Map<Long, Item> itemsById = new HashMap<>();
                Map<Long, Integer> bidCounts = new HashMap<>();
                Map<Long, String> sellerNames = new HashMap<>();

                for (Item item : items) {
                    itemNames.put(item.getId(), item.getFullname());
                    itemsById.put(item.getId(), item);
                }

                for (AccountService.ManagedAccount account : accounts) {
                    if (account.getUserId() != null) {
                        sellerNames.put(account.getUserId(), account.getUsername());
                    }
                }

                // Đếm bid trong background để card render TOTAL BIDS mà không chặn JavaFX thread.
                for (Auction auction : auctions) {
                    try {
                        bidCounts.put(auction.getId(), bidService.getHistoryBySession(auction.getId()).size());
                    } catch (RuntimeException ignored) {
                        bidCounts.put(auction.getId(), 0);
                    }
                }

                return new LoadAuctionData(auctions, itemNames, itemsById, bidCounts, sellerNames);
            }
        };

        task.setOnSucceeded(event -> {
            LoadAuctionData result = task.getValue();
            allAuctions.clear();
            allAuctions.addAll(result.auctions);

            itemNameById.clear();
            itemNameById.putAll(result.itemNames);

            itemById.clear();
            itemById.putAll(result.itemsById);

            bidCountBySession.clear();
            bidCountBySession.putAll(result.bidCounts);

            sellerNameById.clear();
            sellerNameById.putAll(result.sellerNames);

            applyFilters();
            loadingAuctions = false;
        });

        task.setOnFailed(event -> {
            allAuctions.clear();
            itemNameById.clear();
            itemById.clear();
            bidCountBySession.clear();
            sellerNameById.clear();
            clearSelectedItem();
            applyFilters();
            loadingAuctions = false;
        });

        Thread worker = new Thread(task, "auction-list-load");
        worker.setDaemon(true);
        worker.start();
    }

    // Lọc toàn bộ phiên đang có theo trạng thái và nội dung tìm kiếm, không dùng phân trang.
    private void applyFilters() {
        String query = normalize(searchField == null ? "" : searchField.getText());
        String state = stateFilter == null ? ALL_FILTER : stateFilter.getValue();
        List<Auction> rows = new ArrayList<>();

        for (Auction auction : allAuctions) {
            if (!matchesState(auction, state)) {
                continue;
            }
            if (!matchesSearch(auction, query)) {
                continue;
            }
            rows.add(auction);
        }

        table.getItems().setAll(rows);
        table.refresh();
        updateSummary(rows);

        Auction selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || !rows.contains(selected)) {
            table.getSelectionModel().clearSelection();
            clearSelectedItem();
        }
    }

    // Cập nhật các số tổng ở đầu màn theo danh sách phiên đang hiển thị.
    private void updateSummary(List<Auction> rows) {
        if (rows == null) {
            rows = List.of();
        }

        long liveCount = rows.stream().filter(this::isLiveAuction).count();
        int totalBids = rows.stream()
                .mapToInt(auction -> bidCountBySession.getOrDefault(auction.getId(), 0))
                .sum();
        long leadingCount = rows.stream()
                .filter(auction -> auction.getCurrent_user_id() == UserAccount.getUserId())
                .count();

        if (liveSessionsLabel != null) {
            liveSessionsLabel.setText(String.valueOf(liveCount));
        }
        if (totalBidsLabel != null) {
            totalBidsLabel.setText(String.valueOf(totalBids));
        }
        if (leadingSessionsLabel != null) {
            leadingSessionsLabel.setText(String.valueOf(leadingCount));
        }
    }

    // Kiểm tra trạng thái phiên có khớp lựa chọn lọc không.
    private boolean matchesState(Auction auction, String selectedState) {
        if (selectedState == null || selectedState.equals(ALL_FILTER)) {
            return true;
        }
        return auction.getState() != null && auction.getState().name().equals(selectedState);
    }

    // Kiểm tra từ khóa tìm kiếm theo ID phiên, ID item, tên/mô tả item và leader.
    private boolean matchesSearch(Auction auction, String query) {
        if (query.isEmpty()) {
            return true;
        }

        String itemName = itemNameById.getOrDefault(auction.getItem_id(), "");
        Item item = itemById.get(auction.getItem_id());
        String description = item == null ? "" : item.getDescription();
        String minIncrement = item == null ? "" : String.valueOf(getEffectiveMinIncrement(item));
        String target = String.join(" ",
                String.valueOf(auction.getId()),
                String.valueOf(auction.getItem_id()),
                itemName,
                description == null ? "" : description,
                minIncrement,
                String.valueOf(auction.getCurrent_user_id()),
                auction.getState() == null ? "" : auction.getState().name()
        );
        return normalize(target).contains(query);
    }

    // Chuẩn hóa chuỗi để tìm kiếm không phân biệt hoa thường.
    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    // Trả chuỗi dự phòng khi dữ liệu null/rỗng.
    private String nullToText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    // Item cũ có thể chưa có bước giá, fallback 1 để khớp BidService.
    private long getEffectiveMinIncrement(Item item) {
        return item == null || item.getMinIncrement() <= 0 ? 1L : item.getMinIncrement();
    }

    private String formatMoney(long amount) {
        return String.format("%,d", amount);
    }

    // Quay lại dashboard đúng với role hiện tại.
    @FXML
    public void trolai(ActionEvent actionEvent) throws IOException {
        UserRole role = UserAccount.getCurrentRole();
        if (role == UserRole.ADMIN) {
            switchScene(actionEvent, "/com/template/hellfx/dashboard - Admin.fxml");
        } else if (role == UserRole.SELLER) {
            switchScene(actionEvent, "/com/template/hellfx/dashboard - Seller.fxml");
        } else {
            switchScene(actionEvent, "/com/template/hellfx/dashboard-Bidder.fxml");
        }
    }

    // Điều hướng tới màn danh sách phiên đấu giá.
    @FXML
    public void goToSessions(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/danhSachDauGia.fxml");
    }

    // Điều hướng tới màn tài khoản.
    @FXML
    public void goToAccount(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/account.fxml");
    }

    // Điều hướng tới màn nạp tiền, chỉ cho Bidder.
    @FXML
    public void goToDeposit(ActionEvent actionEvent) throws IOException {
        if (UserAccount.getCurrentRole() != UserRole.BIDDER) {
            AlertUtil.showError("Chuc nang nay chi danh cho nguoi dau gia.");
            return;
        }
        switchScene(actionEvent, "/com/template/hellfx/Deposit.fxml");
    }

    // Điều hướng tới màn upload/quản lý sản phẩm của Seller, chỉ cho Seller.
    @FXML
    public void goToUploadItem(ActionEvent actionEvent) throws IOException {
        if (UserAccount.getCurrentRole() != UserRole.SELLER) {
            AlertUtil.showError("Chuc nang nay chi danh cho nguoi ban.");
            return;
        }
        switchScene(actionEvent, "/com/template/hellfx/SellerProducts.fxml");
    }

    // Mở màn chi tiết của phiên đang được chọn trong bảng.
    @FXML
    public void openSessionDetail(ActionEvent actionEvent) throws IOException {
        Auction selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showError("Vui long chon mot phien dau gia.");
            return;
        }

        SessionDetailController.setSessionId(selected.getId());
        switchScene(actionEvent, "/com/template/hellfx/session-detail.fxml");
    }

    // Load FXML mới và thay root scene hiện tại.
    private void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        replaceSceneRoot(stage, root);
    }

    // Giữ nguyên Scene/Stage hiện tại, chỉ thay root để tránh chồng màn.
    private void replaceSceneRoot(Stage stage, Parent root) {
        stopRealtimeTimer();
        Scene currentScene = stage.getScene();
        if (currentScene == null) {
            stage.setScene(new Scene(root, UILogin.APP_WIDTH, UILogin.APP_HEIGHT));
        } else {
            currentScene.setRoot(root);
        }
        stage.show();
    }

    private static class LoadAuctionData {
        private final List<Auction> auctions;
        private final Map<Long, String> itemNames;
        private final Map<Long, Item> itemsById;
        private final Map<Long, Integer> bidCounts;
        private final Map<Long, String> sellerNames;

        // Gói dữ liệu phiên + item trả về từ background task tải danh sách đấu giá.
        private LoadAuctionData(
                List<Auction> auctions,
                Map<Long, String> itemNames,
                Map<Long, Item> itemsById,
                Map<Long, Integer> bidCounts,
                Map<Long, String> sellerNames
        ) {
            this.auctions = auctions;
            this.itemNames = itemNames;
            this.itemsById = itemsById;
            this.bidCounts = bidCounts;
            this.sellerNames = sellerNames;
        }
    }
}
