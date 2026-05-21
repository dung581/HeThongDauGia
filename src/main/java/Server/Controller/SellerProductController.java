package Server.Controller;

import Client.Controller.UILogin;
import Common.DataBase.entities.Item;
import Common.Model.user.UserAccount;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class SellerProductController {

    @FXML private TextField itemName;
    @FXML private TextField itemPrice;
    @FXML private TextField itemDescription;

    @FXML private TableView<Item> table;
    @FXML private TableColumn<Item, Long> colId;
    @FXML private TableColumn<Item, String> colName;
    @FXML private TableColumn<Item, Long> colPrice;
    @FXML private TableColumn<Item, String> colStatus;
    @FXML private TableColumn<Item, String> colReason;
    @FXML private Label lblPageInfo;
    @FXML private TextField txtPageInput;

    private final ItemService itemService = new ItemService();
    private static final int PAGE_SIZE = 8;
    private int currentPage = 1;
    private int totalItems = 0;
    private boolean loading = false;

    // JavaFX tự gọi sau khi load FXML: cấu hình cột bảng và tải danh sách item của seller.
    @FXML
    public void initialize() {
        colId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getId()));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullname()));
        colPrice.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getBeginPrice()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        colReason.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMota() == null ? "" : data.getValue().getMota()));
        loadMyItems();
    }

    // Tải lại danh sách sản phẩm của seller từ trang đầu tiên.
    public void loadMyItems() {
        loadPageAsync(1);
    }

    // Chuyển tới trang đầu tiên của bảng sản phẩm.
    @FXML public void goFirstPage() { loadPageAsync(1); }
    // Chuyển tới trang trước của bảng sản phẩm.
    @FXML public void goPrevPage() { loadPageAsync(currentPage - 1); }
    // Chuyển tới trang tiếp theo của bảng sản phẩm.
    @FXML public void goNextPage() { loadPageAsync(currentPage + 1); }
    // Chuyển tới trang cuối cùng của bảng sản phẩm.
    @FXML public void goLastPage() { loadPageAsync(getTotalPages()); }

    // Đọc số trang người dùng nhập và tải trang tương ứng.
    @FXML
    public void goToPage() {
        if (txtPageInput == null || txtPageInput.getText() == null) return;
        try {
            int page = Integer.parseInt(txtPageInput.getText().trim());
            loadPageAsync(page);
        } catch (NumberFormatException ignored) {
            loadPageAsync(currentPage);
        }
    }

    // Đổ dữ liệu một trang lên TableView và cập nhật nhãn phân trang.
    private void renderPage(int page, List<Item> pageRows) {
        int totalPages = getTotalPages();
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        currentPage = page;
        table.getItems().setAll(pageRows);

        if (lblPageInfo != null) {
            lblPageInfo.setText("Trang " + currentPage + " / " + totalPages);
        }
    }

    // Tính tổng số trang dựa trên tổng item và kích thước trang.
    private int getTotalPages() {
        if (totalItems <= 0) return 1;
        return (totalItems + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    // Tải dữ liệu sản phẩm theo trang trên background thread để không khóa UI.
    private void loadPageAsync(int requestedPage) {
        if (loading) return;
        loading = true;
        if (lblPageInfo != null) lblPageInfo.setText("Đang tải...");

        Task<PageData> task = new Task<>() {
            @Override
            // Hàm chạy trong background task: đếm và lấy sản phẩm của seller theo trang.
            protected PageData call() {
                long ownerId = UserAccount.getUserId();
                int total = itemService.countByOwner(ownerId);
                int totalPages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
                int safePage = Math.max(1, Math.min(requestedPage, totalPages));
                List<Item> rows = itemService.listByOwnerPaged(ownerId, safePage, PAGE_SIZE);
                return new PageData(total, safePage, rows);
            }
        };

        task.setOnSucceeded(event -> {
            PageData result = task.getValue();
            totalItems = result.totalItems;
            renderPage(result.page, result.rows);
            loading = false;
        });

        task.setOnFailed(event -> {
            loading = false;
            if (lblPageInfo != null) lblPageInfo.setText("Tải thất bại");
        });

        Thread worker = new Thread(task, "seller-item-page-load");
        worker.setDaemon(true);
        worker.start();
    }

    private static class PageData {
        private final int totalItems;
        private final int page;
        private final List<Item> rows;

        // Gói dữ liệu trả về từ background task khi tải một trang sản phẩm.
        private PageData(int totalItems, int page, List<Item> rows) {
            this.totalItems = totalItems;
            this.page = page;
            this.rows = rows;
        }
    }

    // Xử lý form đăng bán: đọc input, tạo Item và gửi yêu cầu upload qua ItemService.
    public void submitItem() {
        try {
            String name = itemName.getText() == null ? "" : itemName.getText().trim();
            String desc = itemDescription.getText() == null ? "" : itemDescription.getText().trim();
            long price = Long.parseLong(itemPrice.getText().trim());

            if (name.isEmpty()) {
                showWarning("Vui lòng nhập tên sản phẩm.");
                return;
            }

            Item item = new Item();
            item.setFullname(name);
            item.setOwner_user_id(UserAccount.getUserId());
            item.setDescription(desc);
            item.setBeginPrice(price);
            item.setMota("Chờ duyệt");

            itemService.upload(item);
            showInfo("Đã gửi yêu cầu đăng bán. Chờ admin duyệt.");

            itemName.clear();
            itemPrice.clear();
            itemDescription.clear();
            loadMyItems();
        } catch (NumberFormatException e) {
            showWarning("Giá không hợp lệ.");
        } catch (Exception e) {
            showWarning("Đăng bán thất bại: " + e.getMessage());
        }
    }

    // Quay lại dashboard của Seller.
    public void backToSellerDashboard(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/dashboard - Seller.fxml");
    }

    // Thay root scene hiện tại bằng FXML mới và giữ kích thước chuẩn của ứng dụng.
    private void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene currentScene = stage.getScene();
        if (currentScene == null) {
            stage.setScene(new Scene(root, UILogin.APP_WIDTH, UILogin.APP_HEIGHT));
        } else {
            currentScene.setRoot(root);
        }
        stage.show();
    }

    // Hiện popup cảnh báo khi input đăng bán không hợp lệ hoặc service báo lỗi.
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Hiện popup thông tin khi seller gửi sản phẩm thành công.
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
