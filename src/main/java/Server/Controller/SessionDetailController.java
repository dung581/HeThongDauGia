package Server.Controller;

import Client.Controller.UILogin;

import Client.util.AlertUtil;
import Common.DataBase.entities.Account;
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
import java.util.Comparator;
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
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static void setSessionId(long sessionId) {
        pendingSessionId = sessionId;
    }

    @FXML
    public void initialize() {
        // JavaFX tu goi sau khi load FXML: cau hinh bang va nap du lieu phien da chon.
        configureBidTable();

        if (pendingSessionId <= 0) {
            disableActions(true);
            AlertUtil.showError("Chưa chọn phiên đấu giá.");
            return;
        }

        loadSession(pendingSessionId);
    }

    public void setSession(Auction session) {
        if (session == null) {
            return;
        }
        pendingSessionId = session.getId();
        loadSession(session.getId());
    }

    public void setSessionIdInstance(long sessionId) {
        pendingSessionId = sessionId;
        loadSession(sessionId);
    }

    @FXML
    public void onPlaceBidClick(ActionEvent event) {
        // Xu ly nut dat gia: validate role, thoi gian, so tien roi goi BidService.
        if (!ensureBidderCanBid()) {
            return;
        }

        try {
            long bidAmount = parsePositiveLong(bidAmountField, "Giá đặt không hợp lệ.");
            if (bidAmount <= session.getCurrent_price()) {
                AlertUtil.showError("Giá đặt phải cao hơn giá hiện tại.");
                return;
            }

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
        // Bat auto bid voi muc gia toi da ma user nhap.
        if (!ensureBidderCanBid()) {
            return;
        }

        try {
            long maxPrice = parsePositiveLong(autoMaxField, "Giá tối đa auto bid không hợp lệ.");
            if (maxPrice <= session.getCurrent_price()) {
                AlertUtil.showError("Giá tối đa phải cao hơn giá hiện tại.");
                return;
            }

            long available = accountService.getAvailable(UserAccount.getUserId());
            if (maxPrice > available) {
                AlertUtil.showError("Giá tối đa vượt quá số dư khả dụng.");
                return;
            }

            Autobid autobid = autoBidService.configure(UserAccount.getUserId(), session.getItem_id(), maxPrice);
            long autobidId = resolveAutobidId(autobid, UserAccount.getUserId(), session.getItem_id());
            if (autobidId <= 0) {
                throw new RuntimeException("Không tìm thấy auto bid vừa tạo.");
            }

            autoBidService.activate(autobidId);
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
            Optional<Autobid> active = findUserAutoBid();
            if (active.isEmpty()) {
                AlertUtil.showError("Bạn chưa có auto bid cho phiên này.");
                return;
            }

            autoBidService.deactivate(active.get().getId());
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
        switchScene(event, "/com/template/hellfx/danhSachDauGia.fxml");
    }

    private void loadSession(long sessionId) {
        // Nap Auction va Item theo sessionId, sau do khoi dong countdown.
        try {
            session = auctionService.getById(sessionId);
            if (session == null) {
                disableActions(true);
                AlertUtil.showError("Không tìm thấy phiên đấu giá.");
                return;
            }

            item = itemService.getById(session.getItem_id());
            populateSession();
            refreshData();
            startCountdown();
        } catch (Exception e) {
            disableActions(true);
            AlertUtil.showError("Không tải được phiên đấu giá: " + e.getMessage());
        }
    }

    private void refreshData() {
        // Dong bo lai du lieu moi nhat sau khi dat gia, auto bid hoac bam refresh.
        if (session == null) {
            return;
        }

        session = auctionService.getById(session.getId());
        if (session == null) {
            disableActions(true);
            return;
        }

        populateSession();
        loadBidHistory();
        refreshBalance();
        refreshAutoBidStatus();
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
        bids.sort(Comparator.comparingLong(Bid::getPrice).reversed());
        bidTable.getItems().setAll(bids);
    }

    private void refreshBalance() {
        // Tinh so du kha dung = balance - locked_balance cua user hien tai.
        try {
            Account account = accountService.getBalance(UserAccount.getUserId());
            long available = account.getBalance() - account.getLocked_balance();
            setText(balanceLabel, formatMoney(available));
        } catch (Exception e) {
            setText(balanceLabel, "N/A");
        }
    }

    private void refreshAutoBidStatus() {
        // Hien trang thai auto bid cua user voi item dang dau gia.
        Optional<Autobid> autobid = findUserAutoBid();
        if (autobid.isPresent()) {
            Autobid value = autobid.get();
            String status = value.is_active() ? "Đang bật" : "Đang tắt";
            setText(autoBidStatusLabel, status + " - tối đa " + formatMoney(value.getMax_price()));
        } else {
            setText(autoBidStatusLabel, "Chưa cấu hình");
        }
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
        // Dong phien dau gia neu con RUNNING, sau do mo man ket qua.
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        disableActions(true);

        try {
            if (session.getState() == AuctionState.RUNNING) {
                auctionService.closeSession(session.getId());
            }

            WinnerController.setSessionId(session.getId());
            Node source = getAnyNode();
            if (source != null) {
                Parent root = FXMLLoader.load(getClass().getResource("/com/template/hellfx/Winner.fxml"));
                Stage stage = (Stage) source.getScene().getWindow();
                stage.setScene(new Scene(root, UILogin.APP_WIDTH, UILogin.APP_HEIGHT));
                stage.show();
            }
        } catch (Exception e) {
            AlertUtil.showError("Phiên đã kết thúc nhưng không xử lý được kết quả: " + e.getMessage());
        }
    }

    private boolean ensureBidderCanBid() {
        // Kiem tra cac dieu kien truoc khi cho dat gia hoac bat auto bid.
        if (session == null || item == null) {
            AlertUtil.showError("Dữ liệu phiên đấu giá chưa sẵn sàng.");
            return false;
        }

        if (UserAccount.getCurrentRole() == UserRole.SELLER) {
            AlertUtil.showError("Seller không được đặt giá.");
            return false;
        }

        if (session.getState() != AuctionState.RUNNING) {
            AlertUtil.showError("Phiên đấu giá đã kết thúc.");
            return false;
        }

        if (session.getEndTime() != null && !LocalDateTime.now().isBefore(session.getEndTime())) {
            expireSession();
            return false;
        }

        return true;
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

    private Optional<Autobid> findUserAutoBid() {
        // Tim ban ghi auto bid moi nhat cua user tren item hien tai.
        return autoBidService.getByUserId(UserAccount.getUserId())
                .stream()
                .filter(ab -> ab.getItem_id() == session.getItem_id())
                .max(Comparator.comparingLong(Autobid::getId));
    }

    private long resolveAutobidId(Autobid autobid, long userId, long itemId) {
        // Service hien tai khong tra generated id, nen doc lai theo userId/itemId neu id = 0.
        if (autobid != null && autobid.getId() > 0) {
            return autobid.getId();
        }

        return autoBidService.getByUserId(userId)
                .stream()
                .filter(ab -> ab.getItem_id() == itemId)
                .mapToLong(Autobid::getId)
                .max()
                .orElse(0L);
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
        // Khoa cac nut thao tac khi phien khong con RUNNING.
        boolean disabled = session == null || session.getState() != AuctionState.RUNNING;
        disableActions(disabled);
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
        stage.setScene(new Scene(root, UILogin.APP_WIDTH, UILogin.APP_HEIGHT));
        stage.show();
    }
}
