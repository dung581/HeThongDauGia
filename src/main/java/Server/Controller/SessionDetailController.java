package Server.Controller;

import Client.Controller.UILogin;

import Client.util.AlertUtil;
import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Autobid;
import Common.DataBase.entities.Bid;
import Common.DataBase.entities.Item;
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

    // Cac label hien thong tin chinh cua phien dau gia tren man session-detail.fxml.
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
    @FXML private Node browseItemsNav;
    @FXML private Node depositNav;
    @FXML private Button accountNavButton;

    // O nhap gia dat thu cong va gia toi da cho auto bid.
    @FXML private TextField bidAmountField;
    @FXML private TextField autoMaxField;

    // Cac nut thao tac cua nguoi dung trong man chi tiet phien.
    @FXML private Button placeBidBtn;
    @FXML private Button activateAutobidBtn;
    @FXML private Button deactivateAutobidBtn;

    // Bang lich su dat gia cua item trong phien hien tai.
    @FXML private TableView<Bid> bidTable;
    @FXML private TableColumn<Bid, Long> colBidId;
    @FXML private TableColumn<Bid, Long> colBidUserId;
    @FXML private TableColumn<Bid, Long> colBidPrice;
    @FXML private TableColumn<Bid, String> colBidTime;

    // Luu sessionId tam thoi truoc khi FXMLLoader tao controller moi.
    private static long pendingSessionId;

    // Controller chi goi service, khong goi repository truc tiep.
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

    public static void setSessionId(long sessionId) {
        pendingSessionId = sessionId;
    }

    @FXML
    public void initialize() {
        // JavaFX tu goi sau khi load FXML: cau hinh bang va nap du lieu phien da chon.
        configureRoleUi();
        configureBidTable();

        if (pendingSessionId <= 0) {
            disableActions(true);
            AlertUtil.showError("Chưa chọn phiên đấu giá.");
            return;
        }

        loadSessionAsync(pendingSessionId);
    }

    private void configureRoleUi() {
        UserRole role = UserAccount.getCurrentRole();
        setVisibleManaged(browseItemsNav, role == UserRole.ADMIN);
        setVisibleManaged(depositNav, role == UserRole.BIDDER);
        if (accountNavButton != null) {
            accountNavButton.setText(role == UserRole.ADMIN ? "Account Management" : "My Account");
        }
        if (accountSectionLabel != null) {
            accountSectionLabel.setText(role == UserRole.ADMIN ? "MANAGEMENT" : "ACCOUNT");
        }
    }

    private void setVisibleManaged(Node node, boolean visible) {
        if (node == null) return;
        node.setVisible(visible);
        node.setManaged(visible);
    }

    public void setSession(Auction session) {
        if (session == null) {
            return;
        }
        pendingSessionId = session.getId();
        loadSessionAsync(session.getId());
    }

    public void setSessionIdInstance(long sessionId) {
        pendingSessionId = sessionId;
        loadSessionAsync(sessionId);
    }

    @FXML
    public void onPlaceBidClick(ActionEvent event) {
        // Controller chi doc input va goi BidService; validate gia/lock tien nam trong service.
        try {
            long bidAmount = parsePositiveLong(bidAmountField, "Giá đặt không hợp lệ.");
            bidService.placeBid(UserAccount.getUserId(), session.getItem_id(), bidAmount);
            AlertUtil.showSuccess("Đặt giá thành công.");
            refreshData();
            clearBidInput();
        } catch (Exception e) {
            AlertUtil.showError("Đặt giá thất bại: " + e.getMessage());
        }
    }

    @FXML
    public void onActivateAutobidClick(ActionEvent event) {
        // Controller chi doc input va goi AutoBidService; service xu ly luu + active.
        try {
            long maxPrice = parsePositiveLong(autoMaxField, "Giá tối đa auto bid không hợp lệ.");
            autoBidService.configureAndActivate(UserAccount.getUserId(), session.getItem_id(), maxPrice);
            AlertUtil.showSuccess("Đã bật auto bid.");
            refreshAutoBidStatus();
        } catch (Exception e) {
            AlertUtil.showError("Bật auto bid thất bại: " + e.getMessage());
        }
    }

    @FXML
    public void onDeactivateAutobidClick(ActionEvent event) {
        // Tat auto bid gan nhat cua user tren item hien tai.
        try {
            autoBidService.deactivateByUserAndItem(UserAccount.getUserId(), session.getItem_id());
            AlertUtil.showSuccess("Đã tắt auto bid.");
            refreshAutoBidStatus();
        } catch (Exception e) {
            AlertUtil.showError("Tắt auto bid thất bại: " + e.getMessage());
        }
    }

    @FXML
    public void onRefreshClick(ActionEvent event) {
        // Tai lai gia hien tai, lich su bid, so du va trang thai auto bid.
        refreshData();
    }

    @FXML
    public void onBackClick(ActionEvent event) throws IOException {
        // Quay lai man danh sach dau gia.
        cancelPendingLoadAndStopTimer();
        switchScene(event, "/com/template/hellfx/danhSachDauGia.fxml");
    }

    @FXML
    public void goToSessions(ActionEvent event) throws IOException {
        cancelPendingLoadAndStopTimer();
        switchScene(event, "/com/template/hellfx/danhSachDauGia.fxml");
    }

    @FXML
    public void goToBrowseItems(ActionEvent event) throws IOException {
        if (UserAccount.getCurrentRole() != UserRole.ADMIN) {
            AlertUtil.showError("Chuc nang nay chi danh cho Admin.");
            return;
        }
        cancelPendingLoadAndStopTimer();
        switchScene(event, "/com/template/hellfx/ItemBrowse.fxml");
    }

    @FXML
    public void goToAccount(ActionEvent event) throws IOException {
        cancelPendingLoadAndStopTimer();
        switchScene(event, "/com/template/hellfx/account.fxml");
    }

    @FXML
    public void goToDeposit(ActionEvent event) throws IOException {
        if (UserAccount.getCurrentRole() != UserRole.BIDDER) {
            AlertUtil.showError("Chuc nang nay chi danh cho nguoi dau gia.");
            return;
        }
        cancelPendingLoadAndStopTimer();
        switchScene(event, "/com/template/hellfx/Deposit.fxml");
    }

    private void loadSessionAsync(long sessionId) {
        // Nap du lieu DB tren background thread de FXMLLoader khong khoa UI khi chuyen man.
        stopTimer();
        long requestToken = ++loadRequestToken;
        disableActions(true);
        setText(itemNameLabel, "Dang tai phien dau gia...");
        setText(itemDescriptionLabel, "");

        Task<SessionDetailData> task = new Task<>() {
            @Override
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

    private void refreshData() {
        // Dong bo lai du lieu moi nhat sau khi dat gia, auto bid hoac bam refresh.
        if (session == null) {
            return;
        }

        loadSessionAsync(session.getId());
    }

    private SessionDetailData fetchSessionDetailData(long sessionId) {
        Auction loadedSession = auctionService.getById(sessionId);
        if (loadedSession == null) {
            throw new RuntimeException("Không tìm thấy phiên đấu giá.");
        }

        Item loadedItem = itemService.getById(loadedSession.getItem_id());
        List<Bid> bids = bidService.getHistory(loadedSession.getItem_id());
        long available = accountService.getAvailable(UserAccount.getUserId());
        Optional<Autobid> autobid = autoBidService.getLatestByUserAndItem(
                UserAccount.getUserId(),
                loadedSession.getItem_id()
        );

        return new SessionDetailData(loadedSession, loadedItem, bids, available, autobid);
    }

    private void applySessionDetailData(SessionDetailData data) {
        if (data == null || data.session == null) {
            disableActions(true);
            return;
        }

        session = data.session;
        item = data.item;
        populateSession();
        populateBidHistory(data.bids);
        setText(balanceLabel, formatMoney(data.availableBalance));
        setAutoBidStatus(data.autobid);
        updateActionState();
    }

    private void populateSession() {
        // Do du lieu Auction/Item len cac label trong FXML.
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

        if (session.getCurrent_user_id() > 0) {
            setText(winnerLabel, "Người đang dẫn: " + session.getCurrent_user_id());
        } else {
            setText(winnerLabel, "Chưa có người đặt giá");
        }
    }

    private void loadBidHistory() {
        // Lay lich su bid theo item va hien gia cao nhat len truoc.
        if (bidTable == null) {
            return;
        }

        List<Bid> bids = bidService.getHistory(session.getItem_id());
        populateBidHistory(bids);
    }

    private void populateBidHistory(List<Bid> bids) {
        if (bidTable != null) {
            bidTable.getItems().setAll(bids == null ? List.of() : bids);
        }
    }

    private void refreshBalance() {
        // Lay so du kha dung tu AccountService.
        try {
            long available = accountService.getAvailable(UserAccount.getUserId());
            setText(balanceLabel, formatMoney(available));
        } catch (Exception e) {
            setText(balanceLabel, "N/A");
        }
    }

    private void refreshAutoBidStatus() {
        // Hien trang thai auto bid cua user voi item dang dau gia.
        autoBidService.getLatestByUserAndItem(UserAccount.getUserId(), session.getItem_id())
                .ifPresentOrElse(value -> {
            String status = value.is_active() ? "Đang bật" : "Đang tắt";
            setText(autoBidStatusLabel, status + " - tối đa " + formatMoney(value.getMax_price()));
        }, () -> setText(autoBidStatusLabel, "Chưa cấu hình"));
    }

    private void setAutoBidStatus(Optional<Autobid> autobid) {
        autobid.ifPresentOrElse(value -> {
            String status = value.is_active() ? "Đang bật" : "Đang tắt";
            setText(autoBidStatusLabel, status + " - tối đa " + formatMoney(value.getMax_price()));
        }, () -> setText(autoBidStatusLabel, "Chưa cấu hình"));
    }

    private void startCountdown() {
        // Tao timer chay moi giay de cap nhat thoi gian con lai.
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateCountdown()));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
        updateCountdown();
    }

    private void updateCountdown() {
        // Neu het gio thi dong phien va chuyen sang man Winner.
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

    private void expireSession() {
        // Het countdown thi goi service dong phien, sau do mo man ket qua.
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

    private void stopTimer() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
    }

    private void cancelPendingLoadAndStopTimer() {
        loadRequestToken++;
        stopTimer();
    }

    private long parsePositiveLong(TextField field, String errorMessage) {
        // Chuyen text thanh so duong, nem loi neu rong/khong phai so/<=0.
        if (field == null || field.getText() == null || field.getText().trim().isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }

        long value = Long.parseLong(field.getText().trim());
        if (value <= 0) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }

    private void configureBidTable() {
        // Gan cot TableView voi cac field trong entity Bid.
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

    private void updateActionState() {
        // Controller chi khoa nut khi chua co du lieu phien; rule trang thai phien nam trong service.
        disableActions(session == null);
    }

    private void disableActions(boolean disabled) {
        // Bat/tat dong loat cac nut co the lam thay doi phien.
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

    private void clearBidInput() {
        // Xoa o nhap gia sau khi dat gia thanh cong.
        if (bidAmountField != null) {
            bidAmountField.clear();
        }
    }

    private void setText(Label label, String value) {
        // Set text an toan vi mot so fx:id co the chua ton tai trong FXML.
        if (label != null) {
            label.setText(value == null ? "" : value);
        }
    }

    private String formatMoney(long amount) {
        // Dinh dang tien co dau phay ngan cach hang nghin.
        return String.format("%,d", amount);
    }

    private Node getAnyNode() {
        // Lay mot node bat ky tren scene de tim Stage hien tai khi tu dong chuyen man.
        if (countdownLabel != null) {
            return countdownLabel;
        }
        if (placeBidBtn != null) {
            return placeBidBtn;
        }
        return bidTable;
    }

    private void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        // Doi scene theo duong dan FXML va giu kich thuoc ung dung thong nhat.
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        replaceSceneRoot(stage, root);
    }

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
