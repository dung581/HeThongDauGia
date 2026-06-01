package Server.Controller;

import Client.Controller.UILogin;
import Common.DataBase.entities.Item;
import Server.service.AuctionService;
import Server.service.ItemService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PendingItemController {

    @FXML private ListView<Item> table;
    @FXML private VBox approvePane;
    @FXML private VBox rejectPane;
    @FXML private TextField startTime;
    @FXML private TextField endTime;
    @FXML private TextField rejectReason;

    private final AuctionService auctionService = new AuctionService();
    private final ItemService itemService = new ItemService();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private ActionMode actionMode = ActionMode.NONE;

    // JavaFX tự gọi sau khi load FXML: dựng list pending, ẩn form phụ và tải item chờ duyệt.
    @FXML
    public void initialize() {
        configureList();
        setPaneVisible(approvePane, false);
        setPaneVisible(rejectPane, false);
        loadData();
    }

    // Dạy ListView vẽ mỗi item PENDING thành một card giống màn dashboard mới.
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
                setGraphic(createPendingItemCard(item));
            }
        });
    }

    // Card item chờ duyệt: trái là tên/mô tả, phải là giá và trạng thái.
    private Node createPendingItemCard(Item item) {
        HBox row = new HBox(14.0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14.0, 16.0, 14.0, 16.0));
        row.getStyleClass().add("data-row");

        VBox main = new VBox(5.0);
        HBox.setHgrow(main, Priority.ALWAYS);
        Label title = new Label(nullToText(item.getFullname(), "Item " + item.getId()));
        title.getStyleClass().add("data-title");
        title.setWrapText(true);
        Label description = new Label(nullToText(item.getDescription(), "Không có thông tin"));
        description.getStyleClass().add("product-description");
        description.setWrapText(true);

        HBox meta = new HBox(10.0);
        Label id = new Label("ID #" + item.getId());
        id.getStyleClass().add("data-meta");
        Label minStep = new Label("Bước giá " + formatMoney(getEffectiveMinIncrement(item)));
        minStep.getStyleClass().add("data-meta");
        Label note = new Label(nullToText(item.getMota(), ""));
        note.getStyleClass().add("data-meta");
        meta.getChildren().addAll(id, minStep, note);
        main.getChildren().addAll(title, description, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox value = new VBox(6.0);
        value.setAlignment(Pos.CENTER_RIGHT);
        Label price = new Label(formatMoney(item.getBeginPrice()));
        price.getStyleClass().add("data-money");
        Label status = new Label(item.getStatus() == null ? "" : item.getStatus().name());
        status.getStyleClass().add("data-pill");
        value.getChildren().addAll(price, status);

        row.getChildren().addAll(main, spacer, value);
        return row;
    }

    // Chỉ tải item PENDING vì màn này dành riêng cho duyệt sản phẩm chờ.
    public void loadData() {
        table.getItems().setAll(itemService.listPending());
    }

    // Lần đầu bấm Chấp nhận thì mở form thời gian; lần sau mới thực hiện duyệt.
    @FXML
    public void onChapNhan() {
        if (actionMode != ActionMode.APPROVE) {
            actionMode = ActionMode.APPROVE;
            setPaneVisible(approvePane, true);
            setPaneVisible(rejectPane, false);
            if (startTime != null && (startTime.getText() == null || startTime.getText().isBlank())) {
                startTime.setText(LocalDateTime.now().withSecond(0).withNano(0).format(formatter));
            }
            return;
        }
        approveSelectedItem();
    }

    // Duyệt item đã chọn và mở phiên đấu giá với thời gian kết thúc đã nhập.
    private void approveSelectedItem() {
        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Chưa chọn sản phẩm.");
            return;
        }

        try {
            LocalDateTime start = LocalDateTime.parse(startTime.getText().trim(), formatter);
            LocalDateTime end = LocalDateTime.parse(endTime.getText().trim(), formatter);
            if (!end.isAfter(start)) {
                showWarning("Thời gian kết thúc phải sau thời gian bắt đầu.");
                return;
            }

            auctionService.approveAndCreateSession(selected.getId(), end);
            showInfo("Đã chấp nhận sản phẩm ID: " + selected.getId());
            resetActionForm();
            loadData();
        } catch (Exception e) {
            showWarning("Chấp nhận thất bại: " + e.getMessage());
        }
    }

    // Lần đầu bấm Từ chối thì mở form lý do; lần sau mới thực hiện từ chối.
    @FXML
    public void onTuChoi() {
        if (actionMode != ActionMode.REJECT) {
            actionMode = ActionMode.REJECT;
            setPaneVisible(rejectPane, true);
            setPaneVisible(approvePane, false);
            return;
        }
        rejectSelectedItem();
    }

    // Từ chối item đã chọn và lưu lý do phản hồi cho seller.
    private void rejectSelectedItem() {
        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Chưa chọn sản phẩm.");
            return;
        }

        String reason = rejectReason == null ? "" : rejectReason.getText();
        try {
            itemService.reject(selected.getId(), reason);
            showInfo("Đã từ chối sản phẩm ID: " + selected.getId());
            resetActionForm();
            loadData();
        } catch (Exception e) {
            showWarning("Từ chối thất bại: " + e.getMessage());
        }
    }

    // Quay lại dashboard của Admin.
    @FXML
    public void trolai(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/dashboard - Admin.fxml");
    }

    // Reset panel phụ sau khi thao tác xong để quay về trạng thái giống màn ban đầu.
    private void resetActionForm() {
        actionMode = ActionMode.NONE;
        setPaneVisible(approvePane, false);
        setPaneVisible(rejectPane, false);
        if (endTime != null) endTime.clear();
        if (rejectReason != null) rejectReason.clear();
    }

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

    // Ẩn node khỏi layout hoàn toàn, không để lại khoảng trống.
    private void setPaneVisible(Node pane, boolean visible) {
        if (pane == null) return;
        pane.setVisible(visible);
        pane.setManaged(visible);
    }

    private String nullToText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private long getEffectiveMinIncrement(Item item) {
        return item == null || item.getMinIncrement() <= 0 ? 1L : item.getMinIncrement();
    }

    private String formatMoney(long amount) {
        return String.format("%,d", amount);
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private enum ActionMode { NONE, APPROVE, REJECT }
}
