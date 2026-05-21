package Server.Controller;

import Client.Controller.UILogin;
import Common.DataBase.entities.Item;
import Server.service.AuctionService;
import Server.service.ItemService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ApproveController {

    @FXML private TableView<Item> table;
    @FXML private TableColumn<Item, Long> colId;
    @FXML private TableColumn<Item, String> colName;
    @FXML private TableColumn<Item, Long> colPrice;
    @FXML private TableColumn<Item, String> colDetail;
    @FXML private TableColumn<Item, String> colDescription;
    @FXML private TableColumn<Item, String> colStatus;

    @FXML private VBox approvePane;
    @FXML private VBox rejectPane;
    @FXML private TextField startTime;
    @FXML private TextField endTime;
    @FXML private TextField rejectReason;

    private final AuctionService auctionService = new AuctionService();
    private final ItemService itemService = new ItemService();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private ActionMode actionMode = ActionMode.NONE;

    // JavaFX tự gọi sau khi load FXML: cấu hình bảng, ẩn panel nhập liệu và tải item chờ duyệt.
    @FXML
    public void initialize() {
        colId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getId()));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullname()));
        colPrice.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getBeginPrice()));
        colDescription.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toString()));
        colDetail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMota()));

        setPaneVisible(approvePane, false);
        setPaneVisible(rejectPane, false);
        loadData();
    }

    // Tải danh sách item đang ở trạng thái chờ duyệt lên bảng.
    public void loadData() {
        table.getItems().setAll(itemService.listPending());
    }

    // Xử lý nút chấp nhận: lần đầu mở form thời gian, lần tiếp theo thực hiện duyệt.
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
        duyetsp();
    }

    // Duyệt item đã chọn, validate thời gian và tạo phiên đấu giá tương ứng.
    private void duyetsp() {
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
            // Duyệt sản phẩm.
            itemService.approve(selected.getId());
            showInfo("Đã chấp nhận sản phẩm ID: " + selected.getId());
            // Tạo phiên đấu giá sau khi sản phẩm được duyệt.
            auctionService.createSession(selected.getId(),end);
            if (endTime != null) endTime.clear();
            loadData();
        } catch (Exception e) {
            showWarning("Chấp nhận thất bại: " + e.getMessage());
        }
    }

    // Xử lý nút từ chối: lần đầu mở form nhập lý do, lần tiếp theo thực hiện từ chối.
    public void onTuChoi() {
        if (actionMode != ActionMode.REJECT) {
            actionMode = ActionMode.REJECT;
            setPaneVisible(rejectPane, true);
            setPaneVisible(approvePane, false);
            return;
        }
        tuchoi();
    }

    // Từ chối item đã chọn và lưu lý do phản hồi cho seller.
    private void tuchoi() {
        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Chưa chọn sản phẩm.");
            return;
        }

        String reason = rejectReason == null ? "" : rejectReason.getText();
        try {
            itemService.reject(selected.getId(), reason);
            showInfo("Đã từ chối. Seller sẽ nhận trạng thái CANCELED và lý do phản hồi.");
            if (rejectReason != null) rejectReason.clear();
            loadData();
        } catch (Exception e) {
            showWarning("Từ chối thất bại: " + e.getMessage());
        }
    }

    // Quay lại dashboard của Admin.
    public void trolai(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/dashboard - Admin.fxml");
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

    // Ẩn/hiện panel nhập liệu và bỏ chiếm chỗ layout khi panel bị ẩn.
    private void setPaneVisible(VBox pane, boolean visible) {
        if (pane == null) return;
        pane.setVisible(visible);
        pane.setManaged(visible);
    }

    // Hiện popup cảnh báo cho lỗi validate hoặc lỗi service.
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Hiện popup thông tin khi duyệt hoặc từ chối thành công.
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private enum ActionMode { NONE, APPROVE, REJECT }
}
