package Server.Controller;

import Common.DataBase.entities.Item;
import Common.Enum.ItemStatus;
import Common.Enum.UserRole;
import Common.Model.user.UserAccount;
import Server.service.AuctionService;
import Server.service.ItemService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class    ItemBrowseController {
    @FXML private ListView<Item> table;
    @FXML private ChoiceBox<String> statusFilter;
    @FXML private TextField rejectReason;
    @FXML private TextField endTime;
    @FXML private Label lblRole;
    @FXML private Label lblTotal;
    @FXML private HBox adminInputBox;
    @FXML private HBox adminActionBox;

    private final ItemService itemService = new ItemService();
    private final AuctionService auctionService = new AuctionService();

    private static final String ALL = "TAT_CA";

    // JavaFX tự gọi sau khi load FXML: cấu hình bảng, bộ lọc trạng thái và tải dữ liệu item.
    @FXML
    public void initialize(){
        configureList();

        // Bộ lọc trạng thái item.
        if (statusFilter != null){
            ObservableList<String> options = FXCollections.observableArrayList(ALL, ItemStatus.PENDING.name(), ItemStatus.APPROVED.name(), ItemStatus.IN_AUCTION.name(), ItemStatus.SOLD.name(), ItemStatus.CANCELED.name());
            statusFilter.setItems(options);
            UserRole role = UserAccount.getCurrentRole();
            if (role == UserRole.ADMIN){
                statusFilter.setValue(ItemStatus.PENDING.name()); //ADMIN mac dinh xem Pending de duyet
            }else{
                statusFilter.setValue(ItemStatus.APPROVED.name()); // Role khac se xem APPROVED
            }
            statusFilter.setOnAction(e -> loadData());
        }
        if (lblRole != null){
            UserRole role = UserAccount.getCurrentRole();
            lblRole.setText("Vai trò: "+(role == null ? "?" : role.name()));
        }
        configureRoleUi();
        loadData();
    }

    // Cấu hình danh sách item theo dạng card mềm.
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

    // Tạo card item cho màn duyệt/xem sản phẩm.
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
        Label description = new Label(nullToText(item.getDescription(), "Khong co thong tin"));
        description.getStyleClass().add("product-description");
        description.setWrapText(true);
        HBox meta = new HBox(10.0);
        Label id = new Label("ID #" + item.getId());
        id.getStyleClass().add("data-meta");
        Label minStep = new Label("Buoc gia " + formatMoney(getEffectiveMinIncrement(item)));
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

    // Trả chuỗi dự phòng khi dữ liệu null/rỗng.
    private String nullToText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    // Item cũ có thể chưa có bước giá, fallback 1 để khớp BidService.
    private long getEffectiveMinIncrement(Item item) {
        return item == null || item.getMinIncrement() <= 0 ? 1L : item.getMinIncrement();
    }

    private String formatMoney(long amount) {
        return String.format("%,d", amount);
    }

    // Cấu hình các input/nút chỉ dành cho Admin.
    private void configureRoleUi() {
        boolean isAdmin = UserAccount.getCurrentRole() == UserRole.ADMIN;
        setVisibleManaged(adminInputBox, isAdmin);
        setVisibleManaged(adminActionBox, isAdmin);
    }

    // Set đồng thời visible và managed để node ẩn không chiếm chỗ layout.
    private void setVisibleManaged(Node node, boolean visible) {
        if (node == null) return;
        node.setVisible(visible);
        node.setManaged(visible);
    }

    // Tải dữ liệu item theo bộ lọc trạng thái và giới hạn dữ liệu theo role hiện tại.
    @FXML
    public void loadData(){
        try {
            String filter = (statusFilter == null || statusFilter.getValue() == null) ? ItemStatus.PENDING.name() : statusFilter.getValue();
            List<Item> data;
            if (ALL.equals(filter)){
                data = mergeAll();
            }
            else {
                ItemStatus status = ItemStatus.valueOf(filter);
                data = listByStatusAuto(status);
            }

            // Seller chỉ xem được item của mình.
            UserRole role = UserAccount.getCurrentRole();
            if (role == UserRole.SELLER){
                long myId = UserAccount.getUserId();
                data.removeIf(it -> it.getOwner_user_id() != myId);
            }

            table.getItems().setAll(data);

            if (lblRole != null) {
                lblTotal.setText("Tổng: " + data.size() + " sản phẩm");
            }
        }catch (Exception e){
            showWarning("Không tải được danh sách: "+e.getMessage());
        }
    }

    // Gộp các danh sách item cơ bản để phục vụ bộ lọc TẤT CẢ hoặc lọc trạng thái phụ.
    private List<Item> mergeAll(){
        List<Item> all = itemService.listPending();
        all.addAll(itemService.listApproved());
        return all;
    }

    // Lấy danh sách item theo trạng thái, ưu tiên service chuyên biệt khi có.
    private List<Item> listByStatusAuto(ItemStatus status){
        switch (status){
            case PENDING:
                return itemService.listPending();
            case APPROVED:
                return itemService.listApproved();
            default:
                List<Item> base = mergeAll();
                base.removeIf(i-> i.getStatus() != status);
                return base;
        }
    }

    // Admin bấm Approve: duyệt item PENDING và tạo luôn phiên đấu giá theo số giờ nhập.
    @FXML
    public void onApprove() {
        if (!requireAdmin()) return;

        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Chua chon san pham.");
            return;
        }

        // Yêu cầu nhập số giờ đấu giá.
        int hours;
        try{
            hours = Integer.parseInt(endTime == null ? "" :endTime.getText().trim());
            if (hours <= 0){
                showWarning("Số giờ đấu giá phải > 0.");
                return;
            }
        }catch (NumberFormatException e){
            showWarning("Vui lòng nhập số giờ hợp lệ cho phiên đấu giá");
            return;
        }

        try{
            LocalDateTime endAt = LocalDateTime.now().plus(Duration.ofHours(hours));
            // Service xử lý trọn workflow duyệt item PENDING rồi mở phiên.
            auctionService.approveAndCreateSession(selected.getId(), endAt);

            showInfo("Đã duyệt sản phẩm ID" +selected.getId() + " và mở phiên đấu giá.");
            loadData();
        } catch (Exception e){
            showWarning("Duyệt thất bại: " + e.getMessage());
        }
    }

    // Admin bấm Reject: từ chối item PENDING và lưu lý do phản hồi.
    @FXML
    public void onReject() {
        if(!requireAdmin()) return;

        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null){
            showWarning("Chưa chọn sản phẩm");
            return;
        }
        if(selected.getStatus() != ItemStatus.PENDING){
            showWarning("Chỉ từ chối được item đang ở ngoài trạng thái Pending.");
            return;
        }
        String reason = rejectReason == null ? "" :rejectReason.getText();

        try{
            itemService.reject(selected.getId(), reason);
            showInfo("Đã từ chối sản phẩm ID "+selected.getId() + ".");
            if (rejectReason != null){
                rejectReason.clear();
            }
            loadData();
        }catch (Exception e){
            showWarning("Từ chối thất bại: "+e.getMessage());
        }
    }

    // Với item đã APPROVED, Admin mở phiên đấu giá mới theo số giờ nhập.
    @FXML
    public void onCreateSession(){
        if (!requireAdmin()) return;

        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null){
            showWarning("Chưa chọn sản phẩm");
            return;
        }
        int hours;
        try {
            hours = Integer.parseInt(endTime == null ? "" : endTime.getText().trim());
            if (hours <= 0) {
                showWarning("Số giờ phải > 0.");
                return;
            }
        } catch (NumberFormatException e) {
            showWarning("Vui lòng nhập số giờ hợp lệ.");
            return;
        }

        try {
            LocalDateTime endAt = LocalDateTime.now().plus(Duration.ofHours(hours));
            // Service tự kiểm tra item phải ở trạng thái APPROVED trước khi mở phiên.
            auctionService.createSession(selected.getId(), endAt);
            showInfo("Đã mở phiên đấu giá cho sản phẩm ID " + selected.getId() + ".");
            loadData();
        } catch (Exception e) {
            showWarning("Mở phiên thất bại: " + e.getMessage());
        }
    }

    // Trở về dashboard phù hợp với role hiện tại.
    @FXML
    public void onBack(ActionEvent actionEvent) throws IOException {
        UserRole role = UserAccount.getCurrentRole();
        String target;
        if (role == UserRole.ADMIN) {
            target = "/com/template/hellfx/dashboard - Admin.fxml";
        } else if (role == UserRole.SELLER) {
            target = "/com/template/hellfx/dashboard - Seller.fxml";
        } else {
            target = "/com/template/hellfx/dashboard-Bidder.fxml";
        }
        switchScene(actionEvent, target);
    }

    // Kiểm tra quyền Admin trước khi cho phép thao tác duyệt/từ chối/mở phiên.
    private boolean requireAdmin() {
        UserRole role = UserAccount.getCurrentRole();
        if (role != UserRole.ADMIN) {
            showWarning("Chuc nang nay chi danh cho Admin.");
            return false;
        }
        return true;
    }

    // Thay root scene hiện tại bằng màn FXML mới.
    private void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene currentScene = stage.getScene();
        if (currentScene == null) {
            stage.setScene(new Scene(root));
        } else {
            currentScene.setRoot(root);
        }
        stage.show();
    }

    // Hiện popup cảnh báo khi input/quyền/thao tác service không hợp lệ.
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Thong bao");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Hiện popup thông tin khi thao tác item thành công.
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thong bao");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
