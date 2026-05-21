package Server.Controller;

import Client.Controller.UILogin;
import Common.DataBase.entities.Item;
import Common.Enum.ItemStatus;
import Server.service.ItemService;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

public class AdminItemManagementController {

    @FXML private ListView<Item> table;
    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> statusFilter;
    @FXML private TextField ownerSearchField;

    private final ItemService itemService = new ItemService();
    private final List<Item> allItems = new ArrayList<>();
    private static final String ALL_FILTER = "Tat ca";
    private boolean loading = false;

    // JavaFX tự gọi sau khi load FXML: cấu hình bảng và tải danh sách toàn bộ item.
    @FXML
    public void initialize() {
        configureList();
        configureFilters();
        loadAllItems();
    }

    // Cấu hình danh sách item dạng card để thay bảng cứng.
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

    // Tạo một card item cho màn quản lý vật phẩm Admin.
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
        Label description = new Label(nullToText(item.getDescription(), "Khong co mo ta"));
        description.getStyleClass().add("product-description");
        description.setWrapText(true);
        HBox meta = new HBox(10.0);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label id = new Label("ID #" + item.getId());
        id.getStyleClass().add("data-meta");
        Label owner = new Label("Seller #" + item.getOwner_user_id());
        owner.getStyleClass().add("data-meta");
        Label note = new Label(nullToText(item.getMota(), ""));
        note.getStyleClass().add("data-meta");
        meta.getChildren().addAll(id, owner, note);
        main.getChildren().addAll(title, description, meta);

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

    // Tải lại toàn bộ danh sách item; bảng tự cuộn nên không cần phân trang.
    public void loadAllItems() {
        loadItemsAsync();
    }

    // Cấu hình tìm kiếm theo tên/ID/mô tả, lọc trạng thái và lọc ID người bán.
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
        if (ownerSearchField != null) {
            ownerSearchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        }
    }

    // Quay lại dashboard của Admin.
    public void backToAdminDashboard(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/dashboard - Admin.fxml");
    }

    // Tải toàn bộ item trên background thread để không khóa UI.
    private void loadItemsAsync() {
        if (loading) return;
        loading = true;

        Task<List<Item>> task = new Task<>() {
            @Override
            // Hàm chạy trong background task: lấy toàn bộ item để lọc và cuộn trực tiếp.
            protected List<Item> call() {
                return itemService.listAll();
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

        Thread worker = new Thread(task, "admin-item-page-load");
        worker.setDaemon(true);
        worker.start();
    }

    // Lọc item theo từ khóa, trạng thái và ID người bán.
    private void applyFilters() {
        String query = normalize(searchField == null ? "" : searchField.getText());
        String status = statusFilter == null ? ALL_FILTER : statusFilter.getValue();
        String ownerQuery = normalize(ownerSearchField == null ? "" : ownerSearchField.getText());
        List<Item> rows = new ArrayList<>();

        for (Item item : allItems) {
            if (!matchesStatus(item, status)) {
                continue;
            }
            if (!matchesOwner(item, ownerQuery)) {
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

    // Kiểm tra trạng thái item có khớp bộ lọc không.
    private boolean matchesStatus(Item item, String selectedStatus) {
        if (selectedStatus == null || selectedStatus.equals(ALL_FILTER)) {
            return true;
        }
        return item.getStatus() != null && item.getStatus().name().equals(selectedStatus);
    }

    // Kiểm tra ID người bán có chứa từ khóa lọc không.
    private boolean matchesOwner(Item item, String ownerQuery) {
        return ownerQuery.isEmpty() || String.valueOf(item.getOwner_user_id()).contains(ownerQuery);
    }

    // Kiểm tra keyword theo ID, tên, giá, trạng thái, mô tả và phản hồi.
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
}
