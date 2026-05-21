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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
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

    @FXML private Label availableBalanceLabel;
    @FXML private Label lockedBalanceLabel;
    @FXML private Label activeStakeCountLabel;
    @FXML private Label leadingSessionCountLabel;
    @FXML private Label liveSessionSummaryLabel;
    @FXML private Label sidebarUserLabel;
    @FXML private Label sidebarRoleLabel;
    @FXML private Label sidebarLiveLabel;
    @FXML private Label endedSessionSummaryLabel;
    @FXML private ListView<LiveSessionRow> liveSessionTable;
    @FXML private ListView<EndedSessionRow> endedSessionTable;
    @FXML private Label ownedProductSummaryLabel;
    @FXML private VBox ownedProductList;
    @FXML private Label adminActiveSessionsLabel;
    @FXML private Label adminPendingItemsLabel;
    @FXML private Label adminTotalItemsLabel;
    @FXML private Label adminTotalUsersLabel;
    @FXML private Label adminSidebarUserLabel;
    @FXML private Label adminSidebarPendingLabel;
    @FXML private Label adminSessionSummaryLabel;
    @FXML private Label adminPendingSummaryLabel;
    @FXML private VBox adminSessionList;
    @FXML private ListView<ItemOverviewRow> adminPendingItemTable;
    @FXML private Label sellerTotalItemsLabel;
    @FXML private Label sellerPendingItemsLabel;
    @FXML private Label sellerInAuctionItemsLabel;
    @FXML private Label sellerSoldItemsLabel;
    @FXML private Label sellerSidebarUserLabel;
    @FXML private Label sellerSidebarRoleLabel;
    @FXML private Label sellerSidebarItemsLabel;
    @FXML private Label sellerItemSummaryLabel;
    @FXML private Label sellerSessionSummaryLabel;
    @FXML private ListView<ItemOverviewRow> sellerItemTable;
    @FXML private ListView<SessionOverviewRow> sellerSessionTable;

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

    // Cấu hình các danh sách riêng của dashboard Bidder.
    private void configureBidderTables() {
        if (liveSessionTable != null) {
            liveSessionTable.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(LiveSessionRow row, boolean empty) {
                    super.updateItem(row, empty);
                    if (empty || row == null) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }
                    setText(null);
                    setGraphic(createLiveSessionCard(row));
                }
            });
        }

        if (endedSessionTable != null) {
            endedSessionTable.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(EndedSessionRow row, boolean empty) {
                    super.updateItem(row, empty);
                    if (empty || row == null) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }
                    setText(null);
                    setGraphic(createEndedSessionCard(row));
                }
            });
        }

    }

    // Cấu hình các danh sách riêng của dashboard Admin.
    private void configureAdminTables() {
        if (adminPendingItemTable != null) {
            adminPendingItemTable.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(ItemOverviewRow row, boolean empty) {
                    super.updateItem(row, empty);
                    if (empty || row == null) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }
                    setText(null);
                    setGraphic(createItemOverviewCard(row, true));
                }
            });
        }
    }

    // Cấu hình các danh sách riêng của dashboard Seller.
    private void configureSellerTables() {
        if (sellerItemTable != null) {
            sellerItemTable.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(ItemOverviewRow row, boolean empty) {
                    super.updateItem(row, empty);
                    if (empty || row == null) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }
                    setText(null);
                    setGraphic(createItemOverviewCard(row, false));
                }
            });
        }

        if (sellerSessionTable != null) {
            sellerSessionTable.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(SessionOverviewRow row, boolean empty) {
                    super.updateItem(row, empty);
                    if (empty || row == null) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }
                    setText(null);
                    setGraphic(createAdminSessionNode(row));
                }
            });
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
            renderAdminSessionList(List.of());
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

        renderAdminSessionList(data.sessionRows);
        if (adminPendingItemTable != null) {
            adminPendingItemTable.setItems(FXCollections.observableArrayList(data.pendingRows));
        }
    }

    // Render phiên gần đây của Admin thành từng thanh mềm, có thể bấm để xem hoạt động phiên.
    private void renderAdminSessionList(List<SessionOverviewRow> rows) {
        if (adminSessionList == null) {
            return;
        }

        adminSessionList.getChildren().clear();
        List<SessionOverviewRow> sessions = rows == null ? List.of() : rows;
        if (sessions.isEmpty()) {
            VBox empty = new VBox(6.0);
            empty.setPadding(new Insets(18.0));
            empty.getStyleClass().add("product-empty");
            Label title = new Label("Chua co phien gan day");
            title.getStyleClass().add("product-title");
            Label subtitle = new Label("Cac phien dau gia moi tao hoac vua ket thuc se hien thi o day.");
            subtitle.getStyleClass().add("page-subtitle");
            subtitle.setWrapText(true);
            empty.getChildren().addAll(title, subtitle);
            adminSessionList.getChildren().add(empty);
            return;
        }

        for (SessionOverviewRow row : sessions) {
            adminSessionList.getChildren().add(createAdminSessionNode(row));
        }
    }

    // Tạo một thanh phiên đấu giá gồm tên item, id phiên, giá, leader và trạng thái.
    private Node createAdminSessionNode(SessionOverviewRow row) {
        HBox sessionRow = new HBox(14.0);
        sessionRow.setAlignment(Pos.CENTER_LEFT);
        sessionRow.setPadding(new Insets(13.0, 16.0, 13.0, 16.0));
        sessionRow.getStyleClass().add("session-row");
        sessionRow.setCursor(Cursor.HAND);
        sessionRow.setOnMouseEntered(event -> sessionRow.setTranslateY(-2.0));
        sessionRow.setOnMouseExited(event -> sessionRow.setTranslateY(0.0));
        sessionRow.setOnMouseClicked(event -> openSessionDetailFromNode(sessionRow, row.getSessionId()));

        VBox textBox = new VBox(5.0);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label title = new Label(row.getItemName());
        title.getStyleClass().add("product-title");
        title.setWrapText(true);

        HBox metaBox = new HBox(10.0);
        metaBox.setAlignment(Pos.CENTER_LEFT);
        Label sessionId = new Label("Phien #" + row.getSessionId());
        sessionId.getStyleClass().add("product-meta");
        Label itemId = new Label("Item #" + row.getItemId());
        itemId.getStyleClass().add("product-meta");
        metaBox.getChildren().addAll(sessionId, itemId);
        textBox.getChildren().addAll(title, metaBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox leaderBox = new VBox(4.0);
        leaderBox.setAlignment(Pos.CENTER_RIGHT);
        Label leaderLabel = new Label("Leader");
        leaderLabel.getStyleClass().add("product-meta");
        Label leaderValue = new Label(row.getLeader());
        leaderValue.getStyleClass().add("field-value");
        leaderBox.getChildren().addAll(leaderLabel, leaderValue);

        VBox priceBox = new VBox(6.0);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        Label price = new Label(row.getPrice());
        price.getStyleClass().add("product-price");
        Label status = new Label(row.getStatus());
        status.getStyleClass().add("session-status-pill");
        priceBox.getChildren().addAll(price, status);

        sessionRow.getChildren().addAll(textBox, spacer, leaderBox, priceBox);
        return sessionRow;
    }

    // Mở màn chi tiết phiên từ thanh phiên trên dashboard Admin.
    private void openSessionDetailFromNode(Node source, long sessionId) {
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

    // Tạo card phiên đang chạy cho dashboard Bidder.
    private Node createLiveSessionCard(LiveSessionRow row) {
        HBox card = new HBox(12.0);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12.0, 14.0, 12.0, 14.0));
        card.getStyleClass().add("data-row");
        card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(event -> openSessionDetailFromNode(card, row.getSessionId()));

        VBox main = new VBox(4.0);
        HBox.setHgrow(main, Priority.ALWAYS);
        Label title = new Label(row.getItemName());
        title.getStyleClass().add("data-title");
        title.setWrapText(true);
        Label meta = new Label("Phien #" + row.getSessionId() + " | Leader " + row.getLeader());
        meta.getStyleClass().add("data-meta");
        main.getChildren().addAll(title, meta);

        VBox value = new VBox(5.0);
        value.setAlignment(Pos.CENTER_RIGHT);
        Label price = new Label(row.getCurrentPrice());
        price.getStyleClass().add("data-money");
        Label time = new Label(row.getTimeLeft());
        time.getStyleClass().add("data-pill");
        value.getChildren().addAll(price, time);

        card.getChildren().addAll(main, value);
        return card;
    }

    // Tạo card phiên đã kết thúc cho dashboard Bidder.
    private Node createEndedSessionCard(EndedSessionRow row) {
        HBox card = new HBox(12.0);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12.0, 14.0, 12.0, 14.0));
        card.getStyleClass().add("data-row");
        card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(event -> openWinnerFromNode(card, row.getSessionId()));

        VBox main = new VBox(4.0);
        HBox.setHgrow(main, Priority.ALWAYS);
        Label title = new Label(row.getItemName());
        title.getStyleClass().add("data-title");
        title.setWrapText(true);
        Label meta = new Label("Winner " + row.getWinner() + " | Phien #" + row.getSessionId());
        meta.getStyleClass().add("data-meta");
        main.getChildren().addAll(title, meta);

        VBox value = new VBox(5.0);
        value.setAlignment(Pos.CENTER_RIGHT);
        Label price = new Label(row.getFinalPrice());
        price.getStyleClass().add("data-money");
        Label status = new Label(row.getStatus());
        status.getStyleClass().add("data-pill");
        value.getChildren().addAll(price, status);

        card.getChildren().addAll(main, value);
        return card;
    }

    // Tạo card item tổng quan dùng cho dashboard Admin/Seller.
    private Node createItemOverviewCard(ItemOverviewRow row, boolean showOwner) {
        HBox card = new HBox(12.0);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12.0, 14.0, 12.0, 14.0));
        card.getStyleClass().add("data-row");

        VBox main = new VBox(4.0);
        HBox.setHgrow(main, Priority.ALWAYS);
        Label title = new Label(row.getItemName());
        title.getStyleClass().add("data-title");
        title.setWrapText(true);
        String metaText = "Item #" + row.getId();
        if (showOwner && row.getOwner() != null && !row.getOwner().isBlank()) {
            metaText += " | Seller #" + row.getOwner();
        }
        Label meta = new Label(metaText);
        meta.getStyleClass().add("data-meta");
        main.getChildren().addAll(title, meta);

        VBox value = new VBox(5.0);
        value.setAlignment(Pos.CENTER_RIGHT);
        Label price = new Label(row.getPrice());
        price.getStyleClass().add("data-money");
        Label status = new Label(row.getStatus());
        status.getStyleClass().add("data-pill");
        value.getChildren().addAll(price, status);

        card.getChildren().addAll(main, value);
        return card;
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
                session.getId(),
                session.getItem_id(),
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

    // Chuyển phiên đã thắng thành một thanh sản phẩm trên dashboard Bidder.
    private OwnedProductRow toOwnedProductRow(Auction session, List<Item> items) {
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
                formatMoney(session.getCurrent_price()),
                status
        );
    }

    // Tìm item theo id trong danh sách đã tải sẵn.
    private Item findItem(List<Item> items, long itemId) {
        if (items == null) {
            return null;
        }
        return items.stream()
                .filter(item -> item.getId() == itemId)
                .findFirst()
                .orElse(null);
    }

    // Tải dữ liệu dashboard Bidder: số dư, phiên đang chạy, phiên đã kết thúc và sản phẩm đã thắng.
    private void loadBidderDashboardAsync() {
        setText(availableBalanceLabel, "Dang tai...");
        setText(lockedBalanceLabel, "Dang tai...");
        setText(activeStakeCountLabel, "-");
        setText(leadingSessionCountLabel, "-");
        setText(liveSessionSummaryLabel, "Dang tai du lieu...");
        setText(endedSessionSummaryLabel, "Dang tai du lieu...");
        setText(ownedProductSummaryLabel, "Dang tai du lieu...");
        setText(sidebarLiveLabel, "-");

        Task<BidderDashboardData> task = new Task<>() {
            @Override
            // Hàm chạy trong background task: tổng hợp số dư, stake và phiên của Bidder.
            protected BidderDashboardData call() {
                long userId = UserAccount.getUserId();
                Account account = accountService.getBalance(userId);
                List<Stake> stakes = stakeService.getUserStakes(userId);
                List<Auction> allSessions = auctionService.getAll();
                List<Item> allItems = itemService.listAll();
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

                List<OwnedProductRow> ownedProducts = endedSessions.stream()
                        .filter(session -> session.getCurrent_user_id() == userId)
                        .filter(session -> session.getState() != AuctionState.CANCELED)
                        .sorted(Comparator.comparing(Auction::getEndTime, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(8)
                        .map(session -> DashBoardController.this.toOwnedProductRow(session, allItems))
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

        };

        task.setOnSucceeded(event -> renderBidderDashboard(task.getValue()));
        task.setOnFailed(event -> {
            setText(availableBalanceLabel, "N/A");
            setText(lockedBalanceLabel, "N/A");
            setText(activeStakeCountLabel, "0");
            setText(leadingSessionCountLabel, "0");
            setText(liveSessionSummaryLabel, "Khong tai duoc du lieu dashboard.");
            setText(endedSessionSummaryLabel, "Khong tai duoc du lieu dashboard.");
            setText(ownedProductSummaryLabel, "Khong tai duoc du lieu.");
            setText(sidebarLiveLabel, "0");
            stopLiveCountdown();
            if (liveSessionTable != null) {
                liveSessionTable.setItems(FXCollections.observableArrayList());
            }
            if (endedSessionTable != null) {
                endedSessionTable.setItems(FXCollections.observableArrayList());
            }
            renderOwnedProducts(List.of());
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
        renderOwnedProducts(data.ownedProducts);
    }

    // Render danh sách sản phẩm đã thắng theo dạng từng thanh sản phẩm, không dùng bảng cứng.
    private void renderOwnedProducts(List<OwnedProductRow> products) {
        if (ownedProductList == null) {
            return;
        }

        ownedProductList.getChildren().clear();
        List<OwnedProductRow> rows = products == null ? List.of() : products;
        setText(ownedProductSummaryLabel, rows.size() + " san pham");

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
            ownedProductList.getChildren().add(empty);
            return;
        }

        for (OwnedProductRow row : rows) {
            ownedProductList.getChildren().add(createOwnedProductNode(row));
        }
    }

    // Tạo một thanh sản phẩm gồm tên, mô tả, id phiên/item, giá thắng và trạng thái.
    private Node createOwnedProductNode(OwnedProductRow row) {
        HBox productRow = new HBox(14.0);
        productRow.setAlignment(Pos.CENTER_LEFT);
        productRow.setPadding(new Insets(14.0, 16.0, 14.0, 16.0));
        productRow.getStyleClass().add("product-row");
        productRow.setCursor(Cursor.HAND);
        productRow.setOnMouseEntered(event -> productRow.setTranslateY(-2.0));
        productRow.setOnMouseExited(event -> productRow.setTranslateY(0.0));
        productRow.setOnMouseClicked(event -> openWinnerFromNode(productRow, row.getSessionId()));

        VBox textBox = new VBox(5.0);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        textBox.setCursor(Cursor.HAND);

        Label title = new Label(row.getItemName());
        title.getStyleClass().add("product-title");
        title.setWrapText(true);
        title.setCursor(Cursor.HAND);

        Label description = new Label(row.getDescription());
        description.getStyleClass().add("product-description");
        description.setWrapText(true);
        description.setCursor(Cursor.HAND);

        HBox metaBox = new HBox(8.0);
        metaBox.setAlignment(Pos.CENTER_LEFT);
        metaBox.setCursor(Cursor.HAND);
        Label session = new Label("Phien #" + row.getSessionId());
        session.getStyleClass().add("product-meta");
        session.setCursor(Cursor.HAND);
        Label item = new Label("Item #" + row.getItemId());
        item.getStyleClass().add("product-meta");
        item.setCursor(Cursor.HAND);
        metaBox.getChildren().addAll(session, item);

        textBox.getChildren().addAll(title, description, metaBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        spacer.setCursor(Cursor.HAND);

        VBox valueBox = new VBox(8.0);
        valueBox.setAlignment(Pos.CENTER_RIGHT);
        valueBox.setCursor(Cursor.HAND);
        Label price = new Label(row.getFinalPrice());
        price.getStyleClass().add("product-price");
        price.setCursor(Cursor.HAND);
        Label status = new Label(row.getStatus());
        status.getStyleClass().add("product-pill");
        status.setCursor(Cursor.HAND);
        valueBox.getChildren().addAll(price, status);

        productRow.getChildren().addAll(textBox, spacer, valueBox);
        return productRow;
    }

    // Mở màn kết quả của phiên khi bấm vào sản phẩm đã thắng.
    private void openWinnerFromNode(Node source, long sessionId) {
        try {
            WinnerController.setSessionId(sessionId);
            switchSceneFromNode(source, "/com/template/hellfx/Winner.fxml");
        } catch (IOException e) {
            showWarning("Không mở được kết quả phiên: " + e.getMessage());
        }
    }

    // Chuyển màn từ một Node bất kỳ, dùng cho các card/list item không phát ActionEvent.
    private void switchSceneFromNode(Node source, String fxmlPath) throws IOException {
        URL resource = getClass().getResource(fxmlPath);
        if (resource == null) {
            showWarning("Không tìm thấy màn hình: " + fxmlPath);
            return;
        }

        stopLiveCountdown();
        Parent root = FXMLLoader.load(resource);
        Stage stage = (Stage) source.getScene().getWindow();
        Scene currentScene = stage.getScene();
        if (currentScene == null) {
            stage.setScene(new Scene(root, UILogin.APP_WIDTH, UILogin.APP_HEIGHT));
        } else {
            currentScene.setRoot(root);
        }
        stage.show();
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
        if (liveSessionTable != null) {
            liveSessionTable.refresh();
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

        // Trả về id phiên để ListView hoặc helper khác có thể đọc.
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

        // Trả về property countdown để danh sách tự cập nhật khi timer chạy.
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

    public static class SessionOverviewRow {
        private final long sessionId;
        private final long itemId;
        private final String itemName;
        private final String price;
        private final String leader;
        private final String status;

        // Tạo dòng tổng quan phiên dùng cho dashboard Admin/Seller.
        public SessionOverviewRow(long sessionId, long itemId, String itemName, String price, String leader, String status) {
            this.sessionId = sessionId;
            this.itemId = itemId;
            this.itemName = itemName;
            this.price = price;
            this.leader = leader;
            this.status = status;
        }

        // Trả về id phiên để mở màn hoạt động/chi tiết phiên.
        public long getSessionId() {
            return sessionId;
        }

        // Trả về id item thuộc phiên.
        public long getItemId() {
            return itemId;
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

    public static class OwnedProductRow {
        private final Long itemId;
        private final Long sessionId;
        private final String itemName;
        private final String description;
        private final String finalPrice;
        private final String status;

        // Tạo dữ liệu hiển thị cho một thanh sản phẩm đã thắng của Bidder.
        public OwnedProductRow(Long itemId, Long sessionId, String itemName, String description, String finalPrice, String status) {
            this.itemId = itemId;
            this.sessionId = sessionId;
            this.itemName = itemName;
            this.description = description;
            this.finalPrice = finalPrice;
            this.status = status;
        }

        // Trả về id item đã thắng.
        public Long getItemId() {
            return itemId;
        }

        // Trả về id phiên tạo ra sản phẩm thắng.
        public Long getSessionId() {
            return sessionId;
        }

        // Trả về tên item.
        public String getItemName() {
            return itemName;
        }

        // Trả về mô tả ngắn của item.
        public String getDescription() {
            return description;
        }

        // Trả về giá thắng đã format.
        public String getFinalPrice() {
            return finalPrice;
        }

        // Trả về trạng thái phiên/item.
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
        private final List<OwnedProductRow> ownedProducts;

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
                List<OwnedProductRow> ownedProducts
        ) {
            this.availableBalance = availableBalance;
            this.lockedBalance = lockedBalance;
            this.lockedStakeCount = lockedStakeCount;
            this.leadingSessionCount = leadingSessionCount;
            this.liveSessionCount = liveSessionCount;
            this.endedSessionCount = endedSessionCount;
            this.liveSessions = liveSessions == null ? new ArrayList<>() : liveSessions;
            this.endedSessions = endedSessions == null ? new ArrayList<>() : endedSessions;
            this.ownedProducts = ownedProducts == null ? new ArrayList<>() : ownedProducts;
        }
    }
}
