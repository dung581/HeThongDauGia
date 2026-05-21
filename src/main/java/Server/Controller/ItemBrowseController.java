package Server.Controller;

import Common.DataBase.entities.Item;
import Common.Enum.ItemStatus;
import Common.Enum.UserRole;
import Common.Model.user.UserAccount;
import Server.service.AuctionService;
import Server.service.ItemService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class    ItemBrowseController {
    @FXML private TableView<Item> table;
    @FXML private TableColumn<Item, Long> colId;
    @FXML private TableColumn<Item, String> colName;
    @FXML private TableColumn<Item, Long> colPrice;
    @FXML private TableColumn<Item, String> colDescription;
    @FXML private TableColumn<Item, String> colNote;
    @FXML private TableColumn<Item, String> colStatus;
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
        colId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getId()));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullname()));
        colPrice.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getBeginPrice()));
        colDescription.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription() == null ? "" : data.getValue().getDescription()));
        colNote.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMota() == null ? "" : data.getValue().getMota()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus() == null ? "" : data.getValue().getStatus().name()));

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
    public void onApprove() {
        if (!requireAdmin()) return;

        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Chua chon san pham.");
            return;
        }

        if (selected.getStatus() != ItemStatus.PENDING) {
            showWarning("Chi duyet duoc item dang o trang thai PENDING.");
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
            itemService.approve(selected.getId());

            LocalDateTime endAt = LocalDateTime.now().plus(Duration.ofHours(hours));
            auctionService.createSession(selected.getId(), endAt);

            showInfo("Đã duyệt sản phẩm ID" +selected.getId() + " và mở phiên đấu giá.");
            loadData();
        } catch (Exception e){
            showWarning("Duyệt thất bại: " + e.getMessage());
        }
    }

    // Admin bấm Reject: từ chối item PENDING và lưu lý do phản hồi.
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
    public void onCreateSession(){
        if (!requireAdmin()) return;

        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null){
            showWarning("Chưa chọn sản phẩm");
            return;
        }
        if (selected.getStatus() != ItemStatus.APPROVED){
            showWarning("Chỉ mở phiên đấu giá với các item dang APPROVED");
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
            auctionService.createSession(selected.getId(), endAt);
            showInfo("Đã mở phiên đấu giá cho sản phẩm ID " + selected.getId() + ".");
            loadData();
        } catch (Exception e) {
            showWarning("Mở phiên thất bại: " + e.getMessage());
        }
    }

    // Trở về dashboard phù hợp với role hiện tại.
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
