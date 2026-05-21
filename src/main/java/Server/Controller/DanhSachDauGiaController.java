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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DanhSachDauGiaController {
    @FXML private TableView<Auction> table;
    @FXML private TableColumn<Auction, Long> colId;
    @FXML private TableColumn<Auction, Long> colItemId;
    @FXML private TableColumn<Auction, String> colItemName;
    @FXML private TableColumn<Auction, AuctionState> colState;
    @FXML private TableColumn<Auction, LocalDateTime> colStartTime;
    @FXML private TableColumn<Auction, LocalDateTime> colEndTime;
    @FXML private TableColumn<Auction, Long> colCurrentPrice;
    @FXML private TableColumn<Auction, Long> colCurrentUserId;

    @FXML private Label idLabel;
    @FXML private Label tenLabel;
    @FXML private Label giaLabel;
    @FXML private Label trangthaiLabel;
    @FXML private Label thongtinLabel;
    @FXML private Label lblPageInfo;
    @FXML private Label accountSectionLabel;

    @FXML private Node browseItemsNav;
    @FXML private Node uploadItemNav;
    @FXML private Node depositNav;
    @FXML private Button accountNavButton;
    @FXML private TextField txtPageInput;

    private final AuctionService auctionService = new AuctionService();
    private final ItemService itemService = new ItemService();
    private final List<Auction> allAuctions = new ArrayList<>();
    private final Map<Long, Item> itemById = new HashMap<>();
    private final Map<Long, String> itemNameById = new HashMap<>();

    private static final int PAGE_SIZE = 8;
    private int currentPage = 1;

    // JavaFX tự gọi sau khi load FXML: cấu hình UI, bảng, selection và tải danh sách phiên.
    @FXML
    public void initialize() {
        configureRoleUi();
        configureTable();
        configureSelection();
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

    // Gán các cột TableView với dữ liệu Auction và tên item tương ứng.
    private void configureTable() {
        colId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getId()));
        colItemId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getItem_id()));
        colItemName.setCellValueFactory(data ->
                new SimpleStringProperty(itemNameById.getOrDefault(data.getValue().getItem_id(), "N/A")));
        colState.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getState()));
        colStartTime.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getStartTime()));
        colEndTime.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getEndTime()));
        colCurrentPrice.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getCurrent_price()));
        colCurrentUserId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getCurrent_user_id()));
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

            renderPage(1);
        });

        task.setOnFailed(event -> {
            allAuctions.clear();
            itemNameById.clear();
            itemById.clear();
            clearSelectedItem();
            renderPage(1);
        });

        Thread worker = new Thread(task, "auction-list-load");
        worker.setDaemon(true);
        worker.start();
    }

    // Chuyển bảng về trang đầu tiên.
    @FXML
    public void goFirstPage() {
        renderPage(1);
    }

    // Chuyển bảng về trang trước.
    @FXML
    public void goPrevPage() {
        renderPage(currentPage - 1);
    }

    // Chuyển bảng sang trang tiếp theo.
    @FXML
    public void goNextPage() {
        renderPage(currentPage + 1);
    }

    // Chuyển bảng sang trang cuối cùng.
    @FXML
    public void goLastPage() {
        renderPage(getTotalPages());
    }

    // Đọc số trang người dùng nhập và render trang tương ứng.
    @FXML
    public void goToPage() {
        if (txtPageInput == null || txtPageInput.getText() == null) {
            return;
        }
        try {
            renderPage(Integer.parseInt(txtPageInput.getText().trim()));
        } catch (NumberFormatException ignored) {
            renderPage(currentPage);
        }
    }

    // Cắt danh sách phiên theo trang và đổ dữ liệu lên TableView.
    private void renderPage(int page) {
        int totalPages = getTotalPages();
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        currentPage = page;

        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, allAuctions.size());
        List<Auction> rows = fromIndex >= toIndex ? List.of() : allAuctions.subList(fromIndex, toIndex);

        table.getItems().setAll(rows);
        table.refresh();

        if (lblPageInfo != null) {
            lblPageInfo.setText("Trang " + currentPage + " / " + totalPages);
        }
    }

    // Tính tổng số trang dựa trên số phiên đang có.
    private int getTotalPages() {
        if (allAuctions.isEmpty()) {
            return 1;
        }
        return (allAuctions.size() + PAGE_SIZE - 1) / PAGE_SIZE;
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
