package Server.Controller;

import Client.Controller.UILogin;
import Common.DataBase.entities.Item;
import Common.Enum.ItemStatus;
import Common.Model.user.UserAccount;
import Server.service.ItemService;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SellerProductController {

    @FXML private TextField itemName;
    @FXML private TextField itemPrice;
    @FXML private TextField itemDescription;

    @FXML private ListView<Item> table;
    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> statusFilter;

    private final ItemService itemService = new ItemService();
    private final List<Item> allItems = new ArrayList<>();
    private static final String ALL_FILTER = "Tat ca";
    private boolean loading = false;

    // JavaFX tự gọi sau khi load FXML: cấu hình cột bảng và tải danh sách item của seller.
    @FXML
    public void initialize() {
        configureList();
        configureFilters();
        loadMyItems();
    }

    // Cấu hình danh sách sản phẩm của seller theo dạng card mềm.
    private void configureList() {
        table.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(null);
                setGraphic(createItemCard(item));
            }
        });
    }

    // Tạo card hiển thị trạng thái một sản phẩm seller đã gửi.
    private Node createItemCard(Item item) {
        HBox row = new HBox(14.0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14.0, 16.0, 14.0, 16.0));
        row.getStyleClass().add("data-row");

        VBox main = new VBox(5.0);
        HBox.setHgrow(main, Priority.ALWAYS);
        Label title = new Label(nullToText(item.getFullname(), "Item " + item.getId()));
        title.getStyleClass().add("data-title");
        title.setWrapText(true);
        Label note = new Label(nullToText(item.getMota(), "Chua co phan hoi"));
        note.getStyleClass().add("product-description");
        note.setWrapText(true);
        HBox meta = new HBox(10.0);
        Label id = new Label("ID #" + item.getId());
        id.getStyleClass().add("data-meta");
        Label description = new Label(nullToText(item.getDescription(), ""));
        description.getStyleClass().add("data-meta");
        meta.getChildren().addAll(id, description);
        main.getChildren().addAll(title, note, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox value = new VBox(6.0);
        value.setAlignment(Pos.CENTER_RIGHT);
        Label price = new Label(String.format("%,d", item.getBeginPrice()));
        price.getStyleClass().add("data-money");
        Label status = new Label(item.getStatus() == null ? "" : item.getStatus().name());
        status.getStyleClass().add("data-pill");
        value.getChildren().addAll(price, status);

        row.getChildren().addAll(main, spacer, value);
        return row;
    }

    // Trả chuỗi dự phòng khi dữ liệu null/rỗng.
    private String nullToText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    // Tải lại toàn bộ danh sách sản phẩm của seller, bảng tự cuộn nên không cần phân trang.
    public void loadMyItems() {
        loadItemsAsync();
    }

    // Cấu hình tìm kiếm theo tên/ID/ghi chú và lọc trạng thái item.
    private void configureFilters() {
        if (statusFilter != null) {
            List<String> options = new ArrayList<>();
            options.add(ALL_FILTER);
            for (ItemStatus status : ItemStatus.values()) {
                options.add(status.name());
            }
            statusFilter.setItems(FXCollections.observableArrayList(options));
            statusFilter.setValue(ALL_FILTER);
            statusFilter.setOnAction(event -> applyFilters());
        }

        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        }
    }

    // Tải dữ liệu sản phẩm trên background thread để không khóa UI.
    private void loadItemsAsync() {
        if (loading) return;
        loading = true;

        Task<List<Item>> task = new Task<>() {
            @Override
            // Hàm chạy trong background task: lấy toàn bộ sản phẩm của seller để lọc/cuộn trực tiếp.
            protected List<Item> call() {
                long ownerId = UserAccount.getUserId();
                return itemService.listByOwner(ownerId);
            }
        };

        task.setOnSucceeded(event -> {
            allItems.clear();
            allItems.addAll(task.getValue());
            applyFilters();
            loading = false;
        });

        task.setOnFailed(event -> {
            allItems.clear();
            applyFilters();
            loading = false;
        });

        Thread worker = new Thread(task, "seller-item-page-load");
        worker.setDaemon(true);
        worker.start();
    }

    // Lọc sản phẩm theo từ khóa và trạng thái.
    private void applyFilters() {
        String query = normalize(searchField == null ? "" : searchField.getText());
        String status = statusFilter == null ? ALL_FILTER : statusFilter.getValue();
        List<Item> rows = new ArrayList<>();

        for (Item item : allItems) {
            if (!matchesStatus(item, status)) {
                continue;
            }
            if (!matchesSearch(item, query)) {
                continue;
            }
            rows.add(item);
        }

        table.getItems().setAll(rows);
        table.refresh();
    }

    // Kiểm tra trạng thái sản phẩm có khớp bộ lọc không.
    private boolean matchesStatus(Item item, String selectedStatus) {
        if (selectedStatus == null || selectedStatus.equals(ALL_FILTER)) {
            return true;
        }
        return item.getStatus() != null && item.getStatus().name().equals(selectedStatus);
    }

    // Kiểm tra keyword theo ID, tên, giá, trạng thái, mô tả và ghi chú phản hồi.
    private boolean matchesSearch(Item item, String query) {
        if (query.isEmpty()) {
            return true;
        }

        String target = String.join(" ",
                String.valueOf(item.getId()),
                item.getFullname() == null ? "" : item.getFullname(),
                String.valueOf(item.getBeginPrice()),
                item.getStatus() == null ? "" : item.getStatus().name(),
                item.getDescription() == null ? "" : item.getDescription(),
                item.getMota() == null ? "" : item.getMota()
        );
        return normalize(target).contains(query);
    }

    // Chuẩn hóa chuỗi để tìm kiếm không phân biệt hoa thường.
    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
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
