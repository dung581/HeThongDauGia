package Server.Controller;

import Client.Controller.UILogin;
import Client.util.AlertUtil;
import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Item;
import Common.Enum.AuctionState;
import Common.Enum.UserRole;
import Common.Model.user.UserAccount;
import Server.service.AuctionService;
import Server.service.ItemService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
    @FXML private Label trangthaiLabel;
    @FXML private Label thongtinLabel;
    @FXML private Label accountSectionLabel;

    @FXML private Node browseItemsNav;
    @FXML private Node uploadItemNav;
    @FXML private Node depositNav;
    @FXML private Button accountNavButton;
    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> stateFilter;

    private final AuctionService auctionService = new AuctionService();
    private final ItemService itemService = new ItemService();
    private final List<Auction> allAuctions = new ArrayList<>();
    private final Map<Long, Item> itemById = new HashMap<>();
    private final Map<Long, String> itemNameById = new HashMap<>();

    private static final String ALL_FILTER = "Tat ca";

    // JavaFX tự gọi sau khi load FXML: cấu hình UI, bảng, selection và tải danh sách phiên.
    @FXML
    public void initialize() {
        configureRoleUi();
        configureTable();
        configureSelection();
        configureFilters();
        loadAuctionDataAsync();
    }

    // Ẩn/hiện các mục điều hướng theo role hiện tại của user.
    private void configureRoleUi() {
        UserRole role = UserAccount.getCurrentRole();
        setVisibleManaged(browseItemsNav, role == UserRole.ADMIN);
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

    // Tạo card hiển thị thông tin chính của một phiên đấu giá.
    private Node createAuctionCard(Auction auction) {
        HBox row = new HBox(14.0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14.0, 16.0, 14.0, 16.0));
        row.getStyleClass().add("data-row");

        VBox titleBox = new VBox(5.0);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label title = new Label(itemNameById.getOrDefault(auction.getItem_id(), "Item " + auction.getItem_id()));
        title.getStyleClass().add("data-title");
        title.setWrapText(true);
        HBox meta = new HBox(10.0);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label session = new Label("Phien #" + auction.getId());
        session.getStyleClass().add("data-meta");
        Label item = new Label("Item #" + auction.getItem_id());
        item.getStyleClass().add("data-meta");
        Label time = new Label(formatTimeRange(auction));
        time.getStyleClass().add("data-meta");
        meta.getChildren().addAll(session, item, time);
        titleBox.getChildren().addAll(title, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox leaderBox = valueBox("Leader", auction.getCurrent_user_id() == 0 ? "-" : String.valueOf(auction.getCurrent_user_id()));
        VBox priceBox = new VBox(6.0);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        Label price = new Label(String.format("%,d", auction.getCurrent_price()));
        price.getStyleClass().add("data-money");
        Label state = new Label(auction.getState() == null ? "" : auction.getState().name());
        state.getStyleClass().add("data-pill");
        priceBox.getChildren().addAll(price, state);

        row.getChildren().addAll(titleBox, spacer, leaderBox, priceBox);
        return row;
    }

    // Tạo cụm nhãn nhỏ dạng label/value cho card.
    private VBox valueBox(String name, String value) {
        VBox box = new VBox(4.0);
        box.setAlignment(Pos.CENTER_RIGHT);
        Label key = new Label(name);
        key.getStyleClass().add("data-meta");
        Label val = new Label(value == null || value.isBlank() ? "-" : value);
        val.getStyleClass().add("data-value");
        box.getChildren().addAll(key, val);
        return box;
    }

    // Format khoảng thời gian của phiên để hiển thị gọn trong card.
    private String formatTimeRange(Auction auction) {
        String start = auction.getStartTime() == null ? "?" : auction.getStartTime().toLocalDate() + " " + auction.getStartTime().toLocalTime();
        String end = auction.getEndTime() == null ? "?" : auction.getEndTime().toLocalDate() + " " + auction.getEndTime().toLocalTime();
        return start + " -> " + end;
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
            giaLabel.setText(String.valueOf(item.getBeginPrice()));
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
        Task<LoadAuctionData> task = new Task<>() {
            @Override
            // Hàm chạy trong background task: lấy phiên active và map thông tin item liên quan.
            protected LoadAuctionData call() {
                List<Auction> auctions = auctionService.getActive();
                List<Item> items = itemService.listAll();
                Map<Long, String> itemNames = new HashMap<>();
                Map<Long, Item> itemsById = new HashMap<>();

                for (Item item : items) {
                    itemNames.put(item.getId(), item.getFullname());
                    itemsById.put(item.getId(), item);
                }

                return new LoadAuctionData(auctions, itemNames, itemsById);
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

            applyFilters();
        });

        task.setOnFailed(event -> {
            allAuctions.clear();
            itemNameById.clear();
            itemById.clear();
            clearSelectedItem();
            applyFilters();
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

        Auction selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || !rows.contains(selected)) {
            table.getSelectionModel().clearSelection();
            clearSelectedItem();
        }
    }

    // Kiểm tra trạng thái phiên có khớp lựa chọn lọc không.
    private boolean matchesState(Auction auction, String selectedState) {
        if (selectedState == null || selectedState.equals(ALL_FILTER)) {
            return true;
        }
        return auction.getState() != null && auction.getState().name().equals(selectedState);
    }

    // Kiểm tra từ khóa tìm kiếm theo ID phiên, ID item, tên item và leader.
    private boolean matchesSearch(Auction auction, String query) {
        if (query.isEmpty()) {
            return true;
        }

        String itemName = itemNameById.getOrDefault(auction.getItem_id(), "");
        String target = String.join(" ",
                String.valueOf(auction.getId()),
                String.valueOf(auction.getItem_id()),
                itemName,
                String.valueOf(auction.getCurrent_user_id()),
                auction.getState() == null ? "" : auction.getState().name()
        );
        return normalize(target).contains(query);
    }

    // Chuẩn hóa chuỗi để tìm kiếm không phân biệt hoa thường.
    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    // Quay lại dashboard đúng với role hiện tại.
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

    // Điều hướng tới màn duyệt/quản lý item, chỉ cho Admin.
    @FXML
    public void goToBrowseItems(ActionEvent actionEvent) throws IOException {
        if (UserAccount.getCurrentRole() != UserRole.ADMIN) {
            AlertUtil.showError("Chuc nang nay chi danh cho Admin.");
            return;
        }
        switchScene(actionEvent, "/com/template/hellfx/ItemBrowse.fxml");
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

        // Gói dữ liệu phiên + item trả về từ background task tải danh sách đấu giá.
        private LoadAuctionData(List<Auction> auctions, Map<Long, String> itemNames, Map<Long, Item> itemsById) {
            this.auctions = auctions;
            this.itemNames = itemNames;
            this.itemsById = itemsById;
        }
    }
}
